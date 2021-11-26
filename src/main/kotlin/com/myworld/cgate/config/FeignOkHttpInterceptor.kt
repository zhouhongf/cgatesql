package com.myworld.cgate.config

import com.myworld.cgate.auth.service.JwtUtil
import okhttp3.Interceptor
import okhttp3.Response
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.IOException
import javax.servlet.http.HttpServletRequest


class FeignOkHttpInterceptor : Interceptor {

    private val httpServletRequest: HttpServletRequest?
        get() = try {
            val requestAttributes = RequestContextHolder.getRequestAttributes()
            (requestAttributes as ServletRequestAttributes).request
        } catch (e: Exception) {
            null
        }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val request = httpServletRequest
        if (request != null) {
            val authorizationValue = request.getHeader(JwtUtil.HEADER_AUTH)
            if (authorizationValue != null) {
                val requestWithAuthorization = originalRequest.newBuilder().addHeader(JwtUtil.HEADER_AUTH, authorizationValue).build()
                return chain.proceed(requestWithAuthorization)
            }
        }
        return chain.proceed(originalRequest)
    }


}
