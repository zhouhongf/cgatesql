package com.myworld.cgate.auth.authenticate.service.sms


/**
 * 短信发送接口
 */
interface SmsCodeSender {
    /**
     * 发送短信验证码
     */
    fun send(mobile: String, code: String)
}
