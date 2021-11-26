package com.myworld.cgate.auth.data.entity

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.annotation.Id
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Table

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "sysrole")
class SysRole(
    @javax.persistence.Id
    var name : String? = null
) : Serializable
