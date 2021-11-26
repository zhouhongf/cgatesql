package com.myworld.cgate.util

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.myworld.cgate.common.SecurityConstants
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.util.*

object WeatherUtil {

    private val log = LogManager.getRootLogger()

    var cityCodes: String? = null
        get() {
            if (null == field) {
                synchronized(HttpUtil::class.java) {
                    if (null == field) {
                        field = init()
                    }
                }
            }
            return field
        }
        private set

    private const val url_prefix = "http://d1.weather.com.cn/sk_2d/"
    private const val referer_prefix = "http://www.weather.com.cn/weather1d/"

    private fun init(): String? {
        var newCityCodes: String? = null
        val resource: Resource = ClassPathResource("weatherCity.json")
        try {
            val inputStream = resource.inputStream
            val strList = IOUtils.readLines(inputStream, "utf8")
            val content = StringBuilder()
            for (line in strList) {
                content.append(line)
            }
            newCityCodes = content.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newCityCodes
    }

    @JvmStatic
    fun getCityCodeByCityName(cityName: String?): String? {
        var idNeed: String? = null
        val cityCodesContent = cityCodes
        val jsonArray = JSONArray.parseArray(cityCodesContent)
        for (`object` in jsonArray) {
            val jsonObject = `object` as JSONObject
            val theName = jsonObject.getString("name")
            val theId = jsonObject.getString("id")
            if (theName.contains(cityName!!)) {
                idNeed = theId
                break
            }
        }
        log.info("城市名称是：{}, 获取到的id是：{}", cityName, idNeed)
        return idNeed
    }

    @JvmStatic
    fun getWeatherByCityName(cityName: String?): String? {
        var weatherStr: String? = null
        val idNeed = getCityCodeByCityName(cityName)
        if (idNeed != null) {
            val url = "$url_prefix$idNeed.html"
            val referer = "$referer_prefix$idNeed.shtml"
            val headers: MutableMap<String, String> = HashMap()
            headers["User-Agent"] = SecurityConstants.DEFAULT_USER_AGENT
            headers["Referer"] = referer
            val currentTime = Date().time
            weatherStr = HttpUtil.sendGetRequestWithHeaders(url, "_=$currentTime", headers)
        }
        log.info("返回的天气数据是：{}", weatherStr)
        return weatherStr
    }
}
