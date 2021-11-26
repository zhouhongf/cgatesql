package com.myworld.cgate.auth.authenticate.service.image


import com.myworld.cgate.auth.authenticate.service.ValidateCodeGenerator
import com.myworld.cgate.auth.authenticate.vo.ImageCode
import com.myworld.cgate.common.SecurityProperties
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ServerWebExchange
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.util.*

/**
 * 图片验证码生成器
 */
class ImageCodeGenerator : ValidateCodeGenerator {
    private val log = LogManager.getRootLogger()
    private val codeSequence = charArrayOf('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
        'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    @Autowired
    lateinit var securityProperties: SecurityProperties

    /**
     * 浏览器覆盖配置文件，覆盖默认
     */
    override fun generate(ctx: ServerWebExchange): ImageCode {
        val width = securityProperties.code.image.width
        val height = securityProperties.code.image.height
        // 定义图像buffer
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        // 创建一个随机数生成器类
        val random = Random()

        g.color = getRandColor(200, 250)
        g.fillRect(0, 0, width, height)                               //填充图像颜色

        g.font = Font("Times New Roman", Font.ITALIC, 20)       // 创建字体，字体的大小应该根据图片的高度来定
        g.color = Color.BLACK                                               // 画边框
        g.drawRect(0, 0, width - 1, height - 1)
        g.color = getRandColor(160, 200)                            // 随机产生155条干扰线，使图象中的认证码不易被其它程序探测到。
        for (i in 0..154) {
            val x = random.nextInt(width)
            val y = random.nextInt(height)
            val xl = random.nextInt(12)
            val yl = random.nextInt(12)
            g.drawLine(x, y, x + xl, y + yl)
        }
        //随机产生数字验证码。
        var sRand: String? = ""
        for (i in 0 until securityProperties.code.image.length) {
            val rand = codeSequence[random.nextInt(35)].toString()
            sRand += rand
            // 用随机产生的颜色将验证码绘制到图像中。
            g.color = Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110))
            g.drawString(rand, 13 * i + 6, 16)
        }
        g.dispose()
        val theSessionId = ctx.session.block()?.id
        val expireIn = securityProperties.code.image.expireIn
        val expireTime = LocalDateTime.now().plusSeconds(expireIn.toLong())
        val imageCode = ImageCode(image, sRand, expireTime, theSessionId)
        log.info("ImageCodeGenerator的generate方法制作的图形验证码是：{}, sessionId是：{}, 有效期是：{}", imageCode.code, imageCode.sessionId, imageCode.expireTime)
        return imageCode
    }

    /**
     * 生成随机背景条纹
     */
    private fun getRandColor(fcRaw: Int, bcRaw: Int): Color {
        var fc = fcRaw
        var bc = bcRaw
        val random = Random()
        if (fc > 255) {
            fc = 255
        }
        if (bc > 255) {
            bc = 255
        }
        val r = fc + random.nextInt(bc - fc)
        val g = fc + random.nextInt(bc - fc)
        val b = fc + random.nextInt(bc - fc)
        return Color(r, g, b)
    }
}
