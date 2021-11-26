package com.myworld.cgate.siteinfo.entity

import com.fasterxml.jackson.annotation.JsonIgnore
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
@Table(name = "myfile")
class MyFile(
    @javax.persistence.Id
    var id: String? = null,
    var fileName: String? = null,
    var extensionType: String? = null,
    var size: Long? = null,

    @JsonIgnore
    @Column(name = "file_byte", columnDefinition = "LONGBLOB COMMENT '文件格式'")
    var fileByte: ByteArray? = null,

    var versionNumber: String? = null,
    var officialName: String? = null,

    @JsonIgnore
    var createTime: Long = Date().time,
    var updateTime: Long? = null,
    @JsonIgnore
    var updater: String? = null
) : Serializable
