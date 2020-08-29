package com.lorenzolerate.weather

import com.lorenzolerate.weather.util.Utils
import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UnitTest {

    @Test
    fun convertTimestampToDate() {
        val date = Utils.DateUtil.convertTimestampToPhoneCalendar(1596711976)
        assertEquals(6, date.get(Calendar.DAY_OF_MONTH))
        assertEquals(8 - 1, date.get(Calendar.MONTH))
        assertEquals(2020, date.get(Calendar.YEAR))
        assertEquals(13, date.get(Calendar.HOUR_OF_DAY))
        assertEquals(6, date.get(Calendar.MINUTE))
        assertEquals(16, date.get(Calendar.SECOND))
    }
}
