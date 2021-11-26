package com.myworld.cgate.auth.authenticate.service

import com.myworld.cgate.auth.authenticate.vo.SmsCode
import com.myworld.cgate.auth.authenticate.vo.ValidateCode
import com.myworld.cgate.auth.authenticate.vo.ValidateCodeType
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.SecurityConstants
import com.myworld.cgate.common.SecurityProperties
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.concurrent.TimeUnit

@Component
class ValidateCodeService {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var securityProperties: SecurityProperties
    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var template: RedisTemplate<String, Any>

    fun save(ctx: ServerWebExchange, code: ValidateCode, validateCodeType: ValidateCodeType) {
        //使用手机号作为key的一部分，将validateCode存入redis中，到时取得时候，也得凭借request请求中的手机号组装成key，来redis中取vaidateCode, 如果手机号不正确，那么key肯定不正确，那么就取不到validateCode
        val theCodeType = validateCodeType.toString()

        val theKey = if (theCodeType == "SMS") {
            theCodeType + SecurityConstants.VALIDATE_CODE + (code as SmsCode).mobile
        } else {
            // val sessionId = ctx.session.block()?.id
            // theCodeType + SecurityConstants.VALIDATE_CODE + sessionId;
            // 如果因为跨域导致的sessionId不一致，无法查询到redis中的验证码，则可使用前端传进来的requestParam参数中的random的值来设置
            val imageCodeParam = ctx.request.queryParams.getFirst("random")
            theCodeType + SecurityConstants.VALIDATE_CODE + imageCodeParam
        }
        log.info("【ValidateCodeService的save方法制作完成{}验证码的theKey为：{}】", theCodeType, theKey)

        // 因为imageCodeProperties类继承于smsCodeProperties类，其expireIn属性是相同的，所以这里直接取smsCode的expireIn
        val expireIn = securityProperties.code.sms.expireIn
        template.opsForValue()[theKey, code, expireIn.toLong()] = TimeUnit.SECONDS
    }

    fun get(ctx: ServerWebExchange, validateCodeType: ValidateCodeType): ValidateCode {
        val theKey = getRedisKey(ctx, validateCodeType)
        return template.opsForValue()[theKey] as ValidateCode
    }

    fun remove(ctx: ServerWebExchange, validateCodeType: ValidateCodeType) {
        val theKey = getRedisKey(ctx, validateCodeType)
        template.delete(theKey)
    }

    fun getRedisKey(ctx: ServerWebExchange, validateCodeType: ValidateCodeType): String {
        val request = ctx.request
        log.info("【ValidateCodeService的getRedisKey方法headers中有：{}】", request.headers)
        val theCodeType = validateCodeType.toString()
        return if (theCodeType == "SMS") {
            val usernameInRequest = request.queryParams.getFirst(SecurityConstants.DEFAULT_PARAMETER_NAME_MOBILE)
            val usernameReal = MyUserKeyService.getRealUsername(usernameInRequest!!)
            theCodeType + SecurityConstants.VALIDATE_CODE + usernameReal
        } else {
            val imageCodeParam = request.headers.getFirst(SecurityConstants.HEADER_IMAGE_CODE)
            theCodeType + SecurityConstants.VALIDATE_CODE + imageCodeParam
        }
    }

}
