package com.myworld.cgate.service

import com.myworld.cgate.auth.data.repository.VisitWatchRepository
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class VisitWatchService {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var visitWatchRepository: VisitWatchRepository

    // 每天晚上11点半开始执行定时任务
    @Scheduled(cron = "0 30 23 * * ?")
    fun clearVisitWatchDB() {
        visitWatchRepository.deleteAll()
    }
}
