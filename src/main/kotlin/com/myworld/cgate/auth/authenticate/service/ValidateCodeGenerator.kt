package com.myworld.cgate.auth.authenticate.service


import com.myworld.cgate.auth.authenticate.vo.ValidateCode
import org.springframework.web.server.ServerWebExchange

/**
 * 验证码生成器
 */
interface ValidateCodeGenerator {
    /**
     * 生成验证码
     */
    fun generate(ctx: ServerWebExchange): ValidateCode?
}
