package com.myworld.cgate.auth.data.entity

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.Id
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "userinfo")
class UserInfo(
    /**
     * 编号MYUSER+系统时间
     */
    @javax.persistence.Id
    @Column(name = "id")
    var wid: String? = null,
    @Column(name = "token", columnDefinition = "MEDIUMTEXT COMMENT '长文本文件'")
    var token: String? = null,

    var expireTime: Long? = null,
    var lastLoginTime: Long = Date().time,
    var lastLoginUserAgent: String? = null,
    var lastLoginReferer: String? = null,
    var lastLoginIp: String? = null,
    var lastLoginCity: String? = null
) : Serializable
