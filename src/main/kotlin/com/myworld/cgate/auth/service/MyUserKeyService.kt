package com.myworld.cgate.auth.service

import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.LogManager
import java.math.BigInteger
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec


object MyUserKeyService {
    private val log = LogManager.getRootLogger()
    //密钥 (需要前端和后端保持一致)
    private const val CKEY = "myworldmypasskey"
    //算法
    private const val ALGORITHMSTR = "AES/ECB/PKCS5Padding"

    /**
     * username解密并去掉salt
     */
    @JvmStatic
    fun getRealUsername(value: String): String {
        var valueNew = value
        valueNew = valueNew.replace(" ", "+")
        log.info("【原始的用户名是: {}】", valueNew)
        valueNew = aesDecrypt(valueNew)!!
        val one = valueNew[0].toString()
        val two = valueNew.substring(2, 4)
        val three = valueNew.substring(6, 9)
        val four = valueNew.substring(12, 16)
        val five = valueNew[20].toString()
        log.info("【解密后的用户名是: {}】", one + two + three + four + five)
        return one + two + three + four + five
    }

    /**
     * password解密并去掉salt
     */
    @JvmStatic
    fun getRealPassword(value: String): String {
        var valueNew = value
        valueNew = valueNew.replace(" ", "+")
        log.info("【原始的密码是: {}】", value)
        valueNew = aesDecrypt(valueNew)!!
        val one = valueNew[0].toString()
        val two = valueNew[2].toString()
        val three = valueNew[5].toString()
        val four = valueNew[9].toString()
        val five = valueNew[14].toString()
        val six = valueNew[20].toString()
        val theRest = valueNew.substring(27)
        log.info("【解密后的密码是: {}】", one + two + three + four + five + six + theRest)
        return one + two + three + four + five + six + theRest
    }

    @JvmStatic
    fun encodeUserWid(value: String): String {
        log.info("【真实的idDetail是：{}】", value)
        val thePrefix = value.substring(0, 6)
        val theBody = value.substring(6)
        val one = theBody.substring(0, 3)
        val two = theBody.substring(3, 6)
        val three = theBody.substring(6, 9)
        val four = theBody.substring(9, 12)
        val five = theBody.substring(12, 15)
        val six = theBody.substring(15)
        val theBodies = arrayOf(one, two, three, four, five, six)
        val bodyList = listOf(*theBodies)
        var theBodyNew = ""
        val random = Random()
        for (i in 0..5) {
            val randNum = random.nextInt(10).toString()
            theBodyNew = theBodyNew + bodyList[i] + randNum
        }
        val userIdDetailSalt = thePrefix + theBodyNew
        log.info("加盐和拼接后的idDetail是：{}", userIdDetailSalt)
        val userIdDetailCoded = aesEncrypt(userIdDetailSalt)
        log.info("加密后的userIdDetailCoded 是：{}", userIdDetailCoded)
        return userIdDetailCoded
    }

    @JvmStatic
    fun decodeUserWid_back(value: String): String {
        var valueNew = value
        valueNew = valueNew.replace(" ", "+")
        valueNew = aesDecrypt(valueNew)!!
        log.info("【解密出来的idDetail是: {}】", valueNew)
        val thePrefix = valueNew.substring(0, 6)
        val theBody = valueNew.substring(6)
        val one = theBody.substring(0, 3)
        val two = theBody.substring(4, 7)
        val three = theBody.substring(8, 11)
        val four = theBody.substring(12, 15)
        val five = theBody.substring(16, 19)
        val six = theBody.substring(20, 22)
        val userIdDetailReal = thePrefix + one + two + three + four + five + six
        log.info("去掉盐后的userIdDetail是：{}", userIdDetailReal)
        return userIdDetailReal
    }




    /**
     * aes解密
     */
    @JvmStatic
    fun aesDecrypt(encrypt: String): String? {
        return try {
            aesDecrypt(encrypt, CKEY)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    /**
     * 将base 64 code AES解密
     * @param encryptStr 待解密的base 64 code
     * @param decryptKey 解密密钥
     * @return 解密后的string
     */
    @Throws(Exception::class)
    fun aesDecrypt(encryptStr: String?, decryptKey: String): String? {
        return if (encryptStr.isNullOrEmpty()) null else aesDecryptByBytes(base64Decode(encryptStr), decryptKey)
    }
    /**
     * base 64 decode
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     */
    @Throws(Exception::class)
    fun base64Decode(base64Code: String?): ByteArray? {
        return if (base64Code.isNullOrEmpty()) null else Base64.decodeBase64(base64Code)
    }
    /**
     * AES解密
     * @param encryptBytes 待解密的byte[]
     * @param decryptKey 解密密钥
     * @return 解密后的String
     */
    @Throws(Exception::class)
    fun aesDecryptByBytes(encryptBytes: ByteArray?, decryptKey: String): String {
        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(128)
        val cipher = Cipher.getInstance(ALGORITHMSTR)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decryptKey.toByteArray(), "AES"))
        val decryptBytes = cipher.doFinal(encryptBytes)
        return String(decryptBytes)
    }



    /**
     * aes加密
     */
    @JvmStatic
    fun aesEncrypt(content: String): String {
        return try {
            aesEncrypt(content, CKEY)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    /**
     * AES加密为base 64 code
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的base 64 code
     */
    @Throws(Exception::class)
    fun aesEncrypt(content: String, encryptKey: String): String {
        return base64Encode(aesEncryptToBytes(content, encryptKey))
    }
    /**
     * base 64 encode
     * @param bytes 待编码的byte[]
     * @return 编码后的base 64 code
     */
    fun base64Encode(bytes: ByteArray?): String {
        return Base64.encodeBase64String(bytes)
    }
    /**
     * AES加密
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的byte[]
     */
    @Throws(Exception::class)
    fun aesEncryptToBytes(content: String, encryptKey: String): ByteArray {
        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(128)
        val cipher = Cipher.getInstance(ALGORITHMSTR)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptKey.toByteArray(), "AES"))
        return cipher.doFinal(content.toByteArray(charset("utf-8")))
    }




    /**
     * 将byte[]转为各种进制的字符串
     * @param bytes byte[]
     * @param radix 可以转换进制的范围，从Character.MIN_RADIX到Character.MAX_RADIX，超出范围后变为10进制
     * @return 转换后的字符串
     */
    @JvmStatic
    fun binary(bytes: ByteArray?, radix: Int): String {
        return BigInteger(1, bytes).toString(radix) // 这里的1代表正数
    }
}
