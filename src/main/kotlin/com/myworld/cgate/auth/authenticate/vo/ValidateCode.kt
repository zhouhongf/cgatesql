package com.myworld.cgate.auth.authenticate.vo

import java.io.Serializable
import java.time.LocalDateTime

/**
 * 验证码信息封装类
 */
open class ValidateCode : Serializable {
    var code: String? = null
    var expireTime: LocalDateTime? = null
    var sessionId: String? = null           // 用于校验是否为同一个客户端使用

    constructor() {}
    constructor(code: String?, expireTime: LocalDateTime?, sessionId: String?) {
        this.code = code
        this.expireTime = expireTime
        this.sessionId = sessionId
    }

}
