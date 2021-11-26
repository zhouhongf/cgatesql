package com.myworld.cgate.auth.data.entity


import org.springframework.data.annotation.Id
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "pcds")
class Pcds(
    @javax.persistence.Id
    var id: Long? = null,
    var fullname: String? = null,
    var citycode: String? = null,
    var adcode: String? = null,
    var name: String? = null,
    var center: String? = null,
    var level: String? = null
) : Serializable
