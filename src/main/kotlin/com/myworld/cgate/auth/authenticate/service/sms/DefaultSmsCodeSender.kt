package com.myworld.cgate.auth.authenticate.service.sms

import com.myworld.cgate.auth.submail.lib.MESSAGEXsend
import com.myworld.cgate.auth.submail.utils.ConfigLoader
import org.apache.logging.log4j.LogManager


class DefaultSmsCodeSender : SmsCodeSender {
    private val log = LogManager.getRootLogger()

    override fun send(mobile: String, code: String) {
        log.info("DefaultSmsCodeSender的send方法，向手机" + mobile + "发送短信验证码" + code)
        //以下代码，正式运行时，再使用。
        val config = ConfigLoader.load(ConfigLoader.ConfigType.Message)
        val submail = config?.let { MESSAGEXsend(it) }
        if (submail != null) {
            submail.addTo(mobile)
            submail.setProject("RUCsa1")
            submail.addVar("code", code)
            submail.xsend()
        } else {
            log.error("错误：MESSAGEXsend(config)为空值")
        }
    }
}
