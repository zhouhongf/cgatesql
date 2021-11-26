package com.myworld.cgate.filter

import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import com.myworld.cgate.auth.data.entity.VisitWatch
import com.myworld.cgate.auth.data.repository.UserInfoRepository
import com.myworld.cgate.auth.data.repository.VisitWatchRepository
import com.myworld.cgate.auth.service.JwtUtil
import com.myworld.cgate.auth.service.MyUserKeyService
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.CurrentUser
import com.myworld.cgate.common.ResultUtil

import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.redis.core.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 全局过滤器，后于RequestLimitGlobalFilter执行
 */
@Component
class JwtGlobalFilter : GlobalFilter, Ordered {
    private val log = LogManager.getRootLogger()
    private val BLACKLIST_IPADDR = JwtUtil.BLACKLIST_IPADDR
    private val BLACKLIST_USERID = JwtUtil.BLACKLIST_USERID
    private val BLACKLIST_TOKENS = JwtUtil.BLACKLIST_TOKENS

    @Value("\${my.security.maxVisitNumber}")
    private val maxVisitNumber = 1000
    @Value("\${my.security.blacklistExpireTime}")
    private val blacklistExpireTime = 7 * 24 * 60 * 60 * 1000    // 冻结时间为7天
    @Value("\${my.security.defaultLoginFullPage}")
    private var defaultLoginFullPage: String = "http://localhost:9005/#/auth/login"

    @Autowired
    private lateinit var userInfoRepository: UserInfoRepository
    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var template: RedisTemplate<String, Any>
    @Autowired
    private lateinit var visitWatchRepository: VisitWatchRepository

    override fun getOrder(): Int {
        return -2147483600
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val currentTime = Date().time
        val request = exchange.request
        val response = exchange.response

        val ip = request.remoteAddress!!.address.hostAddress
        val uri = request.path.value()
        val token = request.headers.getFirst(JwtUtil.HEADER_AUTH)
        val userAgent = request.headers.getFirst("User-Agent")
        val referer = request.headers.getFirst("Referer")

        // 如果redis黑名单ipAddress中存在该IP, 并且设置的解封日期大于当前时间的，则返回failure
        val boundHashOperations: BoundHashOperations<String, String, Long> = template.boundHashOps(BLACKLIST_IPADDR)
        if (boundHashOperations.hasKey(ip) == true) {
            val widBlackExpireTime = boundHashOperations[ip]
            if (widBlackExpireTime != null && currentTime < widBlackExpireTime) {
                makeResponseFail(response)
            }
        }
        // 保存本次访问至数据库中
        val visitWatch = VisitWatch(ipAddress = ip, visitUserAgent = userAgent, visitReferer = referer, visitPath = uri)
        // 如果没有token，则放行，保存不含wid的visitWatch
        if (token.isNullOrEmpty()) {
            // 如果没有token, 则以IP地址来计算，在visitWatch表中的，计算该IP的累计访问量，如果累计访问量超过1000，则将该IP放入黑名单，并从表中，移除该IP的所有记录
            checkVisitWatchIPLimit(visitWatch)
            return chain.filter(exchange)
        }

        // 验证 将要访问的路由名称 是否与授权时TOKEN中的路由名称 相同
        val route = exchange.getRequiredAttribute<Route>(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)
        val serviceNameTarget = route.id.substring(6).toUpperCase()      //去掉routeId的“serve_”前缀
        val apiResult: ApiResult<Any?> = JwtUtil.checkToken(token, serviceNameTarget, template, userInfoRepository)
        log.info("================= 返回的检查结果是：{}", apiResult)
        // 验证失败，则返回
        if (apiResult.code < 0) {
            return makeResponseFail(response)
        }
        // 需要更新token, 则返回
        if (apiResult.code == 9) {
            val bodyDataBuffer = makeDataBuffer(apiResult, response)
            return response.writeWith(Mono.just(bodyDataBuffer))
        }
        // 通过以上筛查后，JwtUtil检查返回wid过来，为visitWid添加wid后，再检查wid的累计访问量
        val wid = apiResult.data as String
        visitWatch.wid = wid
        checkVisitWatchLimit(visitWatch)

        return chain.filter(exchange)
    }

    // 用于返回具体的response数据
    fun makeDataBuffer(result: ApiResult<Any?>, response: ServerHttpResponse): DataBuffer {
        val httpHeaders = response.headers
        httpHeaders.add("Content-Type", "application/json; charset=UTF-8")
        httpHeaders.add("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
        // 设置body
        val warningStr = Gson().toJson(result)
        return response.bufferFactory().wrap(warningStr.toByteArray())
    }

    fun makeResponseFail(response: ServerHttpResponse): Mono<Void> {
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers[HttpHeaders.LOCATION] = defaultLoginFullPage
        return response.setComplete()
    }

    // 仅检查IP访问次数
    fun checkVisitWatchIPLimit(visitWatch: VisitWatch) {
        val ip = visitWatch.ipAddress!!
        val visitWatchCount = visitWatchRepository.countByIpAddress(ip)
        if (visitWatchCount > maxVisitNumber) {
            JwtUtil.addToBlacklist(mainKey = BLACKLIST_IPADDR, subKey = ip, template = template)
            visitWatchRepository.deleteAllByIpAddress(ip)
        } else {
            visitWatchRepository.save(visitWatch)
        }
    }

    // 先检查IP访问次数，再检查Wid访问次数
    fun checkVisitWatchLimit(visitWatch: VisitWatch) {
        val ip = visitWatch.ipAddress!!
        val wid = visitWatch.wid!!
        val visitWatchCountIP = visitWatchRepository.countByIpAddress(ip)
        if (visitWatchCountIP > maxVisitNumber) {
            JwtUtil.addToBlacklist(mainKey = BLACKLIST_IPADDR, subKey = ip, template = template)
            visitWatchRepository.deleteAllByIpAddress(ip)
        } else {
            val visitWatchCountWid = visitWatchRepository.countByWid(wid)
            if (visitWatchCountWid > maxVisitNumber) {
                JwtUtil.addToBlacklist(mainKey = BLACKLIST_USERID, subKey = wid, template = template)
                visitWatchRepository.deleteAllByWid(wid)
            } else {
                visitWatchRepository.save(visitWatch)
            }
        }
    }
}
