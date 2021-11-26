package com.myworld.cgate.auth.authenticate.vo

import com.myworld.cgate.common.SecurityConstants


enum class ValidateCodeType {

    //匿名内部类
    SMS {
        override fun getParamNameOnValidate(): String {
            // 返回"smsCode"
            return SecurityConstants.DEFAULT_PARAMETER_NAME_CODE_SMS
        }
    },
    IMAGE {
        override fun getParamNameOnValidate(): String {
            // 返回"imageCode"
            return SecurityConstants.DEFAULT_PARAMETER_NAME_CODE_IMAGE
        }
    };

    /**
     * 因为枚举的构造方法是私有的，所以枚举类是不能有子类的，
     * 所以枚举严格上来说是不能定义抽象方法的，但是枚举类允许
     * 枚举常量以匿名内部类的形式实现枚举类中的抽象方法，所以枚举
     * 类中是能够定义抽象方法的（他是一种特例和放宽）
     * 但当定义抽象方法时，枚举类中的所有枚举常量都必须实现这个
     * 抽象方法
     */
    abstract fun getParamNameOnValidate(): String
}
