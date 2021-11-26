package com.myworld.cgate.util

import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.NoHttpResponseException
import org.apache.http.ParseException
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.config.RegistryBuilder
import org.apache.http.config.SocketConfig
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.LayeredConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.pool.PoolStats
import org.apache.http.protocol.HTTP
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

object HttpUtil {

    private val logger = LoggerFactory.getLogger(HttpUtil::class.java)
    private const val CONNECT_TIMEOUT = 4000                                // 连接超时毫秒
    private const val SOCKET_TIMEOUT = 10000                                // 传输超时毫秒
    private const val REQUESTCONNECT_TIMEOUT = 3000                         // 获取请求超时毫秒
    private const val CONNECT_TOTAL = 200                                   // 最大连接数
    private const val CONNECT_ROUTE = 20                                    // 每个路由基础的连接数
    private const val ENCODE_CHARSET = "utf-8"                              // 响应报文解码字符集
    private const val RESP_CONTENT = "通信失败"
    private var connManager: PoolingHttpClientConnectionManager? = null
    private var httpClient: CloseableHttpClient? = null
    fun getHttpClient(): CloseableHttpClient? {
        if (null == httpClient) {
            synchronized(HttpUtil::class.java) {
                if (null == httpClient) {
                    httpClient = init()
                }
            }
        }
        return httpClient
    }

    private fun init(): CloseableHttpClient? {
        var newHttpclient: CloseableHttpClient? = null
        val plainsf: ConnectionSocketFactory = PlainConnectionSocketFactory.getSocketFactory()
        val sslsf: LayeredConnectionSocketFactory? = createSSLConnSocketFactory()
        val registry = RegistryBuilder.create<ConnectionSocketFactory?>().register("http", plainsf).register("https", sslsf).build()
        connManager = PoolingHttpClientConnectionManager(registry)
        // 将最大连接数增加到200
        connManager!!.maxTotal = CONNECT_TOTAL
        // 将每个路由基础的连接增加到20
        connManager!!.defaultMaxPerRoute = CONNECT_ROUTE
        // 可用空闲连接过期时间,重用空闲连接时会先检查是否空闲时间超过这个时间，如果超过，释放socket重新建立
        connManager!!.validateAfterInactivity = 30000
        // 设置socket超时时间
        val socketConfig = SocketConfig.custom().setSoTimeout(SOCKET_TIMEOUT).build()
        connManager!!.defaultSocketConfig = socketConfig
        val requestConfig = RequestConfig.custom().setConnectionRequestTimeout(REQUESTCONNECT_TIMEOUT).setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build()
        val httpRequestRetryHandler = HttpRequestRetryHandler { exception, executionCount, context ->
            if (executionCount >= 3) {                          // 如果已经重试了3次，就放弃
                return@HttpRequestRetryHandler false
            }
            if (exception is NoHttpResponseException) {         // 如果服务器丢掉了连接，那么就重试
                return@HttpRequestRetryHandler true
            }
            if (exception is SSLHandshakeException) {           // 不要重试SSL握手异常
                return@HttpRequestRetryHandler false
            }
            if (exception is InterruptedIOException) {          // 超时
                return@HttpRequestRetryHandler true
            }
            if (exception is UnknownHostException) {            // 目标服务器不可达
                return@HttpRequestRetryHandler false
            }
            if (exception is ConnectTimeoutException) {         // 连接被拒绝
                return@HttpRequestRetryHandler false
            }
            if (exception is SSLException) {                    // ssl握手异常
                return@HttpRequestRetryHandler false
            }
            val clientContext = HttpClientContext.adapt(context)
            val request = clientContext.request

            if (request is HttpEntityEnclosingRequest) {        // 如果请求是幂等的，就再次尝试
                false
            } else false
        }
        newHttpclient = HttpClients.custom().setConnectionManager(connManager).setDefaultRequestConfig(requestConfig).setRetryHandler(httpRequestRetryHandler).build()
        if (connManager != null && connManager!!.totalStats != null) {
            logger.info("【now client pool: {} 】", connManager!!.totalStats.toString())
        }
        return newHttpclient
    }

