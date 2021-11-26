package com.myworld.cgate.service

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@FeignClient(name = "schat-server", url = "\${feign.schat.url}")
interface ChatFeignService {

    @RequestMapping(value = ["/avatarGroup/{id}"], method = [RequestMethod.GET])
    fun getAvatarGroup(@PathVariable id: String): ResponseEntity<ByteArray>

    @RequestMapping(value = ["/getChatFileLocation/{idDetail}"], method = [RequestMethod.GET])
    fun getChatFileLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray>

    @RequestMapping(value = ["/getBlogPanelPhotoLocation/{idDetail}"], method = [RequestMethod.GET])
    fun getBlogPanelPhotoLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray>

    @RequestMapping(value = ["/getPhotoShowLocation/{idDetail}"], method = [RequestMethod.GET])
    fun getPhotoShowLocation(@PathVariable idDetail: String): ResponseEntity<ByteArray>


}
