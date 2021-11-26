package com.myworld.cgate.common


data class ApiResult<T>(
    val code: Int,
    val msg: String,
    val num: Long? = null,
    val data: T? = null
)

enum class ApiResultEnum(val code: Int, val msg: String) {
    // 枚举成员的构造方法，同枚举类的构造方法
    FAILURE(-2, "失败"),
    ERROR(-1, "未知错误"),
    SUCCESS(0, "成功"),
    UPDATE(9, "更新令牌");
}

data class CurrentUser(
    var wid: String,
    var roles: Set<*>,
    var token: String,
    var expireTime: Long
)

data class JwtToken(
    var tokenType: String,
    var wid: String,
    var roles: Set<*>,
    var expireTime: Long,
    var serviceNames: Set<*>,
    var groupName: String
)


enum class PlayerType {
    ADMIN_SUPER, USERS_CLIENT, USERS_BANKER, USERS_STUDENT
}

enum class AdminUsers {
    ADMIN, USERS
}

enum class ServiceName {
    DEFAULT, SWEALTH, SCHAT, SSTUDENT
}
