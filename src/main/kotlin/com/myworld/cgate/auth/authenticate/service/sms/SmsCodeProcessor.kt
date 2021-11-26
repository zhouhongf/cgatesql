package com.myworld.cgate.auth.authenticate.service.sms


import com.myworld.cgate.auth.authenticate.service.AbstractValidateCodeProcessor
import com.myworld.cgate.auth.authenticate.vo.ValidateCode
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.SecurityConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component("smsValidateCodeProcessor")
class SmsCodeProcessor : AbstractValidateCodeProcessor<ValidateCode>() {
    /**
     * 短信验证码发送器
     */
    @Autowired
    private lateinit var smsCodeSender: SmsCodeSender

    @Throws(Exception::class)
    protected override fun send(ctx: ServerWebExchange, validateCode: ValidateCode) {
        //从ServletWebRequest中获得"mobile"手机号码
        var mobile = ctx.request.queryParams.getFirst(SecurityConstants.DEFAULT_PARAMETER_NAME_MOBILE)
        mobile = MyUserKeyService.getRealUsername(mobile!!)
        return smsCodeSender.send(mobile, validateCode.code!!)
    }
}
