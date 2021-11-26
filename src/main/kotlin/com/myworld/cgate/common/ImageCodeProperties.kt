package com.myworld.cgate.common

/**
 * 图片验证码
 */
class ImageCodeProperties : SmsCodeProperties() {
    var width = 90
    var height = 25
    override var length = 6
}
