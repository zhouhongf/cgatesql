package com.myworld.cgate.config

import com.netflix.hystrix.HystrixThreadPoolKey
import com.netflix.hystrix.HystrixThreadPoolProperties
import com.netflix.hystrix.strategy.HystrixPlugins
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy
import com.netflix.hystrix.strategy.properties.HystrixProperty
import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Configuration
open class FeignHystrixConcurrencyStrategy : HystrixConcurrencyStrategy() {

    private val log = LogManager.getRootLogger()
    private lateinit var delegate: HystrixConcurrencyStrategy

    init {
        try {
            delegate = HystrixPlugins.getInstance().concurrencyStrategy
            if (delegate is FeignHystrixConcurrencyStrategy) {
                log.info("Welcome to singleton hell...")
            } else {
                val commandExecutionHook = HystrixPlugins.getInstance().commandExecutionHook
                val eventNotifier = HystrixPlugins.getInstance().eventNotifier
                val metricsPublisher = HystrixPlugins.getInstance().metricsPublisher
                val propertiesStrategy = HystrixPlugins.getInstance().propertiesStrategy
                logCurrentStateOfHystrixPlugins(eventNotifier, metricsPublisher, propertiesStrategy)
                HystrixPlugins.reset()
                HystrixPlugins.getInstance().registerConcurrencyStrategy(this)
                HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook)
                HystrixPlugins.getInstance().registerEventNotifier(eventNotifier)
                HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher)
                HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy)
            }
        } catch (e: Exception) {
            log.error("Failed to register Sleuth Hystrix Concurrency Strategy", e)
        }
    }



    private fun logCurrentStateOfHystrixPlugins(eventNotifier: HystrixEventNotifier, metricsPublisher: HystrixMetricsPublisher, propertiesStrategy: HystrixPropertiesStrategy) {
        if (log.isDebugEnabled) {
            log.debug("Current Hystrix plugins configuration is [" + "concurrencyStrategy ["
                + delegate + "]," + "eventNotifier [" + eventNotifier + "]," + "metricPublisher ["
                + metricsPublisher + "]," + "propertiesStrategy [" + propertiesStrategy + "]," + "]")
            log.debug("Registering Sleuth Hystrix Concurrency Strategy.")
        }
    }

    override fun <T> wrapCallable(callable: Callable<T>): Callable<T> {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return WrappedCallable(callable, requestAttributes)
    }

    override fun getThreadPool(threadPoolKey: HystrixThreadPoolKey, corePoolSize: HystrixProperty<Int>, maximumPoolSize: HystrixProperty<Int>, keepAliveTime: HystrixProperty<Int>, unit: TimeUnit, workQueue: BlockingQueue<Runnable>): ThreadPoolExecutor {
        return delegate.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue)
    }

    override fun getThreadPool(threadPoolKey: HystrixThreadPoolKey, threadPoolProperties: HystrixThreadPoolProperties): ThreadPoolExecutor {
        return delegate.getThreadPool(threadPoolKey, threadPoolProperties)
    }

    override fun getBlockingQueue(maxQueueSize: Int): BlockingQueue<Runnable> {
        return delegate.getBlockingQueue(maxQueueSize)
    }

    override fun <T> getRequestVariable(rv: HystrixRequestVariableLifecycle<T>): HystrixRequestVariable<T> {
        return delegate.getRequestVariable(rv)
    }

    internal class WrappedCallable<T>(private val target: Callable<T>, private val requestAttributes: RequestAttributes) : Callable<T> {
        @Throws(Exception::class)
        override fun call(): T {
            return try {
                RequestContextHolder.setRequestAttributes(requestAttributes)
                target.call()
            } finally {
                RequestContextHolder.resetRequestAttributes()
            }
        }

    }
}
