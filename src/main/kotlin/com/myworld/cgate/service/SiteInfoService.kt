package com.myworld.cgate.service

import com.myworld.cgate.auth.authenticate.config.UserContextHolder
import com.myworld.cgate.common.ApiResult
import com.myworld.cgate.common.PlayerType
import com.myworld.cgate.common.ResultUtil
import com.myworld.cgate.common.SecurityConstants
import com.myworld.cgate.siteinfo.entity.MyFile
import com.myworld.cgate.siteinfo.entity.MySlide
import com.myworld.cgate.siteinfo.entity.Writing
import com.myworld.cgate.siteinfo.repository.MyFileRepository
import com.myworld.cgate.siteinfo.repository.MySlideRepository
import com.myworld.cgate.siteinfo.repository.WritingRepository
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Service
class SiteInfoService {

    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var writingRepository: WritingRepository
    @Autowired
    private lateinit var myFileRepository: MyFileRepository
    @Autowired
    private lateinit var mySlideRepository: MySlideRepository


    fun getWritingList(type: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val writings = writingRepository.findByType(type) ?: return ResultUtil.failure()
        val writingList: MutableList<Any> = ArrayList()
        for (writing in writings) {
            val map: MutableMap<String, Any> = HashMap()
            map["id"] = writing.id!!
            map["title"] = writing.title!!
            map["author"] = writing.author!!
            map["updateTime"] = writing.updateTime
            map["canRelease"] = writing.canRelease
            writingList.add(map)
        }
        return ResultUtil.success(data = writingList)
    }

