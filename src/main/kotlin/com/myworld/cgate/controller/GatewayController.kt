package com.myworld.cgate.controller

import com.myworld.cgate.service.ChatFeignService
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.ResultUtil
import com.myworld.cgate.service.SysUserService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class GatewayController(val chatFeignService: ChatFeignService, val sysUserService: SysUserService) {

    private val log : Logger = LogManager.getRootLogger()

    @GetMapping("/test")
    fun test(@RequestParam word: String): ApiResult<Any?> {
        val result = ResultUtil.success(msg = "准备返回内容", data = word)

        log.info("返回的内容是：{}", result)
        return result
    }

    @GetMapping("/avatar/{id}")
    fun avatar(@PathVariable id: String, ctx: ServerWebExchange): Mono<Void> {
        return sysUserService.avatar(id, ctx)
    }

    @GetMapping("/updateVisitor")
    fun updateVisitor(ctx: ServerWebExchange): ApiResult<*> {
        return sysUserService.updateVisitor(ctx)
    }


    @GetMapping(value = ["/getChatFileLocation/{idDetail}"])
    fun getChatFileLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray> {
        return chatFeignService.getChatFileLocation(idDetail)
    }

    @GetMapping(value = ["/avatarGroup/{id}"])
    fun getAvatarGroup(@PathVariable id: String): ResponseEntity<ByteArray> {
        return chatFeignService.getAvatarGroup(id)
    }

    @GetMapping(value = ["/getBlogPanelPhotoLocation/{idDetail}"])
    fun getBlogPanelPhotoLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray> {
        return chatFeignService.getBlogPanelPhotoLocation(idDetail)
    }

    @GetMapping(value = ["/getPhotoShowLocation/{idDetail}"])
    fun getPhotoShowLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray> {
        return chatFeignService.getPhotoShowLocation(idDetail)
    }
}
