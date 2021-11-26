package com.myworld.cgate.filter

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import org.apache.logging.log4j.LogManager
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RequestLimitGlobalFilter : GlobalFilter, Ordered {

    private val log = LogManager.getRootLogger()

    // HIGHEST_PRECEDENCE值是-2147483648，值越小，优先级越高
    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }

    /**
     * 单机网关限流用一个ConcurrentHashMap来存储 bucket，
     * 如果是分布式集群限流的话，可以采用 Redis等分布式解决方案
     */
    private val LOCAL_CACHE: MutableMap<String, Bucket> = ConcurrentHashMap()
    /**
     * 桶的最大容量，即能装载 Token 的最大数量
     */
    private val capacity = 30
    /**
     * 每次 Token 补充量
     */
    private val refillTokens = 1
    /**
     * 补充 Token 的时间间隔
     */
    private val refillDuration = Duration.ofSeconds(1)

    private fun createNewBucket(): Bucket {
        val refill = Refill.of(refillTokens.toLong(), refillDuration)
        val limit = Bandwidth.classic(capacity.toLong(), refill)
        return Bucket4j.builder().addLimit(limit).build()
    }

    /**
     * 根据IP地址进行限流
     */
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val ip = exchange.request.remoteAddress!!.address.hostAddress
        val bucket = LOCAL_CACHE.computeIfAbsent(ip) { k: String? -> createNewBucket() }
        log.info("【============================================== IP:{} ,令牌通可用数量:{} =======================================】", ip, bucket.availableTokens)
        return if (bucket.tryConsume(1)) {
            chain.filter(exchange)
        } else {
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS         //当可用的令牌书为0时，限流返回429状态码
            exchange.response.setComplete()
        }
    }
}