    fun setWriting(writingRaw: Writing): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户没有权限发布文章")
        }
        val idRaw = writingRaw.id!!

        val id = SecurityConstants.WRITING_PREFIX + Date().time
        val writing = if (idRaw.isEmpty()) {
            Writing(id = id, title = writingRaw.title, author = writingRaw.author, type = writingRaw.type)
        } else {
            val optional = writingRepository.findById(idRaw)
            if (optional.isPresent) {
                optional.get()
            } else {
                Writing(id = id, title = writingRaw.title, author = writingRaw.author, type = writingRaw.type)
            }
        }

        writing.type = writingRaw.type
        writing.author = writingRaw.author
        writing.title = writingRaw.title
        writing.content = writingRaw.content
        writing.canRelease = writingRaw.canRelease
        writing.updater = userDetail.wid
        writing.updateTime = Date().time
        writingRepository.save(writing)
        return ResultUtil.success()
    }

    fun delWriting(id: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户没有权限删除文章")
        }
        writingRepository.deleteById(id)
        return ResultUtil.success()
    }

    fun getWriting(id: String): ApiResult<*> {
        val optional = writingRepository.findById(id)
        return if (optional.isPresent) {
            ResultUtil.success(data = optional.get())
        } else {
            ResultUtil.failure()
        }
    }

    fun getWritingByTitle(title: String): ApiResult<*> {
        val writing = writingRepository.findByCanReleaseAndTitle(title = title) ?: ResultUtil.failure()
        return ResultUtil.success(data = writing)
    }

    fun getWritingListByTypeAndAuthor(type: String, author: String, pageSize: Int, pageIndex: Int): ApiResult<*> {
        val pageable: Pageable = PageRequest.of(pageIndex, pageSize, Sort.Direction.DESC, "createTime")
        val writingsPaged = writingRepository.findByCanReleaseAndTypeAndAuthor(type = type, author = author, pageable = pageable) ?: return ResultUtil.failure()
        val writingList: MutableList<Any> = ArrayList()
        for (writing in writingsPaged.content) {
            val map: MutableMap<String, Any> = HashMap()
            map["id"] = writing.id!!
            map["title"] = writing.title!!
            map["author"] = writing.author!!
            map["type"] = writing.type!!
            map["createTime"] = writing.createTime
            writingList.add(map)
        }
        return ResultUtil.success(num = writingsPaged.totalElements, data = writingList)
    }



    fun getFileList(): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val myfiles = myFileRepository.findAll(Sort.by(Sort.Direction.DESC, "updateTime"))
        return if (myfiles.isNullOrEmpty()) {
            ResultUtil.failure()
        } else {
            ResultUtil.success(data = myfiles)
        }
    }

    fun setFile(officialName: String, versionNumber: String, filePart: FilePart): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户没有权限发布文件")
        }

        // 设定了officialName为唯一值
        var myfile: MyFile? = myFileRepository.findByOfficialName(officialName)
        if (myfile == null) {
            val id = SecurityConstants.MYFILE_PREFIX + Date().time
            myfile = MyFile(id = id, officialName = officialName)
        }
        // 创建一个临时文件，从WebFlux的filePart中取出file
        val tempFile: Path = Files.createTempFile(SecurityConstants.MYFILE_PREFIX, filePart.filename())
        val file: File = tempFile.toFile()
        filePart.transferTo(file)

        myfile.fileByte = file.readBytes()
        myfile.extensionType = file.extension
        myfile.fileName = file.name
        myfile.size = file.length()
        myfile.versionNumber = versionNumber
        myfile.updateTime = Date().time
        myfile.updater = userDetail.wid
        myFileRepository.save(myfile)
        return ResultUtil.success()
    }

    fun delFile(id: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户没有权限删除文件")
        }
        myFileRepository.deleteById(id)
        return ResultUtil.success()
    }

    fun getFile(officialName: String, ctx: ServerWebExchange): Mono<Void> {
        val response = ctx.response
        val myfile = myFileRepository.findByOfficialName(officialName) ?: return response.setComplete()

        val fileBytes = myfile.fileByte
        if (fileBytes != null) {
            val bodyDataBuffer = response.bufferFactory().wrap(fileBytes)
            return response.writeWith(Mono.just(bodyDataBuffer))
        } else {
            response.statusCode = HttpStatus.NOT_FOUND
            return response.setComplete()
        }
    }


    fun setSlide(id: String?, title: String, description: String, link: String, base64: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户不是管理员")
        }
        if (id.isNullOrEmpty()){
            val slideNum = mySlideRepository.count()
            if (slideNum >= 5) {
                return ResultUtil.failure(msg = "Slide数量不能超过5张")
            }
            val idNew = SecurityConstants.MYSILDE_PREFIX + Date().time
            val mySlide = MySlide(id = idNew, title = title, description = description, image = base64, link = link, updater = userDetail.wid)
            mySlideRepository.save(mySlide)
            return ResultUtil.success()
        } else {
            val mySlideOption = mySlideRepository.findById(id)
            if (!mySlideOption.isPresent) {
                return ResultUtil.failure(msg = "不存在该slide,无法修改")
            }
            val mySlide = mySlideOption.get()
            mySlide.title = title
            mySlide.description = description
            mySlide.link = link
            mySlide.image = base64
            mySlide.updateTime = Date().time
            mySlide.updater = userDetail.wid
            mySlideRepository.save(mySlide)
            return ResultUtil.success()
        }
    }

    fun delSlide(id: String): ApiResult<*> {
        val userDetail = UserContextHolder.getUserContext() ?: return ResultUtil.failure(msg = "用户没有权限")
        val roles = userDetail.roles
        if (PlayerType.ADMIN_SUPER.name !in roles) {
            return ResultUtil.failure(msg = "用户没有权限删除文件")
        }
        mySlideRepository.deleteById(id)
        return ResultUtil.success()
    }

    fun getSlideList(): ApiResult<*> {
        val mySlides = mySlideRepository.findAll(Sort.by(Sort.Direction.DESC, "updateTime"))
        return if (mySlides.isNullOrEmpty()) {
            ResultUtil.failure()
        } else {
            ResultUtil.success(data = mySlides)
        }
    }
}
