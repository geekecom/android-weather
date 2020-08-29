package com.lorenzolerate.weather.util

import android.content.Context
import android.location.LocationManager
import java.util.*
import kotlin.math.roundToInt

class Utils {
    object DateUtil {
        fun convertTimestampToPhoneCalendar(timestamp: Long): Calendar {
            val phoneCalendar: Calendar = Calendar.getInstance()
            phoneCalendar.timeInMillis = timestamp * 1000
            return phoneCalendar
        }

        fun convertTimestampToCityCalendar(timestamp: Long, timezoneOffset: Int): Calendar {
            val calendarCity: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendarCity.timeInMillis = timestamp * 1000
            calendarCity.timeInMillis += timezoneOffset * 1000
            return calendarCity
        }
    }

    object TemperatureUtil {
        fun convertKelvinToFahrenheit(kelvingValue: Float): Int {
            //(0K − 273.15) × 9/5 + 32 = -459.7°F
            return ((kelvingValue - 273.15) * 9 / 5 + 32).roundToInt()
        }
    }

    object LocationUtil {
        fun isLocationOn(context: Context) : Boolean{
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gpsEnabled = false;
            var networkEnabled = false;

            try {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (ex: Exception) {
            }

            try {
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (ex: Exception) {
            }

            return gpsEnabled || networkEnabled
        }
    }
}