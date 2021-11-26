package com.myworld.cgate.auth.authenticate.vo

import java.time.LocalDateTime

/**
 * 因父类ValidateCode已添加了serializable，所以子类就不用再添加了
 * 但是序列化时，子类必须要有一个自己的空的构造函数
 */
class SmsCode : ValidateCode {
    var mobile: String? = null

    constructor() {}
    constructor(mobile: String?, code: String?, expireTime: LocalDateTime?, sessionId: String?) : super(code, expireTime, sessionId) {
        this.mobile = mobile
    }

}
