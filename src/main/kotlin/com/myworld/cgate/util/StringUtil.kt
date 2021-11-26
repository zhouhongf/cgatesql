package com.myworld.cgate.util

import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 字符串处理类
 * submail中可能需要使用第二个方法
 */
object StringUtil {
    private val log = LogManager.getRootLogger()
    private val placeSuffix = arrayOf("自治州", "自治县", "自治旗", "联合旗", "市辖区", "地区", "辖区", "左旗", "右旗", "前旗", "后旗", "中旗", "街道", "新区", "高新区", "开发区", "省", "市", "区", "县")
    /**
     * 两种过滤匹配方式
     * 1、去掉特定后缀名称
     * 2、正则表达式匹配
     */
    @JvmStatic
    fun filterPlaceName(name: String): String {
        var nameNew = name
        val originName = name
        // 去掉特定后缀，如果剩余name字数大于等于2的，则返回
        for (suffix in placeSuffix) {
            if (nameNew.contains(suffix)) {
                nameNew = nameNew.replace(suffix, "")
            }
        }
        if (nameNew.length > 1) {
            log.info("去掉后缀后的城市名字是：$nameNew")
            return nameNew
        }
        // 如果去掉后缀后，name只剩下1个字了，则使用正则表达式重新匹配
        val pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,})[市|区|县|盟|旗]")
        val matcher = pattern.matcher(originName)
        return if (matcher.find()) matcher.group(1) else originName
    }

    @JvmStatic
    fun shortCityName(cityName: String): String { // 如果城市中包含 市 区 县， 则去掉市 区 县 的后缀
        val theCityName: String
        val pattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,})[市|区|县]")
        val m = pattern.matcher(cityName)
        theCityName = if (m.find()) {
            m.group(1)
        } else {
            cityName
        }
        return theCityName
    }

    @JvmStatic
    fun hasWhiteSpace(word: String): Boolean {
        val pattern = Pattern.compile("\\s+")
        val matcher = pattern.matcher(word.trim { it <= ' ' })
        return matcher.find()
    }

    @JvmStatic
    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("^(-|\\+)?\\d+(\\.\\d+)?$")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    @JvmStatic
    fun isInteger(str: String): Boolean {
        val pattern = Pattern.compile("^[0-9]+$")
        val isInt = pattern.matcher(str)
        return isInt.matches()
    }

    @JvmStatic
    fun isLetter(str: String): Boolean {
        val pattern = Pattern.compile("^[A-Za-z]+$")
        val isLet = pattern.matcher(str)
        return isLet.matches()
    }

    @JvmStatic
    fun isZhCN(str: String): Boolean {
        val pattern = Pattern.compile("^[\\u4e00-\\u9fa5]+$")
        val isZh = pattern.matcher(str)
        return isZh.matches()
    }

    @JvmStatic
    fun isChineseLetters(word: String): Boolean {
        val pattern = Pattern.compile("[\\u4e00-\\u9fa5]+")
        val matcher = pattern.matcher(word)
        return matcher.find()
    }

    @JvmStatic
    fun isWid(str: String): Boolean {
        val pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{6,18}$")
        val isWid = pattern.matcher(str)
        return isWid.matches()
    }

    /**
     * 利用正则表达式，获取tinymce中的图片链接地址
     */
    @JvmStatic
    fun getImgStr(htmlStr: String): Set<String> {
        val pics: MutableSet<String> = HashSet()
        var img = ""
        val p_image: Pattern
        val m_image: Matcher
        //  String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
        val regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>"
        p_image = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE)
        m_image = p_image.matcher(htmlStr)
        while (m_image.find()) { // 得到<img />数据
            img = m_image.group()
            // 匹配<img>中的src数据
            val m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img)
            while (m.find()) {
                pics.add(m.group(1))
            }
        }
        log.info("【提取出来的图片URL是】$pics")
        return pics
    }

    @JvmStatic
    fun base64ToBytes(base64: String): ByteArray {
        var base64New = base64
        base64New = base64New.replace("data:image/jpeg;base64,".toRegex(), "")
        return Base64.decodeBase64(base64New)
    }

    @JvmStatic
    fun bytesToBase64(bytes: ByteArray): String {
        val pngBase64 = Base64.encodeBase64String(bytes)
        return "data:image/jpeg;base64,$pngBase64"
    }

    @JvmStatic
    fun getFileMimeType(fileSuffix: String): String {
        val fileSuffixNet = fileSuffix.split(".")[1]
        var mimeType = "application/$fileSuffixNet"
        when (fileSuffixNet) {
            "text" -> mimeType = "text/plain"
            "docx" -> mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> mimeType = "application/msword"
            "pptx" -> mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> mimeType = "application/vnd.ms-powerpoint"
            "xlsx" -> mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> mimeType = "application/vnd.ms-excel"
            "zip" -> mimeType = "application/x-zip-compressed"
            "rar" -> mimeType = "application/octet-stream"
            "pdf" -> mimeType = "application/pdf"
            "jpg" -> mimeType = "image/jpeg"
            "png" -> mimeType = "image/png"
            "apk" -> mimeType = "application/vnd.android.package-archive"
            "html" -> mimeType = "text/html"
            "htm" -> mimeType = "text/html"
            "stm" -> mimeType = "text/html"
        }
        return mimeType
    }
}
