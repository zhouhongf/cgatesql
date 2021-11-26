package com.myworld.cgate.auth.data.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.Id
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * 全部要设置默认值，这样可以自动构造无参的构造函数，否则JPA的转换时会提时无default construct的错误
 */
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "sysuser")
class SysUser(
    /**
     * 编号MYUSER+系统时间
     */
    @javax.persistence.Id
    @Column(name = "id")
    var wid: String? = null,
    var sysroles: String? = null,

    /**
     * 用户账号，即手机号
     */
    var username: String? = null,
    @JsonIgnore
    var usernameold: String? = null,
    @JsonIgnore
    var password: String? = null,
    @JsonIgnore
    var passwordold: String? = null,

    @JsonIgnore
    var creator: String? = null,
    @JsonIgnore
    var updater: String? = null,
    @JsonIgnore
    var createTime: Long = Date().time,
    var updateTime: Long = Date().time
) : Serializable
