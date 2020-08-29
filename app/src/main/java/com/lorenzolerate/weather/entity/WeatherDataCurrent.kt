package com.lorenzolerate.weather.entity

class WeatherDataCurrent {
    data class WeatherData(
        val name: String, //city name
        val coord: Coord
    )

    data class Coord(
        val lat: Float,
        val lon: Float
    )
}