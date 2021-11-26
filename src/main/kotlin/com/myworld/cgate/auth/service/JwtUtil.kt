package com.myworld.cgate.auth.service


import com.alibaba.fastjson.JSONObject
import com.myworld.cgate.auth.authenticate.config.UserContextHolder
import com.myworld.cgate.auth.data.repository.UserInfoRepository
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.CurrentUser
import com.myworld.cgate.common.ResultUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.crypto.MacProvider
import org.apache.logging.log4j.LogManager
import org.springframework.data.redis.core.BoundHashOperations
import org.springframework.data.redis.core.RedisTemplate
import java.io.Serializable
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


object JwtUtil : Serializable {
    private val log = LogManager.getRootLogger()

    const val HEADER_AUTH = "Authorization"
    const val TOKEN_PREFIX = "Bearer"
    const val TOKEN_TYPE = "tokenType"
    const val HEADER_SERVICE = "serviceName"
    const val HEADER_ROLE = "role"
    const val HEADER_WID = "wid"
    const val TIME_EXPIRE = "expireTime"

    const val GROUP = "group"
    const val GROUP_NAME = "xinheJingrong"

    const val TOKEN_ACCESS = "access-token"
    const val TOKEN_REFRESH = "refresh-token"
    const val TOKEN_REFRESH_SAVE = "TOKENREFRESH:"

    const val timeValidAccessToken = 1 * 60 * 60 * 1000L         // 1个小时，单位毫秒
    const val timeValidRefreshToken = 24 * 60 * 60 * 1000L       // 24个小时，单位毫秒
    const val timeGraceRefreshToken = 20 * 60 * 1000L            // 续签token宽限时间，为到期前20分钟

    const val BLACKLIST_USERID = "MYBLACKLIST:USERID"
    const val BLACKLIST_IPADDR = "MYBLACKLIST:IPADDR"
    const val BLACKLIST_TOKENS = "MYBLACKLIST:TOKENS"
    const val blacklistExpireTime = 7 * 24 * 60 * 60 * 1000L

    // 可以使用MacProvider.generateKey()，但生成的key是基于本机Mac地址的，其他机器将无法解析
    private val secret = MacProvider.generateKey()
    private val privateKey: PrivateKey = TokenKeyGenerator.privateKey
    private val publicKey: PublicKey = TokenKeyGenerator.publicKey

    @JvmStatic
    fun generateToken(wid: String, roles: Set<*>, serviceNames: Set<*>): MutableMap<String, Any> {
        val expireTime = Date().time + timeValidAccessToken
        val map = HashMap<String, Any>()
        map[GROUP] = GROUP_NAME
        map[TOKEN_TYPE] = TOKEN_ACCESS
        map[HEADER_WID] = wid
        map[HEADER_ROLE] = roles
        map[HEADER_SERVICE] = serviceNames
        map[TIME_EXPIRE] = expireTime
        val jwt = Jwts.builder().setClaims(map).signWith(privateKey, SignatureAlgorithm.RS256).compact()
        val jwtFull = "$TOKEN_PREFIX $jwt"
        val mapBack: MutableMap<String, Any> = HashMap()
        mapBack["token"] = jwtFull
        mapBack["expireTime"] = expireTime
        return mapBack
    }

    @JvmStatic
    fun refreshToken(token: String, template: RedisTemplate<String, Any>, userInfoRepository: UserInfoRepository): String {
        val tokenMap: MutableMap<String, Any> = parseToken(token)
        val wid = tokenMap[HEADER_WID] as String
        val expireTimeOld = tokenMap[TIME_EXPIRE] as Long
        val currentTime = Date().time
        val expireTimeNew = currentTime + timeValidRefreshToken

        // 1、续签一个新的token, tokeType为refresh-token，其他不变
        tokenMap[TOKEN_TYPE] = TOKEN_REFRESH
        tokenMap[TIME_EXPIRE] = expireTimeNew
        val jwt = Jwts.builder().setClaims(tokenMap).signWith(privateKey, SignatureAlgorithm.RS256).compact()
        val refreshToken = "$TOKEN_PREFIX $jwt"
        // 2、把旧的token存入redis, key为旧token，value为refreshToken，时间为到期时间和当前时间的差额
        val redisKey = TOKEN_REFRESH_SAVE + token
        val diffTime = expireTimeOld - currentTime
        if (diffTime > 0) {
            template.opsForValue()[redisKey, refreshToken, diffTime / 1000] = TimeUnit.SECONDS
        }
        // 3、更新数据库UserInfo表中的token和expireTime
        val optional = userInfoRepository.findById(wid)
        if (optional.isPresent) {
            val userInfo = optional.get()
            userInfo.token = refreshToken
            userInfo.expireTime = expireTimeNew
            userInfoRepository.save(userInfo)
        }

        return refreshToken
    }

