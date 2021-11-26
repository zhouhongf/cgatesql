package com.myworld.cgate.config

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
open class DataSourceConfig {

    @Bean
    open fun dataSource(environment: Environment?): DataSource {
        // return DruidDataSourceBuilder.create().build(environment, "spring.datasource.druid.");
        return DruidDataSourceBuilder.create().build()
    }
}
