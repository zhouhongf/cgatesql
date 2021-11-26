package com.myworld.cgate.siteinfo.repository

import com.myworld.cgate.siteinfo.entity.MySlide
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface MySlideRepository : JpaRepository<MySlide, String> {
}
