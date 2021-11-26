package com.myworld.cgate.auth.submail.lib

import com.myworld.cgate.auth.submail.config.AppConfig
import com.myworld.cgate.auth.submail.lib.base.ISender
import net.sf.json.JSONObject
import org.apache.logging.log4j.LogManager

/**
 * essage/xsend  提供完整且强大的短信发送功能，区别在于，message/xsend
 * 无需提交短信内容和短信签名，仅需提交你在 SUBMAIL MESSAGE 应用程序中创
 * 建的短信项目的标记（请参见 获取项目或地址薄的开发者标识），并可以使用文
 * 本变量动态的控制每封短信的内容。 了解如何使用文本变量。
 */
class MESSAGEXsend(config: AppConfig) {
    private val log = LogManager.getRootLogger()
    companion object {
        const val ADDRESSBOOK = "addressbook"
        const val TO = "to"
        const val PROJECT = "project"
        const val VARS = "vars"
        const val LINKS = "links"
        const val COMMA = ","
    }
    var requestData : MutableMap<String, Any?> = HashMap()

    var config: AppConfig? = null
    init {
        this.config = config
    }

    fun addWithComma(key: String, value: String) {
        if (key.isEmpty()) return
        if (requestData.containsKey(key)) {
            val item = COMMA + value
            requestData[key] = requestData[key].toString() + item
        } else {
            requestData[key] = value
        }
    }

    @Throws(Exception::class)
    fun addWithJson(key: String, jKey: String, jValue: String) {
        if (key.isEmpty()) return
        val json = if (requestData.containsKey(key)) {
            val value = requestData[key]
            JSONObject.fromObject(value)
        } else {
            JSONObject()
        }
        if (json != null) {
            json[jKey] = jValue
            requestData[key] = json.toString()
        }
    }


    fun addTo(address: String) {
        addWithComma(TO, address)
    }

    fun addAddressBook(addressbook: String) {
        addWithComma(ADDRESSBOOK, addressbook)
    }

    fun setProject(project: String) {
        requestData[PROJECT] = project
    }

    fun addVar(key: String, value: String) {
        addWithJson(VARS, key, value)
    }

    fun getSender(): ISender {
        return Message(this.config)
    }

    fun xsend(): String? {
        log.info("============ requestData是：{}", requestData)
        val sender = getSender()
        return sender.xsend(requestData)
    }
}
