package com.myworld.cgate.auth.authenticate.service.image

import com.fasterxml.jackson.databind.ObjectMapper
import com.myworld.cgate.auth.authenticate.service.AbstractValidateCodeProcessor
import com.myworld.cgate.auth.authenticate.vo.ImageCode
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange



@Component("imageValidateCodeProcessor")
class ImageCodeProcessor : AbstractValidateCodeProcessor<ImageCode>() {

    private val log = LogManager.getRootLogger()
    private val objectMapper = ObjectMapper()
    /**
     * 发送图形验证码，将其写到相应中
     */
    @Throws(Exception::class)
    protected override fun send(ctx: ServerWebExchange, validateCode: ImageCode) {
        log.info("ImageCodeProcessor的send方法，发送验证码图片{}", validateCode.code)

        // ImageIO.write(imageCode.image, "JPEG", response.outputStream)
        // val response = request.response
        // response.contentType = "application/json;charset=UTF-8"
        // response.writer.write(objectMapper.writeValueAsString(ResultUtil.success(imageCode.image!!)))
        // response.writer.write(imageCode.code)
    }

}
