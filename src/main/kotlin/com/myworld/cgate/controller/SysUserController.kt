package com.myworld.cgate.controller

import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.service.SysUserService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/cauth")
class SysUserController {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var sysUserService: SysUserService

    @GetMapping("/checkLogin")
    fun checkLogin(ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.checkLogin(ctx)
    }

    @GetMapping("/exists")
    fun doesUserExists(@RequestParam username: String): ApiResult<*> {
        return sysUserService.doesUserExists(username)
    }

    @GetMapping("/getUsername")
    fun getUsername(wid: String?): String? {
        return sysUserService.getUsername(wid)
    }

    @PostMapping("/doRegister")
    fun register(@RequestParam("mobile") mobile: String, @RequestParam("smsCode") smsCode: String, @RequestBody password: String, ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.register(mobile, smsCode, password, ctx)
    }

    @PostMapping("/doLogin")
    fun login(@RequestParam username: String, @RequestParam password: String, @RequestParam imageCode: String, ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.login(username, password, imageCode, ctx)
    }

    @PostMapping("/doSmsLogin")
    fun smsLogin(@RequestParam mobile: String, @RequestParam smsCode: String, ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.smsLogin(mobile, smsCode, ctx)
    }

    @PostMapping("/doChangePassword")
    fun changePassword(@RequestParam passwordold: String, @RequestBody password: String): ApiResult<*> {
        return sysUserService.changePassword(passwordold, password)
    }

    @PostMapping("/doResetPassword")
    fun resetPassword(@RequestParam mobile: String, @RequestParam smsCode: String, @RequestBody password: String, ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.resetPassword(mobile, smsCode, password, ctx)
    }

    @PostMapping("/uploadUserAvatarBase64")
    fun uploadUserAvatarBase64(@RequestBody base64: String): ApiResult<*> {
        return sysUserService.uploadUserAvatarBase64(base64)
    }

    @GetMapping("/getAvatar")
    fun getAvatar(): ApiResult<*> {
        return sysUserService.getAvatar()
    }

    @GetMapping("/updateVisitor")
    fun updateVisitor(ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.updateVisitor(ctx)
    }


    @GetMapping("/adminGetUsers")
    fun adminGetUsers(@RequestParam playerType: String, @RequestParam pageSize: Int, @RequestParam pageIndex: Int): ApiResult<*> {
        return sysUserService.adminGetUsers(playerType, pageSize, pageIndex)
    }

    @PostMapping("/adminSetUser")
    fun adminSetUser(@RequestParam username: String, @RequestParam password: String, @RequestParam wid: String?, @RequestBody sysroles: MutableList<String>): ApiResult<*> {
        return sysUserService.adminSetUser(username, password, wid, sysroles)
    }

    @PostMapping("/adminDelUser")
    fun adminDelUser(@RequestBody wid: String): ApiResult<*> {
        return sysUserService.adminDelUser(wid)
    }


}
