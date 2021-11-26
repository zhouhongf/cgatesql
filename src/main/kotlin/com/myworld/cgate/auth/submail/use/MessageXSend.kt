package com.myworld.cgate.auth.submail.use

import com.myworld.cgate.auth.submail.lib.MESSAGEXsend
import com.myworld.cgate.auth.submail.utils.ConfigLoader
import com.myworld.cgate.auth.submail.utils.ConfigLoader.load

/**
 * 使用示范，实际不派用场
 */
object MessageXSend {

    @JvmStatic
    fun main(args: Array<String>) {
        val config = load(ConfigLoader.ConfigType.Message)
        val submail = MESSAGEXsend(config!!)
        submail.addTo("13771880835")
        submail.setProject("RUCsa1")
        submail.addVar("code", "158955")
        val response = submail.xsend()
        println("接口返回数据：$response")
    }
}
