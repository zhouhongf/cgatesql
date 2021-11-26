package com.myworld.cgate

import com.myworld.cgate.auth.authenticate.config.UserContextHolder
import com.myworld.cgate.auth.service.JwtUtil
import org.apache.logging.log4j.LogManager
import org.springframework.boot.SpringApplication
import org.springframework.cloud.client.SpringCloudApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.cors.reactive.CorsUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


@EnableScheduling
@EnableFeignClients
@SpringCloudApplication
open class CgateSqlApplication {
    private val log = LogManager.getRootLogger()

    @Bean
    open fun corsFilter(): WebFilter {
        return object : WebFilter {
            override fun filter(ctx: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
                log.info("==================== 执行corsFilter方法 ======================")
                val request = ctx.request;
                val token = request.headers.getFirst(JwtUtil.HEADER_AUTH)
                if (token != null) {
                    val currentUser = JwtUtil.tokenToCurrentUser(token)
                    UserContextHolder.setUserContext(currentUser)
                }

                if (CorsUtils.isCorsRequest(request)) {
                    val requestHeaders = request.headers;
                    val response = ctx.response;
                    val requestMethod = requestHeaders.accessControlRequestMethod;
                    val headers = response.headers;
                    headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, requestHeaders.origin);
                    headers.addAll(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders.accessControlRequestHeaders);

                    if (requestMethod != null) {
                        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethod.name);
                    }

                    headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                    headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
                    headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "18000L");  // 单位：秒


                    var allowHeaders = mutableListOf<String>();
                    if (request.method == HttpMethod.OPTIONS) {
                        allowHeaders.addAll(requestHeaders.get("Access-Control-Request-Headers")?.filter { it.isNotEmpty() } ?: listOf())
                    }

                    if (allowHeaders.any()) {
                        headers.set("Access-Control-Allow-Headers", allowHeaders.joinToString(","))
                    } else {
                        allowHeaders = requestHeaders.keys.toMutableList()
                        //https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Access-Control-Expose-Headers
                        val standardHeaders = arrayOf(
                            "expires",
                            "cache-control",
                            "content-language",
                            "content-type",
                            "last-modified",
                            "pragma",
                            "origin",
                            "accept",
                            "user-agent",
                            "connection",
                            "host",
                            "accept-language",
                            "accept-encoding"
                        )
                        //移除标准 header
                        allowHeaders.removeAll { standardHeaders.contains(it.toLowerCase()) }
                    }

                    if (request.method == HttpMethod.OPTIONS) {
                        response.statusCode = HttpStatus.OK;
                        return Mono.empty();
                    }
                }
                return chain.filter(ctx);
            }
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(CgateSqlApplication::class.java, *args)
}
