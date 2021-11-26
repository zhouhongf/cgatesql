package com.myworld.cgate.auth.data.repository


import com.myworld.cgate.auth.data.entity.UserAvatar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Transactional
@Repository
interface UserAvatarRepository : JpaRepository<UserAvatar, String> {
}
