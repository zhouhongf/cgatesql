package com.myworld.cgate.auth.submail.lib.base

import com.myworld.cgate.auth.submail.config.AppConfig
import com.myworld.cgate.auth.submail.utils.RequestEncoder
import net.sf.json.JSONObject
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HTTP
import org.apache.http.util.EntityUtils
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException

open class Sender : ISender {
    private val log = LogManager.getRootLogger()
    companion object {
        private const val API_TIMESTAMP = "http://api.submail.cn/service/timestamp.json"
        const val APPID = "appid"
        const val TIMESTAMP = "timestamp"
        const val SIGN_TYPE = "sign_type"
        const val SIGNATURE = "signature"
        const val APPKEY = "appkey"
    }

    var config: AppConfig? = null
    var closeableHttpClient: CloseableHttpClient = HttpClientBuilder.create().build()

    override fun send(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return null
    }

    override fun xsend(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return null
    }

    override fun subscribe(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return null
    }

    override fun unsubscribe(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return null
    }

    /**
     * 请求时间戳
     * @return timestamp
     */
    @Throws(IOException::class, ClientProtocolException::class)
    fun getTimestamp(): String? {
        val httpget = HttpGet(API_TIMESTAMP)
        val response: HttpResponse = closeableHttpClient.execute(httpget)
        val httpEntity = response.getEntity()
        val jsonStr = EntityUtils.toString(httpEntity, "UTF-8")
        if (jsonStr != null) {
            val json = JSONObject.fromObject(jsonStr)
            return json.getString("timestamp")
        }
        closeableHttpClient.close()
        return null
    }

    fun createSignature(data: String): String? {
        return if (AppConfig.TYPE_NORMAL == config!!.signType) {
            config!!.appKey
        } else {
            buildSignature(data)
        }
    }

    /**
     * 当 [AppConfig.setSignType] 不正常时,创建
     * 一个签名类型
     *
     * @param data
     * 请求数据
     * @return signature
     */
    fun buildSignature(data: String): String? {
        val app = config!!.appId
        val appKey = config!!.appKey
        // order is confirmed
        val jointData = app + appKey + data + app + appKey
        if (AppConfig.TYPE_MD5 == config!!.signType) {
            return RequestEncoder.encode(RequestEncoder.MD5, jointData)
        } else if (AppConfig.TYPE_SHA1 == config!!.signType) {
            return RequestEncoder.encode(RequestEncoder.SHA1, jointData)
        }
        return null
    }

    /**
     * 请求数据 post提交
     *
     * @param url
     * @param data
     * @return boolean
     */
    @Throws(IOException::class, ClientProtocolException::class)
    fun request(url: String, data: MutableMap<String, Any?>): String? {
        log.info("================ url是：{}", url)
        log.info("================ data是：{}", data)
        val httpPost = HttpPost(url)
        httpPost.addHeader("charset", "UTF-8")
        httpPost.entity = build(data)
        val response: HttpResponse = closeableHttpClient.execute(httpPost)
        val httpEntity = response.entity
        if (httpEntity != null) {
            val jsonStr = EntityUtils.toString(httpEntity, "UTF-8")
            log.info("================ 返回的jsonStr是：{}", jsonStr)
            return jsonStr
        }
        closeableHttpClient.close()
        return null
    }

    /**
     * 将请求数据转换为HttpEntity
     *
     * @param data
     * @return HttpEntity
     */
    fun build(data: MutableMap<String, Any?>): HttpEntity {
        val builder = MultipartEntityBuilder.create()
        builder.addTextBody(APPID, config!!.appId)
        // builder.setCharset(Charset.);
        builder.addTextBody(TIMESTAMP, getTimestamp())
        builder.addTextBody(SIGN_TYPE, config!!.signType)
        // set the properties below for signature
        data[APPID] = config!!.appId
        data[TIMESTAMP] = getTimestamp()
        data[SIGN_TYPE] = config!!.signType
        val contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8)
        val textBodyData = RequestEncoder.formatRequest(data)?.let { createSignature(it) }
        builder.addTextBody(SIGNATURE, textBodyData, contentType)

        for ((key, value) in data) {
            if (value is String) {
                builder.addTextBody(key, value.toString(), contentType)
            } else if (value is File) {
                builder.addBinaryBody(key, value as File?)
            }
        }
        return builder.build()
    }
}
