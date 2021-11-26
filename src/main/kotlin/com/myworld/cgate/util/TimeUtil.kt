package com.myworld.cgate.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {

    val yearNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 4)
        }

    val yearMonthNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 6)
        }

    val yearMonthDayNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 0, 8)
        }

    @JvmStatic
    fun getCustomDateFromTimestamp(timestamp: Timestamp?, start: Int?, end: Int?): Int {
        val timeNow = SimpleDateFormat("yyyyMMddHHmmss").format(timestamp)
        return timeNow.substring(start!!, end!!).toInt()
    }

    val monthNow: Int
        get() {
            val timestamp = Timestamp(Date().time)
            return getCustomDateFromTimestamp(timestamp, 4, 6)
        }

    val lastMonth: Int
        get() {
            val monthNow = monthNow
            val lastMonth: Int
            lastMonth = if (monthNow == 1) {
                12
            } else {
                monthNow - 1
            }
            return lastMonth
        }

    @JvmStatic
    fun getPreviousTwelveNumber(number: Int): Int {
        val num: Int
        num = when (number) {
            1 -> 12
            2 -> 1
            3 -> 2
            4 -> 3
            5 -> 4
            6 -> 5
            7 -> 6
            8 -> 7
            9 -> 8
            10 -> 9
            11 -> 10
            12 -> 11
            else -> 0
        }
        return num
    }
}
