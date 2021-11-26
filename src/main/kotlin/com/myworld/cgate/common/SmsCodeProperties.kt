package com.myworld.cgate.common

/**
 * 短信验证码
 */
open class SmsCodeProperties {

    open var length = 6
    var expireIn = 60
    /**
     * 要拦截的url，多个url用逗号隔开，ant pattern
     */
    var url: String? = null

}
