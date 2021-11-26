package com.myworld.cgate.auth.data.repository

import com.myworld.cgate.auth.data.entity.VisitWatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface VisitWatchRepository : JpaRepository<VisitWatch, Long> {
    fun countByIpAddress(ip: String): Int
    fun deleteAllByIpAddress(ip: String)

    fun countByWid(wid: String): Int
    fun deleteAllByWid(wid: String)
}
