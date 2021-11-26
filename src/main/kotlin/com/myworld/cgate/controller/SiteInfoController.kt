package com.myworld.cgate.controller

import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.service.SiteInfoService
import com.myworld.cgate.siteinfo.entity.Writing
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@RestController
@RequestMapping("/shared")
class SiteInfoController {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var siteInfoService: SiteInfoService

    @GetMapping("/getWritingList")
    fun getWritingList(@RequestParam type: String): ApiResult<*> {
        return siteInfoService.getWritingList(type)
    }

    @PostMapping("/setWriting")
    fun setWriting(@RequestBody writingRaw: Writing): ApiResult<*> {
        return siteInfoService.setWriting(writingRaw)
    }

    @DeleteMapping("/delWriting")
    fun delWriting(@RequestParam id: String): ApiResult<*> {
        return siteInfoService.delWriting(id)
    }

    @GetMapping("/getWriting")
    fun getWriting(@RequestParam id: String): ApiResult<*> {
        return siteInfoService.getWriting(id)
    }

    @GetMapping("/getWritingByTitle")
    fun getWritingByTitle(@RequestParam title: String): ApiResult<*> {
        return siteInfoService.getWritingByTitle(title)
    }

    @GetMapping("/getWritingListByTypeAndAuthor")
    fun getWritingListByTypeAndAuthor(@RequestParam type: String, @RequestParam author: String, @RequestParam pageSize: Int, @RequestParam pageIndex: Int): ApiResult<*> {
        return siteInfoService.getWritingListByTypeAndAuthor(type, author, pageSize, pageIndex)
    }


    @GetMapping("/getFileList")
    fun getFileList(): ApiResult<*> {
        return siteInfoService.getFileList()
    }

    @PostMapping(value = ["/setFile"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun setFile(@RequestPart("officialName") officialName: String, @RequestPart("versionNumber") versionNumber: String, @RequestPart("file") filePart: FilePart): ApiResult<*> {
        return siteInfoService.setFile(officialName, versionNumber, filePart)
    }

    @DeleteMapping("/delFile/{id}")
    fun delFile(@PathVariable id: String): ApiResult<*> {
        return siteInfoService.delFile(id)
    }

    @GetMapping("/getFile/{officialName}")
    fun getFile(@PathVariable officialName: String, ctx: ServerWebExchange): Mono<Void> {
        return siteInfoService.getFile(officialName, ctx)
    }


    @GetMapping("/getSlideList")
    fun getSlideList(): ApiResult<*> {
        return siteInfoService.getSlideList()
    }

    @PostMapping(value = ["/setSlide"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun setSlide(@RequestParam id: String?, @RequestPart("title") title: String, @RequestPart("description") description: String, @RequestPart("link") link: String, @RequestPart("base64") base64: String): ApiResult<*> {
        return siteInfoService.setSlide(id, title, description, link, base64)
    }

    @DeleteMapping("/delSlide/{id}")
    fun delSlide(@PathVariable id: String): ApiResult<*> {
        return siteInfoService.delSlide(id)
    }

}
