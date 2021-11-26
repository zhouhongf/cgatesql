package com.myworld.cgate.auth.submail.utils

import com.myworld.cgate.auth.submail.config.AppConfig
import com.myworld.cgate.auth.submail.config.MessageConfig
import org.apache.logging.log4j.LogManager
import java.util.*

object ConfigLoader {
    private val log = LogManager.getRootLogger()

    var pros: Properties = Properties()
    /**
     * 加载文件时，类载入，静态块内部的操作将被运行一次
     */
    init {
        pros.load(ConfigLoader::class.java.getResourceAsStream("/app_config.properties"))
    }

    /**
     * enum define two kinds of configuration.
     */
    enum class ConfigType {
        Mail, Message, Voice, Internationalsms, Mobiledata
    }

    /**
     * 外部类的静态方法，可以通过加载文件创建配置。
     */
    @JvmStatic
    fun load(type: ConfigType): AppConfig? {
        return when (type) {
            ConfigType.Message -> createMessageConfig()
            else -> null
        }
    }

    fun createMessageConfig(): AppConfig {
        val config: AppConfig = MessageConfig()
        config.appId = pros.getProperty(MessageConfig.APP_ID)
        config.appKey = pros.getProperty(MessageConfig.APP_KEY)
        config.signType = pros.getProperty(MessageConfig.APP_SIGNTYPE)
        log.info("=========== appId:{}， appKey:{}， signType:{}", config.appId, config.appKey, config.signType)
        return config
    }




}
