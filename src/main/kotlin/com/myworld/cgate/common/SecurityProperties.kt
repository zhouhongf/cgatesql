package com.myworld.cgate.common

import org.springframework.stereotype.Component

@Component
class SecurityProperties {
    /**
     * 验证码配置
     */
    var code = ValidateCodeProperties()
    /**
     * 1个小时，单位毫秒
     */
    var accessTokenValidSeconds = 1 * 60 * 60 * 1000L
    /**
     * 24个小时，单位毫秒
     */
    var refreshTokenValidSeconds = 24 * 60 * 60 * 1000L
    /**
     * 续签token宽限时间，为到期前20分钟
     */
    var refreshTokenGraceSeconds = 20 * 60 * 1000L

    /**
     * 默认的redis缓存时间为30分钟
     */
    var defaultCacheMinute = 30L


}
