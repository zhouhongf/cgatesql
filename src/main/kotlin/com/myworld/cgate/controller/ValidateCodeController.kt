package com.myworld.cgate.controller

import com.myworld.cgate.auth.authenticate.service.ValidateCodeProcessorHolder
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.SecurityConstants
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange


@RestController
class ValidateCodeController {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var validateCodeProcessorHolder: ValidateCodeProcessorHolder

    /**
     * 创建验证码，根据验证码类型不同，调用不同的 ValidateCodeProcessor接口实现
     * DEFAULT_VALIDATE_CODE_URL_PREFIX = "/validatecode"
     * type是： image 或者 sms
     */
    @GetMapping(SecurityConstants.DEFAULT_VALIDATE_CODE_URL_PREFIX + "/{type}")
    @Throws(Exception::class)
    fun createCode(ctx: ServerWebExchange, @PathVariable type: String): ApiResult<*>? {
        return validateCodeProcessorHolder.findValidateCodeProcessor(type)?.create(ctx)
    }

    /**
     * 内部Feign服务
     * 验证短信验证码
     * type="sms"
     * RequestParam不能少，因为有了其在方法括号中的注解，该请求路径才变得完整，完整的请求路径为：/checkValidateCode/{type}?mobile=xxx&smsCode=xxx
     * 验证短信验证码是根据请求路径来操作的，请求路径的参数必须为mobile和smsCode，并且请求方法必须为POST
     * RequestParam("mobile") String username, 其中RequestParam括号中引号中的内容mobile定义了请求的路径参数，
     * 当checkValidateCode()方法中的属性名称和请求路径的名称一致时，即RequestParam("mobile") String mobile，RequestParam括号中引号中的内容可省略，即RequestParam String mobile
     */
    @PostMapping("/checkValidateCode/{type}")
    fun checkSmsCode(ctx: ServerWebExchange, @PathVariable type: String, @RequestParam mobile: String, @RequestParam smsCode: String): ApiResult<*>? {
        return validateCodeProcessorHolder.findValidateCodeProcessor(type)?.validate(ctx)
    }

}
