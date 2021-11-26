package com.myworld.cgate.controller

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FallbackController {
    @GetMapping("/fallback")
    // @HystrixCommand(commandKey = "fallbackcmd")
    fun fallback(): String {
        return "Spring Cloud Gateway Fallback!"
    }
}
