package com.myworld.cgate.common

interface SecurityConstants {
    companion object {
        // 以下6个常量为处理验证码登录使用
        const val HEADER_IMAGE_CODE = "imageCodeParam"
        /**
         * 验证图片验证码时，http请求中默认的携带图片验证码信息的参数的名称
         */
        const val DEFAULT_PARAMETER_NAME_CODE_IMAGE = "imageCode"
        /**
         * 验证短信验证码时，http请求中默认的携带短信验证码信息的参数的名称
         */
        const val DEFAULT_PARAMETER_NAME_CODE_SMS = "smsCode"
        /**
         * 发送短信验证码 或 验证短信验证码时，传递手机号的参数的名称
         */
        const val DEFAULT_PARAMETER_NAME_MOBILE = "mobile"
        /**
         * redis缓存验证码的keyhead
         */
        const val VALIDATE_CODE = ":VALIDATE:"
        /**
         * 默认的处理验证码的url前缀
         */
        const val DEFAULT_VALIDATE_CODE_URL_PREFIX = "/validatecode"



        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36"

        // 尽量都使用6位字母的前缀
        const val USER_ID_PREFIX = "MYUSER"
        // 虚拟用户前缀
        const val USER_VR_PREFIX = "VRUSER"
        const val MYTWEET_PREFIX = "MYWEET"

        const val WRITING_PREFIX = "MYWRIT"
        const val MYFILE_PREFIX = "MYFILE"
        const val MYSILDE_PREFIX = "MYSLID"

        const val FILE_MAX_SIZE = 16793600L
    }
}
