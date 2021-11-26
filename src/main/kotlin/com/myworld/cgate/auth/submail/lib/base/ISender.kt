package com.myworld.cgate.auth.submail.lib.base

interface ISender {
    /**
     * 发送请求数据
     * @param data{@link HashMap}
     * @return 如果发送成功,返回true，发生错误,返回false。
     */
    fun send(data: Map<String, Any?>): String?
    fun xsend(data: Map<String, Any?>): String?
    fun subscribe(data: Map<String, Any?>): String?
    fun unsubscribe(data: Map<String, Any?>): String?
}
