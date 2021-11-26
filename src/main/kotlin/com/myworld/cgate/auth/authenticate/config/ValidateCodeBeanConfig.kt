package com.myworld.cgate.auth.authenticate.config

import com.myworld.cgate.auth.authenticate.service.ValidateCodeGenerator
import com.myworld.cgate.auth.authenticate.service.image.ImageCodeGenerator
import com.myworld.cgate.auth.authenticate.service.sms.DefaultSmsCodeSender
import com.myworld.cgate.auth.authenticate.service.sms.SmsCodeSender
import com.myworld.cgate.common.SecurityProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 验证码相关的扩展点配置。配置在这里的bean，业务系统都可以通过声明同类型或同名的bean来覆盖安全
 */
@Configuration
open class ValidateCodeBeanConfig {
    @Autowired
    private lateinit var securityProperties: SecurityProperties

    /**
     * 图片验证码图片生成器
     */
    @Bean
    @ConditionalOnMissingBean(name = ["imageValidateCodeGenerator"])
    open fun imageValidateCodeGenerator(): ValidateCodeGenerator {
        val codeGenerator = ImageCodeGenerator()
        codeGenerator.securityProperties = securityProperties
        return codeGenerator
    }

    /**
     * 短信验证码发送器
     */
    @Bean
    @ConditionalOnMissingBean(SmsCodeSender::class)
    open fun smsCodeSender(): SmsCodeSender {
        return DefaultSmsCodeSender()
    }
}
