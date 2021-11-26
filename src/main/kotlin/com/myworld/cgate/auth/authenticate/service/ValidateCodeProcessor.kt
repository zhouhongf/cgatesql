package com.myworld.cgate.auth.authenticate.service

import com.myworld.cgate.common.ApiResult
import org.springframework.web.server.ServerWebExchange

/**
 * 验证码处理器，封装不同的验证码处理逻辑
 */
interface ValidateCodeProcessor {
    /**
     * 创建校验码
     */
    @Throws(Exception::class)
    fun create(ctx: ServerWebExchange): ApiResult<*>?

    /**
     * 校验验证码
     */
    fun validate(ctx: ServerWebExchange): ApiResult<*>?
}
