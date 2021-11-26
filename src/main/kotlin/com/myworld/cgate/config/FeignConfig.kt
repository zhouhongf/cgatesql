package com.myworld.cgate.config

import feign.Client
import feign.Feign
import feign.Logger
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.commons.httpclient.OkHttpClientConnectionPoolFactory
import org.springframework.cloud.commons.httpclient.OkHttpClientFactory
import org.springframework.cloud.openfeign.FeignAutoConfiguration
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

@Configuration
@ConditionalOnClass(Feign::class)
@AutoConfigureBefore(FeignAutoConfiguration::class)
open class FeignConfig {

    lateinit var okHttpClient: OkHttpClient

    @Bean
    open fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.FULL
    }

    @Bean
    @ConditionalOnMissingBean(ConnectionPool::class)
    open fun httpClientConnectionPool(
        httpClientProperties: FeignHttpClientProperties,
        connectionPoolFactory: OkHttpClientConnectionPoolFactory): ConnectionPool {
        val maxTotalConnections = httpClientProperties.maxConnections
        val timeToLive = httpClientProperties.timeToLive
        val ttlUnit = httpClientProperties.timeToLiveUnit
        return connectionPoolFactory.create(maxTotalConnections, timeToLive, ttlUnit)
    }

    @Bean
    open fun client(httpClientFactory: OkHttpClientFactory,
                    connectionPool: ConnectionPool,
                    httpClientProperties: FeignHttpClientProperties): OkHttpClient? {
        val followRedirects = httpClientProperties.isFollowRedirects
        val connectTimeout = httpClientProperties.connectionTimeout
        val disableSslValidation = httpClientProperties.isDisableSslValidation
        okHttpClient = httpClientFactory.createBuilder(disableSslValidation)
            .connectTimeout(connectTimeout.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(followRedirects).connectionPool(connectionPool)
            .addInterceptor(feignOkHttpInterceptor())
            .build()
        return okHttpClient
    }

    @PreDestroy
    fun destroy() {
        okHttpClient.dispatcher().executorService().shutdown()
        okHttpClient.connectionPool().evictAll()
    }

    @Bean
    @ConditionalOnMissingBean(Client::class)
    open fun feignClient(client: OkHttpClient): Client {
        return feign.okhttp.OkHttpClient(client)
    }

    @Bean
    open fun feignOkHttpInterceptor(): Interceptor {
        return FeignOkHttpInterceptor()
    }
}
