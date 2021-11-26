package com.myworld.cgate.auth.authenticate.service.handler

import com.alibaba.fastjson.JSONObject
import com.myworld.cgate.auth.authenticate.config.UserContextHolder
import com.myworld.cgate.auth.data.entity.SysUser
import com.myworld.cgate.auth.data.entity.UserInfo
import com.myworld.cgate.auth.data.repository.UserInfoRepository
import com.myworld.cgate.auth.service.JwtUtil
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.*
import com.myworld.cgate.util.IPUtil
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.*
import kotlin.collections.HashSet


@Component
class MyLoginHandler {
    private val log = LogManager.getRootLogger()
    private val BLACKLIST_TOKENS = JwtUtil.BLACKLIST_TOKENS

    @Autowired
    private lateinit var userInfoRepository: UserInfoRepository
    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var template: RedisTemplate<String, Any>

    /**
     * 1、制作token
     * 2、制作UserContextHolder
     * 3、更新MongoDB和Redis中的token信息
     * 4、返回加密后的currentUser给前端
     */
    fun onAuthenticationSuccess(sysUser: SysUser, ctx: ServerWebExchange): ApiResult<*> {
        val serviceNames: MutableSet<String> = HashSet()
        serviceNames.add(ServiceName.DEFAULT.name)
        serviceNames.add(ServiceName.SCHAT.name)
        serviceNames.add(ServiceName.SWEALTH.name)
        serviceNames.add(ServiceName.SSTUDENT.name)
        val wid = sysUser.wid
        val roles = sysUser.sysroles!!.split(",").toSet()

        val tokenMap = JwtUtil.generateToken(wid = wid!!, roles = roles, serviceNames = serviceNames)
        val token = tokenMap["token"] as String
        val expireTime = tokenMap["expireTime"] as Long

        val currentUser = CurrentUser(wid = wid, roles = roles, token = token, expireTime = expireTime)
        UserContextHolder.setUserContext(currentUser)
        val cuserStr = JSONObject.toJSONString(currentUser)
        val cuserStrEncode = MyUserKeyService.aesEncrypt(cuserStr)

        // 更新userInfo, 更新token和expireTime, 以及最近一次登录的信息记录；将原token，如果还没有到期，则放入黑名单处理
        val userInfoOptional = userInfoRepository.findById(sysUser.wid!!)
        val userInfo: UserInfo
        if (userInfoOptional.isPresent) {
            userInfo = userInfoOptional.get()
            val tokenOld = userInfo.token!!
            if (userInfo.expireTime!! > Date().time) {
                JwtUtil.addToBlacklist(BLACKLIST_TOKENS, tokenOld, expireTime, template)
            }
        } else {
            userInfo = UserInfo(wid = wid, token = token, expireTime = expireTime)
        }
        val request = ctx.request
        val userAgent = request.headers.getFirst("User-Agent")
        val referer = request.headers.getFirst("Referer")
        val ip = request.remoteAddress!!.address.hostAddress
        val city = IPUtil.getCityInfo(ip)

        userInfo.lastLoginUserAgent = userAgent
        userInfo.lastLoginReferer = referer
        userInfo.lastLoginIp = ip
        userInfo.lastLoginCity = city
        userInfoRepository.save(userInfo)

        return ResultUtil.success(data = cuserStrEncode)
    }
}
