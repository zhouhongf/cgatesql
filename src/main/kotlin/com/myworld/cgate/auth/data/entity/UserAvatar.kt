package com.myworld.cgate.auth.data.entity

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "user_avatar")
class UserAvatar(
    @javax.persistence.Id
    @Column(name = "id")
    var wid: String? = null,
    var fileName: String? = null,
    var extensionType: String? = null,
    @Column(name = "file_byte", columnDefinition = "LONGBLOB COMMENT '文件格式'")
    var fileByte: ByteArray? = null,
    var status: String? = null
) : Serializable
