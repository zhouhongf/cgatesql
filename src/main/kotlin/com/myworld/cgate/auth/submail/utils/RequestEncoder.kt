package com.myworld.cgate.auth.submail.utils

import java.security.MessageDigest

/**
 * 处理请求数据
 */
object RequestEncoder {
    val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    const val MD5 = "MD5"
    const val SHA1 = "SHA1"

    /**
     * 编码的字符串
     *
     * @param algorithm
     * @param str
     * @return String
     */
    fun encode(algorithm: String, str: String?): String? {
        return if (str == null) {
            null
        } else try {
            val messageDigest = MessageDigest.getInstance(algorithm)
            messageDigest.update(str.toByteArray())
            getFormattedText(messageDigest.digest())
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * 获取原始字节并将其格式化。
     * @param bytes
     * the raw bytes from the digest.
     * @return the formatted bytes.
     */
    fun getFormattedText(bytes: ByteArray): String {
        val len = bytes.size
        val buf = StringBuilder(len * 2)
        for (j in 0 until len) {
            buf.append(HEX_DIGITS[bytes[j].toInt() shr 4 and 0x0f])
            buf.append(HEX_DIGITS[bytes[j].toInt() and 0x0f])
        }
        return buf.toString()
    }

    fun formatRequest(data: Map<String, Any?>): String? {
        val keySet = data.keys
        val it = keySet.iterator()
        val sb = StringBuffer()
        while (it.hasNext()) {
            val key = it.next()
            val value = data[key]
            if (value is String) {
                sb.append("$key=$value&")
            }
        }
        return if (sb.length != 0) {
            sb.substring(0, sb.length - 1)
        } else null
    }
}
