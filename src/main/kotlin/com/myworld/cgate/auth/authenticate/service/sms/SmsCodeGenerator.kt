package com.myworld.cgate.auth.authenticate.service.sms


import com.myworld.cgate.auth.authenticate.service.ValidateCodeGenerator
import com.myworld.cgate.auth.authenticate.vo.SmsCode
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.SecurityConstants
import com.myworld.cgate.common.SecurityProperties
import org.apache.commons.lang.RandomStringUtils
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * SMS验证码生成器
 */
@Component("smsValidateCodeGenerator")
class SmsCodeGenerator : ValidateCodeGenerator {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var securityProperties: SecurityProperties
    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var template: RedisTemplate<String, Any>

    override fun generate(ctx: ServerWebExchange): SmsCode? {
        // 从ServletWebRequest中获得"mobile"手机号码
        var mobile = ctx.request.queryParams.getFirst(SecurityConstants.DEFAULT_PARAMETER_NAME_MOBILE)
        mobile = MyUserKeyService.getRealUsername(mobile!!)
        val validateCodeKey = SecurityConstants.VALIDATE_CODE
        val theKeyCheck = "SMS$validateCodeKey$mobile"
        if (template.hasKey(theKeyCheck)) {
            val oldSmsCode = template.opsForValue()[theKeyCheck] as SmsCode
            val oldExpireTime = oldSmsCode.expireTime
            val zoneId = ZoneId.systemDefault()
            val instant = oldExpireTime!!.atZone(zoneId).toInstant()
            val expireTime = instant.toEpochMilli()
            val currentTime = System.currentTimeMillis()
            val diffTime = expireTime - currentTime
            val expireIn = securityProperties.code.sms.expireIn * 1000
            if (diffTime in 1 until expireIn) {
                return null
            }
        }
        // 根据 securityProperties及其子类，获得需要生成的验证码的长度，生成一串随机数字。
        val code = RandomStringUtils.randomNumeric(securityProperties.code.sms.length)
        val theSessionId = ctx.session.block()!!.id
        val expireIn = securityProperties.code.image.expireIn
        val expireTime = LocalDateTime.now().plusSeconds(expireIn.toLong())
        return SmsCode(mobile, code, expireTime, theSessionId)
    }
}