    @JvmStatic
    fun parseToken(token: String): MutableMap<String, Any> {
        val map: MutableMap<String, Any> = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token.replace(TOKEN_PREFIX, "")).body
        log.info("【JwtUtil解析token body是{}】", map.toString())
        return map
    }

    @JvmStatic
    fun checkToken(token: String, serviceNameTarget: String, template: RedisTemplate<String, Any>, userInfoRepository: UserInfoRepository): ApiResult<Any?> {
        log.info("【JwtUtil的checkToken方法中serviceNameTarget是：{}】", serviceNameTarget)
        val currentTime = Date().time

        // 1、如果redis黑名单tokens中存在该token, 并且token的到期时间大于当前时间的，则返回failure
        val boundHashOperationTokens: BoundHashOperations<String, String, Long> = template.boundHashOps(BLACKLIST_TOKENS)
        if (boundHashOperationTokens.hasKey(token) == true) {
            val widBlackExpireTimeToken = boundHashOperationTokens[token]
            if (widBlackExpireTimeToken != null && currentTime < widBlackExpireTimeToken) {
                return ResultUtil.failure(msg = "黑名单TOKEN用户")
            }
        }

        // 2、解析token
        var tokenReturn = token
        val tokenMap = parseToken(tokenReturn)
        val wid = tokenMap[HEADER_WID] as String
        val roles = tokenMap[HEADER_ROLE] as ArrayList<*>
        val serviceNames = tokenMap[HEADER_SERVICE] as ArrayList<*>
        val expireTime = tokenMap[TIME_EXPIRE] as Long
        val groupName = tokenMap[GROUP] as String
        // 以下4个条件检查，返回failure
        if (groupName != GROUP_NAME) {
            return ResultUtil.failure(msg = "非法权限")
        }
        if (serviceNameTarget !in serviceNames) {
            return ResultUtil.failure(msg = "权限不匹配")
        }
        val diffTime = expireTime - currentTime
        if (diffTime <= 0) {
            return ResultUtil.failure(msg = "令牌已过期")
        }

        // 3、如果redis黑名单userId中存在该wid, 并且设置的解封日期大于当前时间的，则返回failure
        val boundHashOperations: BoundHashOperations<String, String, Long> = template.boundHashOps(BLACKLIST_USERID)
        if (boundHashOperations.hasKey(wid) == true) {
            val widBlackExpireTime = boundHashOperations[wid]
            if (widBlackExpireTime != null && currentTime < widBlackExpireTime) {
                return ResultUtil.failure(msg = "黑名单ID用户")
            }
        }

        // 4、通过以上3道检查后，实现一个currentUser实例，放入上下文中
        val currentUser = CurrentUser(wid = wid, roles = setOf(roles), token = tokenReturn, expireTime = expireTime)
        // 如果到期时间处于token宽限期内，则更新为refreshToken后返回
        if (diffTime < timeGraceRefreshToken) {
            // 从redis中查找，是否有需要用旧token换取refreshToken的记录，如有则直接取出refreshToken，如没有则制作一个新的refreshToken
            val redisKey = TOKEN_REFRESH_SAVE + token
            val refreshTokenSave = template.opsForValue()[redisKey]
            tokenReturn = if (refreshTokenSave != null) {
                refreshTokenSave as String
            } else {
                refreshToken(tokenReturn, template, userInfoRepository)
            }
            currentUser.token = tokenReturn
            val currentUserStr = JSONObject.toJSONString(currentUser)
            val currentUserStrEncode = MyUserKeyService.aesEncrypt(currentUserStr)
            return ResultUtil.update(data = currentUserStrEncode)
        }
        // 设置线程中的用户上下文, 并返回wid
        UserContextHolder.setUserContext(currentUser)
        return ResultUtil.success(data = wid)
    }

    @JvmStatic
    fun addToBlacklist(mainKey: String, subKey: String, expireTime: Long? = null, template: RedisTemplate<String, Any>) {
        val expireTimeNeed = expireTime ?: Date().time + blacklistExpireTime
        val boundHashOperations: BoundHashOperations<String, String, Long> = template.boundHashOps(mainKey)
        boundHashOperations.put(subKey, expireTimeNeed)
    }


    @JvmStatic
    fun tokenToCurrentUser(token: String): CurrentUser {
        val tokenMap = parseToken(token)
        val wid = tokenMap[HEADER_WID] as String
        val roles = tokenMap[HEADER_ROLE] as ArrayList<*>
        val expireTime = tokenMap[TIME_EXPIRE] as Long
        return CurrentUser(wid = wid, roles = roles.toHashSet(), token = token, expireTime = expireTime)
    }
}
