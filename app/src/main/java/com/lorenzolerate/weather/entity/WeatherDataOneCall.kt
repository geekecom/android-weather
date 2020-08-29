package com.lorenzolerate.weather.entity

class WeatherDataOneCall {
    data class WeatherData(
        val lon: Float,
        val lat: Float,
        val timezone: String,
        val timezone_offset: Int,
        val current: Current,
        var daily: Array<Daily>,
        var hourly: Array<Hourly>
    )

    data class Current(
        val dt: Long,
        val sunrise: String,
        val sunset: String,
        val temp: Float,
        val feels_like: Float,
        val pressure: Int,
        val humidity: Int,
        val dew_point: Float,
        val uvi: Float,
        val clouds: Int,
        val visibility: Int,
        val wind_speed: Float,
        val wind_deg: Int,
        val weather: Array<Weather>
    )

    data class Weather(
        val id: Int,
        val main: String,
        val description: String,
        val icon: String
    )

    data class Hourly(
        val dt: Long,
        val temp: Float,
        val feels_like: Float,
        val pressure: Int,
        val humidity: Int,
        val dew_point: Float,
        val clouds: Int,
        val visibility: Int,
        val wind_speed: Float,
        val wind_deg: Int,
        val weather: Array<Weather>,
        val pop: Double
    )

    data class Daily(
        val dt: Long,
        val sunrise: String,
        val sunset: String,
        val temp: Temp,
        val feels_like: FeelsLike,
        val pressure: Int,
        val humidity: Int,
        val dew_point: Float,
        val wind_speed: Float,
        val wind_deg: Int,
        val weather: Array<Weather>,
        val clouds: Int,
        val pop: Double,
        val uvi: Float
    )

    data class Temp(
        val day: Float,
        val min: Float,
        val max: Float,
        val night: Float,
        val eve: Float,
        val morn: Float
    )

    data class FeelsLike(
        val day: Float,
        val night: Float,
        val eve: Float,
        val morn: Float
    )
}