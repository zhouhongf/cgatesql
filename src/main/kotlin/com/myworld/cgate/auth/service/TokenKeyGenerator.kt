package com.myworld.cgate.auth.service

import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey


object TokenKeyGenerator {
    lateinit var publicKey: PublicKey
    lateinit var privateKey: PrivateKey
    //加载myworld.jks文件
    private val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("myworld.jks")


    init {
        try {
            val keyStore = KeyStore.getInstance("JKS")
            keyStore.load(inputStream, "myworldpass".toCharArray())
            privateKey = keyStore.getKey("myworld", "myworldpass".toCharArray()) as PrivateKey
            publicKey = keyStore.getCertificate("myworld").publicKey
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
