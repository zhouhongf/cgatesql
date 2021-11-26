package com.myworld.cgate.siteinfo.repository

import com.myworld.cgate.siteinfo.entity.Writing
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface WritingRepository : JpaRepository<Writing, String> {

    fun findByType(type: String): MutableList<Writing>?
    fun findByCanReleaseAndTitle(canRelease: Boolean = true, title: String): Writing?
    fun findByCanReleaseAndTypeAndAuthor(canRelease: Boolean = true, type: String, author: String, pageable: Pageable): Page<Writing>?
}
