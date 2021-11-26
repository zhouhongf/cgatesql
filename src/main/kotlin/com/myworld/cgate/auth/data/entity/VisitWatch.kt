package com.myworld.cgate.auth.data.entity

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.Id
import java.io.Serializable
import java.util.*
import javax.persistence.Entity
import javax.persistence.Table

/**
 * 存储在MongoDB中，每天0点定时清空
 * 如果一个IP，当天访问次数超过1000次，则列入黑名单
 * 如果一个WID，当然访问次数超过1000次，则列入黑名单
 */
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "visit_watch")
class VisitWatch(
    @javax.persistence.Id
    var id: Long = Date().time,

    var wid: String? = null,
    var ipAddress: String? = null,
    var city: String? = null,

    var visitUserAgent: String? = null,
    var visitReferer: String? = null,
    var visitPath: String? = null
) : Serializable
