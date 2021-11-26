package com.myworld.cgate.service


import com.myworld.cgate.auth.data.entity.SysRole
import com.myworld.cgate.auth.data.repository.SysRoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SysRoleService {
    @Autowired
    private lateinit var sysRoleRepository: SysRoleRepository

    fun updateRole(name: String) {
        val optional = sysRoleRepository.findById(name)
        if (!optional.isPresent) {
            val sysRole = SysRole(name)
            sysRoleRepository.save(sysRole)
        }
    }
}
