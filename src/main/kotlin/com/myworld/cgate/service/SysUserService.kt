package com.myworld.cgate.service

import com.myworld.cgate.common.ApiResult
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

interface SysUserService {
    fun checkLogin(ctx: ServerWebExchange): ApiResult<*>

    fun doesUserExists(username: String): ApiResult<*>
    fun getUsername(wid: String?): String?

    fun register(username: String, smsCode: String, password: String, ctx: ServerWebExchange): ApiResult<*>
    fun createSysUser(realUsername: String, realPassword: String, creator: String): ApiResult<*>
    fun setSysUser(username: String, passwordEncode: String, wid: String, sysroles: MutableList<String>, updater: String): ApiResult<*>

    fun login(username: String, password: String, imageCode: String, ctx: ServerWebExchange): ApiResult<*>
    fun smsLogin(mobile: String, smsCode: String, ctx: ServerWebExchange): ApiResult<*>

    fun changePassword(passwordold: String, password: String): ApiResult<*>
    fun resetPassword(mobile: String, smsCode: String, password: String, ctx: ServerWebExchange): ApiResult<*>

    fun uploadUserAvatarBase64(base64: String): ApiResult<*>
    fun avatar(id: String,ctx: ServerWebExchange): Mono<Void>
    fun getAvatar(): ApiResult<*>

    fun updateVisitor(ctx: ServerWebExchange): ApiResult<*>
    fun getCityLngLatByProvinceAndCity(provinceName: String?, cityName: String?): String?

    fun adminGetUsers(playerType: String, pageSize: Int, pageIndex: Int): ApiResult<*>
    fun adminSetUser(username: String, password: String, wid: String?, sysroles: MutableList<String>): ApiResult<*>
    fun adminDelUser(wid: String): ApiResult<*>
}
