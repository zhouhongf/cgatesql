package com.myworld.cgate.auth.data.repository

import com.myworld.cgate.auth.data.entity.SysUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface SysUserRepository : JpaRepository<SysUser, String> {

    fun findByUsername(username: String): SysUser?
    fun findBySysrolesContaining(playerType: String, pageable: Pageable): Page<SysUser>
}
