package com.myworld.cgate.auth.submail.config

open class AppConfig {
    /**
     * Three kind of value of [.signType]
     */
    companion object {
        const val TYPE_NORMAL = "normal"
        const val TYPE_MD5 = "md5"
        const val TYPE_SHA1 = "sha1"
    }

    /**
     * 由用户提供的appId作为签名的一部分
     */
    var appId: String? = null

    /**
     * 由用户提供的appKey作为签名的一部分
     */
    var appKey: String? = null

    /**
     * Assign the type for encryption. md5:by md5 algorithm.If by this
     * algorithm,the format of signature is md5([.appId][.appKey]
     * <variable>RequestData</variable>[.appId][.appKey]); sha1:by
     * sha1 algorithm. normal:use the the value of [.appKey] without no
     * algorithm.It also the default value.
     */
    var signType: String? = null
        get() {
            return if (checkType(field)) {
                field
            } else {
                TYPE_NORMAL
            }
        }
        set(value) {
            field = value
        }

    /**
     * Judge the value of [.signType] is valid or not.
     *
     * @param signType
     * @return If the value is inside of {@value #TYPE_MD5},{@value #TYPE_SHA1}
     * and {@value #TYPE_NORMAL}, return true,otherwise return false;
     */
    fun checkType(signType: String?): Boolean {
        return TYPE_NORMAL == signType || TYPE_MD5 == signType || TYPE_SHA1 == signType
    }


}
