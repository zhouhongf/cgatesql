package com.myworld.cgate.auth.data.repository

import com.myworld.cgate.auth.data.entity.Pcds
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface PcdsRepository : JpaRepository<Pcds, Long> {

    fun findByNameAndLevel(name: String, level: String): List<Pcds>
    fun findByFullname(fullname: String): Pcds?

}
