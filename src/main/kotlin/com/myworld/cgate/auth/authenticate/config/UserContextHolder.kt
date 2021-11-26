package com.myworld.cgate.auth.authenticate.config

import com.myworld.cgate.common.CurrentUser

object UserContextHolder {
    var userContext = ThreadLocal<CurrentUser>()

    @JvmStatic
    fun getUserContext(): CurrentUser? {
        return userContext.get()
    }

    @JvmStatic
    fun setUserContext(currentUser: CurrentUser) {
        userContext.set(currentUser)
    }

    @JvmStatic
    fun shutdown() {
        userContext.remove()
    }
}
