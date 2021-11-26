package com.myworld.cgate.siteinfo.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "myslide")
class MySlide(
    @javax.persistence.Id
    var id: String? = null,

    var title: String? = null,
    var description: String? = null,
    @Column(columnDefinition = "LONGTEXT COMMENT 'BASE64文件格式'")
    var image: String? = null,
    @Column(columnDefinition = "MEDIUMTEXT")
    var link: String? = null,

    var updateTime: Long? = Date().time,
    @JsonIgnore
    var updater: String? = null
) : Serializable