    /**
     * 发送HTTP_GET请求
     * @param reqURL 请求地址
     * @param param  请求参数
     * @return 远程主机响应正文
     * @see 1)该方法会自动关闭连接,释放资源
     * @see 2)方法内设置了连接和读取超时时间,单位为毫秒,超时或发生其它异常时方法会自动返回"通信失败"字符串
     * @see 3)请求参数含中文时,经测试可直接传入中文,HttpClient会自动编码发给Server,应用时应根据实际效果决定传入前是否转码
     * @see 4)该方法会自动获取到响应消息头中[Content-Type:text/html; charset=GBK]的charset值作为响应报文的解码字符集
     * @see 5)若响应消息头中无Content-Type属性,则会使用HttpClient内部默认的ISO-8859-1作为响应报文的解码字符集
     */
    fun sendGetRequest(reqURL: String, param: String?): String {
        var reqURLNew = reqURL
        if (null != param) {
            reqURLNew += "?$param"
        }
        val newHttpclient = getHttpClient()
        var respContent = RESP_CONTENT // 响应内容
        // reqURL = URLDecoder.decode(reqURL, ENCODE_CHARSET);
        val httpget = HttpGet(reqURLNew)
        var response: CloseableHttpResponse? = null
        try {
            response = newHttpclient!!.execute(httpget, HttpClientContext.create()) // 执行GET请求
            val entity = response.entity // 获取响应实体
            if (null != entity) {
                respContent = EntityUtils.toString(entity, "utf-8")
                EntityUtils.consume(entity)
            }
        } catch (cte: ConnectTimeoutException) {
            logger.error("请求通信[$reqURLNew]时连接超时,堆栈轨迹如下", cte)
        } catch (ste: SocketTimeoutException) {
            logger.error("请求通信[$reqURLNew]时读取超时,堆栈轨迹如下", ste)
        } catch (cpe: ClientProtocolException) { // 该异常通常是协议错误导致:比如构造HttpGet对象时传入协议不对(将'http'写成'htp')or响应内容不符合HTTP协议要求等
            logger.error("请求通信[$reqURLNew]时协议异常,堆栈轨迹如下", cpe)
        } catch (pe: ParseException) {
            logger.error("请求通信[$reqURLNew]时解析异常,堆栈轨迹如下", pe)
        } catch (ioe: IOException) { // 该异常通常是网络原因引起的,如HTTP服务器未启动等
            logger.error("请求通信[$reqURLNew]时网络异常,堆栈轨迹如下", ioe)
        } catch (e: Exception) {
            logger.error("请求通信[$reqURLNew]时偶遇异常,堆栈轨迹如下", e)
        } finally {
            try {
                response?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            httpget.releaseConnection()
        }
        return respContent
    }

    @JvmStatic
    fun sendGetRequestWithHeaders(reqURL: String, param: String?, headers: Map<String, String>): String {
        var reqURLNew = reqURL
        if (null != param) {
            reqURLNew += "?$param"
            logger.info("HttpClient的请求地址是：$reqURLNew")
        }
        val newHttpclient = getHttpClient()
        var respContent = RESP_CONTENT
        val httpget = HttpGet(reqURLNew)
        for ((key, value) in headers) {
            httpget.addHeader(key, value)
        }
        var response: CloseableHttpResponse? = null
        try {
            response = newHttpclient!!.execute(httpget, HttpClientContext.create()) // 执行GET请求
            val entity = response.entity // 获取响应实体
            if (null != entity) {
                respContent = EntityUtils.toString(entity, "utf-8")
                EntityUtils.consume(entity)
            }
        } catch (cte: ConnectTimeoutException) {
            logger.error("请求通信[$reqURLNew]时连接超时,堆栈轨迹如下", cte)
        } catch (ste: SocketTimeoutException) {
            logger.error("请求通信[$reqURLNew]时读取超时,堆栈轨迹如下", ste)
        } catch (cpe: ClientProtocolException) {                            // 该异常通常是协议错误导致:比如构造HttpGet对象时传入协议不对(将'http'写成'htp')or响应内容不符合HTTP协议要求等
            logger.error("请求通信[$reqURLNew]时协议异常,堆栈轨迹如下", cpe)
        } catch (pe: ParseException) {
            logger.error("请求通信[$reqURLNew]时解析异常,堆栈轨迹如下", pe)
        } catch (ioe: IOException) {                                        // 该异常通常是网络原因引起的,如HTTP服务器未启动等
            logger.error("请求通信[$reqURLNew]时网络异常,堆栈轨迹如下", ioe)
        } catch (e: Exception) {
            logger.error("请求通信[$reqURLNew]时偶遇异常,堆栈轨迹如下", e)
        } finally {
            try {
                response?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            httpget.releaseConnection()
        }
        return respContent
    }

    /**
     * 发送HTTP_POST请求 type: 默认是表单请求，
     * @param reqURL 请求地址
     * @param param  请求参数,若有多个参数则应拼接为param11=value11&22=value22&33=value33的形式
     * @param type   编码字符集,编码请求数据时用之,此参数为必填项(不能为""或null)
     * @return 远程主机响应正文
     * @see 1)该方法允许自定义任何格式和内容的HTTP请求报文体
     * @see 2)该方法会自动关闭连接,释放资源
     * @see 3)方法内设置了连接和读取超时时间,单位为毫秒,超时或发生其它异常时方法会自动返回"通信失败"字符串
     * @see 4)请求参数含中文等特殊字符时,可直接传入本方法,并指明其编码字符集encodeCharset参数,方法内部会自动对其转码
     * @see 5)该方法在解码响应报文时所采用的编码,取自响应消息头中的[Content-Type:text/html; charset=GBK]的charset值
     * @see 6)若响应消息头中未指定Content-Type属性,则会使用HttpClient内部默认的ISO-8859-1
     */
    @JvmOverloads
    fun sendPostRequest(reqURL: String, param: String?, type: String? = ""): String {
        val newHttpclient = getHttpClient()
        var result = RESP_CONTENT
        // 设置请求和传输超时时间
        val httpPost = HttpPost(reqURL)
        // 这就有可能会导致服务端接收不到POST过去的参数,比如运行在Tomcat6.0.36中的Servlet,所以我们手工指定CONTENT_TYPE头消息
        if (type.isNullOrEmpty()) {
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json; charset=$ENCODE_CHARSET")
        } else {
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=$ENCODE_CHARSET")
        }
        var response: CloseableHttpResponse? = null
        try {
            if (param != null) {
                val entity = StringEntity(param, ENCODE_CHARSET)
                httpPost.entity = entity
            }
            logger.info("开始执行请求：$reqURL")
            // reqURL = URLDecoder.decode(reqURL, ENCODE_CHARSET);
            response = newHttpclient!!.execute(httpPost, HttpClientContext.create())
            val entity = response.entity
            if (null != entity) {
                result = EntityUtils.toString(entity, "utf-8")
                logger.info("执行请求完毕：$result")
                EntityUtils.consume(entity)
            }
        } catch (cte: ConnectTimeoutException) {
            logger.error("请求通信[$reqURL]时连接超时,堆栈轨迹如下", cte)
        } catch (ste: SocketTimeoutException) {
            logger.error("请求通信[$reqURL]时读取超时,堆栈轨迹如下", ste)
        } catch (cpe: ClientProtocolException) {
            logger.error("请求通信[$reqURL]时协议异常,堆栈轨迹如下", cpe)
        } catch (pe: ParseException) {
            logger.error("请求通信[$reqURL]时解析异常,堆栈轨迹如下", pe)
        } catch (ioe: IOException) {
            logger.error("请求通信[$reqURL]时网络异常,堆栈轨迹如下", ioe)
        } catch (e: Exception) {
            logger.error("请求通信[$reqURL]时偶遇异常,堆栈轨迹如下", e)
        } finally {
            try {
                response?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            httpPost.releaseConnection()
        }
        return result
    }

    //SSL的socket工厂创建
    @Throws(IOException::class)
    private fun createSSLConnSocketFactory(): SSLConnectionSocketFactory? {
        val sslsf: SSLConnectionSocketFactory?
        // 创建TrustManager() 用于解决javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
        val trustManager: X509TrustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

            @Throws(CertificateException::class)
            override fun checkClientTrusted(arg0: Array<X509Certificate>, authType: String) { // TODO Auto-generated method stub
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(arg0: Array<X509Certificate>, authType: String) { // TODO Auto-generated method stub
            }
        }
        val sslContext: SSLContext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS)
        sslContext.init(null, arrayOf(trustManager as TrustManager), null)
        sslsf = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)       // 创建SSLSocketFactory ,不校验域名, 取代以前验证规则
        return sslsf
    }

    val connManagerStats: Map<HttpRoute, PoolStats>?
        get() {
            if (connManager != null) {
                val routeSet = connManager!!.routes
                if (routeSet != null && !routeSet.isEmpty()) {
                    val routeStatsMap: MutableMap<HttpRoute, PoolStats> = HashMap()
                    for (route in routeSet) {
                        val stats = connManager!!.getStats(route)
                        routeStatsMap[route] = stats
                    }
                    return routeStatsMap
                }
            }
            return null
        }

    val connManagerTotalStats: PoolStats?
        get() = if (connManager != null) {
            connManager!!.totalStats
        } else null

    /**
     * 关闭系统时关闭httpClient
     */
    fun releaseHttpClient() {
        try {
            httpClient!!.close()
        } catch (e: IOException) {
            logger.error("关闭httpClient异常$e")
        } finally {
            if (connManager != null) {
                connManager!!.shutdown()
            }
        }
    }

    /**
     * url参数转Map
     * 形如：access_token=example_token&scope=&token_type=bearer
     * @author jitwxs
     * @since 2018/5/21 16:47
     */
    fun params2Map(params: String): Map<String, String> {
        val map: MutableMap<String, String> = HashMap()
        val tmp = params.trim { it <= ' ' }.split("&").toTypedArray()
        for (param in tmp) {
            val kv = param.split("=").toTypedArray()
            if (kv.size == 2) {
                map[kv[0]] = kv[1]
            }
        }
        return map
    }
}
