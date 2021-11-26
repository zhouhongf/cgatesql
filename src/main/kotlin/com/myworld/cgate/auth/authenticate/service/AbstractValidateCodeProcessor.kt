package com.myworld.cgate.auth.authenticate.service


import com.myworld.cgate.auth.authenticate.vo.ImageCode
import com.myworld.cgate.auth.authenticate.vo.SmsCode
import com.myworld.cgate.auth.authenticate.vo.ValidateCode
import com.myworld.cgate.auth.authenticate.vo.ValidateCodeType
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.ApiResultEnum
import com.myworld.cgate.common.ResultUtil
import com.myworld.cgate.util.StringUtil
import org.apache.commons.lang.StringUtils
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.server.ServerWebExchange
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime
import javax.imageio.ImageIO


abstract class AbstractValidateCodeProcessor<C : ValidateCode> : ValidateCodeProcessor {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var validateCodeGenerators: Map<String, ValidateCodeGenerator>
    @Autowired
    private lateinit var validateCodeService: ValidateCodeService

    /**
     * 根据Holder返回的CodeProcessor类型来获取验证码的类型
     */
    private fun getValidateCodeType(): ValidateCodeType {
        // 从ImageCodeProcessor或者SmsCodeProcessor类名中，提取出Image或者Sms
        val type = StringUtils.substringBefore(javaClass.simpleName, "CodeProcessor")
        // 通过大写的验证码类型IMAGE或SMS，返回IMAGE或SMS
        return ValidateCodeType.valueOf(type.toUpperCase())
    }

    /**
     * 验证码关键步骤
     * 验证码产生、保存、发送
     */
    @Throws(Exception::class, IOException::class)
    override fun create(ctx: ServerWebExchange): ApiResult<*>? {
        // 第一步，生成验证码
        val validateCode = generate(ctx)
        // 第二步，保存验证码
        save(ctx, validateCode)
        // 第三步，发送验证码
        send(ctx, validateCode)
        if (validateCode.javaClass.simpleName == "ImageCode") {
            val imageCode = validateCode as ImageCode
            val os = ByteArrayOutputStream()
            ImageIO.write(imageCode.image, "png", os)
            val pngBase64 = StringUtil.bytesToBase64(os.toByteArray())
            return ResultUtil.success(data = pngBase64)
        }
        return ResultUtil.success()
    }

    /**
     * 抽象方法，发送校验码，由子类实现
     */
    @Throws(Exception::class)
    protected abstract fun send(ctx: ServerWebExchange, validateCode: C)


    /**
     * 生成验证码
     */
    private fun generate(ctx: ServerWebExchange): C {
        val type = getValidateCodeType().toString().toLowerCase()
        val generatorName = type + ValidateCodeGenerator::class.java.simpleName
        val validateCodeGenerator = validateCodeGenerators[generatorName]
        return validateCodeGenerator?.generate(ctx) as C
    }

    /**
     * 保存验证码
     * 保存在redis当中
     */
    private fun save(ctx: ServerWebExchange, validateCode: C) {
        val theCode = validateCode.code
        val theExpireTime = validateCode.expireTime
        val theSessionId = validateCode.sessionId
        val type = getValidateCodeType().toString().toLowerCase()
        if (type == "sms") {
            val smsCode = validateCode as SmsCode
            val code = SmsCode(smsCode.mobile, theCode, theExpireTime, theSessionId)
            log.info("AbstractValidateCodeProcessor的save()方法: 手机号" + smsCode.mobile + ", 验证码数字" + theCode + ", 到期时间" + theExpireTime)
            validateCodeService.save(ctx, code, getValidateCodeType())
        } else {
            val code = ValidateCode(theCode, theExpireTime, theSessionId)
            validateCodeService.save(ctx, code, getValidateCodeType())
        }
    }

    /**
     * 验证 验证码
     */
    override fun validate(ctx: ServerWebExchange): ApiResult<*> {
        //根据CodeProcessor类名的前缀，Image或Sms，来判别validateCode的Type
        val codeType = getValidateCodeType()
        //从redis中取出保存的validateCode
        val codeInSave = validateCodeService.get(ctx, codeType) as C
        return doValidate(ctx, codeType, codeInSave)
    }

    fun doValidate(ctx: ServerWebExchange, validateCodeType: ValidateCodeType, codeInSave: ValidateCode?): ApiResult<*> {
        //从request中取出String类型的验证码
        val request = ctx.request
        val codeType = validateCodeType.getParamNameOnValidate()
        val codeInRequest = request.queryParams.getFirst(codeType)
        if (codeInRequest.isNullOrEmpty()) {
            return ResultUtil.failure(msg = "验证码的值不能为空")
        }
        if (codeInSave == null) {
            return ResultUtil.failure(msg = "验证码不存在")
        }
        val theDataTime = codeInSave.expireTime
        if (LocalDateTime.now().isAfter(theDataTime)) {
            validateCodeService.remove(ctx, validateCodeType)
            return ResultUtil.failure(msg ="验证码已过期")
        }
        log.info("【AbstractValidateCodeProcessor的codeInRequest为：{}】", codeInRequest)
        log.info("【AbstractValidateCodeProcessor的codeInSave为：{}】", codeInSave.code)
        if (codeInSave.code != codeInRequest) {
            return ResultUtil.failure(msg ="验证码不匹配")
        }
        validateCodeService.remove(ctx, validateCodeType)
        return ResultUtil.success(msg = "验证码验证通过！")
    }

}
