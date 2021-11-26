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
@Table(name = "writing")
class Writing(
    @javax.persistence.Id
    var id: String? = null,
    var title: String? = null,
    var author: String? = null,
    var type: String? = null,
    @Column(name = "file_byte", columnDefinition = "LONGTEXT COMMENT '大文本'")
    var content: String? = null,
    var canRelease: Boolean = false,

    @JsonIgnore
    var createTime: Long = Date().time,
    @JsonIgnore
    var updater: String? = null,
    var updateTime: Long = Date().time
) : Serializable
