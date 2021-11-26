package com.myworld.cgate.auth.submail.lib

import com.myworld.cgate.auth.submail.config.AppConfig
import com.myworld.cgate.auth.submail.lib.base.ISender
import net.sf.json.JSONObject

/**
 * message/send API 不仅提供强大的短信发送功能,
 * 并在API中集成了地址薄发送功能。你可以通过设定一些参数来确定 API 以哪种模式发送。
 */
class MessageSend(config: AppConfig) {
    var config: AppConfig? = null
    companion object {
        const val TO = "to"
        const val CONTENT = "content"
        const val COMMA = ","
    }
    var requestData : MutableMap<String, Any?> = HashMap()
    init {
        this.config = config
    }

    fun addWithComma(key: String, value: String) {
        if (key.isEmpty()) return
        if (requestData.containsKey(key)) {
            val item = MESSAGEXsend.COMMA + value
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


    fun addTo(to: String) {
        addWithComma(TO, to)
    }

    fun addContent(content: String) {
        addWithComma(CONTENT, content)
    }

    fun getSender(): ISender {
        return Message(this.config)
    }

    fun send(): String? {
        return getSender().send(requestData)
    }
}
