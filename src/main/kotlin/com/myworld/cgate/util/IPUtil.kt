package com.myworld.cgate.util

import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import org.lionsoul.ip2region.DataBlock
import org.lionsoul.ip2region.DbConfig
import org.lionsoul.ip2region.DbSearcher
import org.lionsoul.ip2region.Util
import org.springframework.http.server.reactive.ServerHttpRequest
import java.io.File
import java.lang.reflect.Method
import java.net.InetSocketAddress
import java.util.*
import javax.servlet.http.HttpServletRequest

object IPUtil {
    private val log = LogManager.getRootLogger()

    @JvmStatic
    fun getCityInfo(ip: String?): String? {
        try {
            log.info("需要查找地区的IP地址是$ip")
            var dbPath = IPUtil::class.java.getResource("/ip2region.db").path
            log.info("一开始的ip2region文件地址为$dbPath")
            var file = File(dbPath)
            if (!file.exists()) {
                log.info("未能找到一开始的ip2region.db文件")
                val tmpDir = System.getProperties().getProperty("java.io.tmpdir")
                dbPath = "$tmpDir/ip2region.db"
                log.info("调整后的ip2region文件路径为：$dbPath")
                file = File(dbPath)
                // FileUtils.copyInputStreamToFile(IPUtil::class.java.classLoader.getResourceAsStream("classpath:ip2region.db"), file)
                FileUtils.copyInputStreamToFile(Thread.currentThread().contextClassLoader.getResourceAsStream("ip2region.db"), file)
            }
            //查询算法
            val algorithm = DbSearcher.BTREE_ALGORITHM          //B-tree
            //DbSearcher.BINARY_ALGORITHM                       //Binary
            //DbSearcher.MEMORY_ALGORITYM                       //Memory
            try {
                val config = DbConfig()
                val searcher = DbSearcher(config, dbPath)
                //define the method
                var method: Method? = null
                when (algorithm) {
                    DbSearcher.BTREE_ALGORITHM -> method = searcher.javaClass.getMethod("btreeSearch", String::class.java)
                    DbSearcher.BINARY_ALGORITHM -> method = searcher.javaClass.getMethod("binarySearch", String::class.java)
                    DbSearcher.MEMORY_ALGORITYM -> method = searcher.javaClass.getMethod("memorySearch", String::class.java)
                }
                // 以下IP赋值仅为测试使用
                // ip = "117.136.67.80";
                val dataBlock: DataBlock
                if (!Util.isIpAddress(ip)) {
                    log.info("错误: 无效的ip地址")
                }
                dataBlock = method!!.invoke(searcher, ip) as DataBlock
                val regionBack = dataBlock.region
                log.info("查询到的region是：$regionBack")
                return regionBack
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun getIpAddress(request: HttpServletRequest): String {
        var ip = request.getHeader("x-forwarded-for")
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        log.info("ip地址为：$ip")
        return ip
    }

    fun getRegionOnIpAddress(ip: String?): String? {
        val fullName = getCityInfo(ip)
        if (fullName == null) {
            log.info("cannot get city location or name")
            return null
        }
        log.info("ip belongs cityname:$fullName")
        return fullName
    }

    @JvmStatic
    fun getIpAddressReactive(request: ServerHttpRequest): String {
        val remoteAddress: InetSocketAddress = request.remoteAddress!!
        val ip: String = remoteAddress.address.hostAddress
        log.info("ip地址为：$ip")
        return ip
    }
}
