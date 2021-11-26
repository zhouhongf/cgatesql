package com.myworld.cgate.auth.submail.lib

import com.myworld.cgate.auth.submail.config.AppConfig
import com.myworld.cgate.auth.submail.lib.base.Sender
import org.apache.logging.log4j.LogManager

class Message(config: AppConfig?) : Sender() {
    private val log = LogManager.getRootLogger()
    companion object {
        private const val API_SEND = "http://api.submail.cn/message/send.json"
        private const val API_XSEND = "http://api.submail.cn/message/xsend.json"
        private const val API_SUBSCRIBE = "http://api.submail.cn/addressbook/message/subscribe.json"
        private const val API_UNSUBSCRIBE = "http://api.submail.cn/addressbook/message/unsubscribe.json"
    }

    init {
        this.config = config
    }
    /**
     * 发送请求数据到服务器,数据由两部分组成,其中一个是原始数据，另一个是签名
     */
    override fun send(data: Map<String, Any?>): String? {
        log.info("============== Message的send()方法中data是：{}", data)
        return request(API_SEND, data as MutableMap<String, Any?>)
    }

    override fun xsend(data: Map<String, Any?>): String? {
        log.info("============== Message的xsend()方法中data是：{}", data)
        return request(API_XSEND, data as MutableMap<String, Any?>)
    }

    override fun subscribe(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return request(API_SUBSCRIBE, data as MutableMap<String, Any?>)
    }

    override fun unsubscribe(data: Map<String, Any?>): String? {
        // TODO Auto-generated method stub
        return request(API_UNSUBSCRIBE, data as MutableMap<String, Any?>)
    }

}
