package com.lorenzolerate.weather.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.lorenzolerate.weather.R
import com.lorenzolerate.weather.entity.WeatherDataCurrent
import com.lorenzolerate.weather.entity.WeatherDataOneCall
import com.lorenzolerate.weather.util.Utils
import com.lorenzolerate.weather.util.VolleySingleton
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainFragment : Fragment() {
    private lateinit var urlCurrent: String
    private lateinit var urlOneCall: String
    private lateinit var urlCityName: String

    // declare a global variable of FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val apiKey = "28ccd530bdf46e5f18a002327a2bdc70"
    private lateinit var weatherDataOneCall: WeatherDataOneCall.WeatherData
    private lateinit var viewCreated: View
    private lateinit var progressDialog: ProgressDialog
    private var lastValidLocation: String = ""
    private lateinit var unitsFromPreferences: String
    private lateinit var mAdView : AdView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //Ads
        MobileAds.initialize(this.requireContext()) {}
        mAdView = activity?.findViewById(R.id.adView)!!
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            requestCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
//                    startApplication()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun askForPermissions() {
        val checkPermission = ContextCompat.checkSelfPermission(
            this.requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        ContextCompat.checkSelfPermission(
            this.requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d("", "Location permission granted")


        } else {
            Log.d("", "Location permission denied")
            Log.d("", "Asking for permissions...")
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }
    }


    private fun startApplication() {
        //TODO refactor this block
        //-----------------------------------------
        while (true) {
            val checkPermission = ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )


            if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                Log.d("", "Location Manifest.permission granted")
                break
            }
        }
        //-----------------------------------------
        unitsFromPreferences =
            PreferenceManager.getDefaultSharedPreferences(this.requireContext())
                .getString(getString(R.string.units_preferences_key), null).toString()
        if (unitsFromPreferences == "null")
            unitsFromPreferences = getString(R.string.celsius_preferences_value)

        lastValidLocation = getSharedPreference(R.string.last_valid_location_key).toString()
        progressDialog = ProgressDialog.show(
            this.requireContext(), "",
            "Loading. Please wait...", true
        )
        // set reload button click listener
        val reloadButton =
            viewCreated.findViewById<AppCompatImageButton>(R.id.reload_view_button)
        reloadButton.setOnClickListener {
            reloadView()
        }
        // Initializes location service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val location = getSharedPreference(R.string.location_from_search_key).toString()
        loadWeather(location)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewCreated = view
        askForPermissions()
        startApplication()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create()?.apply {
            interval = 1000
            fastestInterval = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                val location = locationResult.locations[0]
                // Update UI with location data
                retrieveCurrentData(
                    location.latitude.toString(),
                    location.longitude.toString()
                )
            }
        }
    }

    private fun requestSingleUpdate() {
        // only works with SDK Version 23 or higher
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.requireContext()!!
                    .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED || this.requireContext()!!
                    //TODO add fine location support
                    .checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) !== PackageManager.PERMISSION_GRANTED
            ) {
                // permission is not granted
                Log.e("SiSoLocProvider", "Permission not granted.")
                return
            } else {
                Log.d("SiSoLocProvider", "Permission granted.")
            }
        } else {
            Log.d("SiSoLocProvider", "SDK < 23, checking permissions should not be necessary")
        }
        val startTime = System.currentTimeMillis()
        val fusedTrackerCallback = object : LocationCallback() {
            @SuppressLint("MissingPermission")
            override fun onLocationResult(locationResult: LocationResult) {
                //These lines of code will run on UI thread.
                if (locationResult.lastLocation != null && System.currentTimeMillis() <= startTime + 30 * 1000) {
                    System.out.println("LOCATION: " + locationResult.lastLocation.latitude.toString() + "|" + locationResult.lastLocation.longitude)
                    System.out.println("ACCURACY: " + locationResult.lastLocation.accuracy)
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    println("LastKnownNull? :: " + (locationResult.lastLocation == null))
                    println("Time over? :: " + (System.currentTimeMillis() > startTime + 30 * 1000))
                }

                // After receiving location result, remove the listener.
                fusedLocationClient.removeLocationUpdates(this)

                retrieveCurrentData(
                    locationResult.lastLocation.latitude.toString(),
                    locationResult.lastLocation.longitude.toString()
                )
            }
        }

        val req = LocationRequest()
        req.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        req.fastestInterval = 2000
        req.interval = 2000
        // Receive location result on UI thread.
        fusedLocationClient.requestLocationUpdates(
            req,
            fusedTrackerCallback,
            Looper.getMainLooper()
        )

    }

    private fun loadWeather(location: String) {
        if (location != "null" && location != "") { //if true -> search by city name. Else -> search by current location
            urlCityName =
                "https://api.openweathermap.org/data/2.5/weather?q=" + location + "&appid=" + apiKey
            loadCoordinates(location)

        } else {
            //check permissions
            if (ActivityCompat.checkSelfPermission(
                    this.requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            if (Utils.LocationUtil.isLocationOn(this.requireContext())) {
                fusedLocationClient.lastLocation.addOnSuccessListener { locationGps ->
                    if (locationGps == null) {
                        requestSingleUpdate()
                    } else {
                        retrieveCurrentData(
                            locationGps.latitude.toString(),
                            locationGps.longitude.toString()
                        )
                    }
                }
            } else {//show dialog "location services not enabled"
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder?.setTitle("Location must be turned on to detect current location.")
                        .setMessage("Please turn on location and reload view. Otherwise use search bar")
                    builder.apply {
                        setPositiveButton("OK",
                            DialogInterface.OnClickListener { dialog, id ->
                            })
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                progressDialog.cancel()
                alertDialog?.show()
            }
        }
    }


    private fun loadCoordinates(location: String) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, urlCityName, null,
            Response.Listener { response -> //successful response
                lastValidLocation = location
                setSharedPreference(R.string.last_valid_location_key, location)
                var gson = Gson()
                var weatherDataCurrent: WeatherDataCurrent.WeatherData
                weatherDataCurrent = gson.fromJson(
                    response.toString(),
                    WeatherDataCurrent.WeatherData::class.java
                )
                retrieveCurrentData(
                    weatherDataCurrent.coord.lat.toString(),
                    weatherDataCurrent.coord.lon.toString()
                )
            },
            Response.ErrorListener { error ->
                Log.e("TAG", error.toString())
                if (lastValidLocation != null && lastValidLocation != "") {
                    setSharedPreference(
                        R.string.location_from_search_key,
                        lastValidLocation
                    )
                }

                loadWeather(lastValidLocation)

            })


        VolleySingleton.getInstance(this.requireContext())
            .addToRequestQueue(jsonObjectRequest)

    }


    private fun retrieveCurrentData(latitude: String, longitude: String) {
        //weather info is got from this url
        urlOneCall =
            "https://api.openweathermap.org/data/2.5/onecall?lat=" + latitude + "&lon=" + longitude + "&exclude=minutely&appid=" + apiKey

        //city name is got from this url
        urlCurrent =
            "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, urlOneCall, null,
            Response.Listener { response ->
                var gson = Gson()
                weatherDataOneCall = gson.fromJson(
                    response.toString(),
                    WeatherDataOneCall.WeatherData::class.java
                )
                // set latitude and longitude text with 2 decimals----------------------------------------
                val df = DecimalFormat()
                df.setMaximumFractionDigits(2)
                viewCreated.findViewById<TextView>(R.id.longitude).text =
                    df.format(longitude.toFloat())
                viewCreated.findViewById<TextView>(R.id.latitude).text =
                    df.format(latitude.toFloat())
                //-----------------------------------------------------------------------------------------
                if (unitsFromPreferences == "celsius") {
                    Log.d("", "Preferences: celsius detected")

                    viewCreated.findViewById<TextView>(R.id.temperature).text =
                        getString(
                            R.string.current_temp_celsius,
                            (weatherDataOneCall.current.temp - 273.15).roundToInt()
                        )
                    viewCreated.findViewById<TextView>(R.id.feels_like).text =
                        getString(
                            R.string.current_feels_like_celsius,
                            (weatherDataOneCall.current.feels_like - 273.15).roundToInt()
                        )
                    viewCreated.findViewById<TextView>(R.id.max_temperature).text =
                        getString(
                            R.string.current_temp_max_celsius,
                            (weatherDataOneCall.daily.get(0).temp.max - 273.15).roundToInt()
                        )
                    viewCreated.findViewById<TextView>(R.id.min_temperature).text =
                        getString(
                            R.string.current_temp_min_celsius,
                            (weatherDataOneCall.daily.get(0).temp.min - 273.15).roundToInt()
                        )
                    viewCreated.findViewById<TextView>(R.id.wind_speed).text =
                        getString(
                            R.string.current_wind_speed_metric,
                            weatherDataOneCall.current.wind_speed
                        )
                } else { //imperial units
                    viewCreated.findViewById<TextView>(R.id.temperature).text =
                        getString(
                            R.string.current_temp_fahrenheit,
                            Utils.TemperatureUtil.convertKelvinToFahrenheit(
                                weatherDataOneCall.current.temp
                            )
                        )
                    viewCreated.findViewById<TextView>(R.id.feels_like).text =
                        getString(
                            R.string.current_feels_like_fahrenheit,
                            Utils.TemperatureUtil.convertKelvinToFahrenheit(
                                weatherDataOneCall.current.feels_like
                            )
                        )
                    viewCreated.findViewById<TextView>(R.id.max_temperature).text =
                        getString(
                            R.string.current_temp_max_fahrenheit,
                            Utils.TemperatureUtil.convertKelvinToFahrenheit(
                                weatherDataOneCall.daily.get(
                                    0
                                ).temp.max
                            )
                        )
                    viewCreated.findViewById<TextView>(R.id.min_temperature).text =
                        getString(
                            R.string.current_temp_min_fahrenheit,
                            Utils.TemperatureUtil.convertKelvinToFahrenheit(
                                weatherDataOneCall.daily.get(
                                    0
                                ).temp.min
                            )
                        )
                    viewCreated.findViewById<TextView>(R.id.wind_speed).text =
                        getString(
                            R.string.current_wind_speed_imperial,
                            (weatherDataOneCall.current.wind_speed / 1.609)
                        )
                }

                viewCreated.findViewById<TextView>(R.id.sky).text =
                    weatherDataOneCall.current.weather.get(0).main
                viewCreated.findViewById<TextView>(R.id.humidity).text =
                    getString(
                        R.string.current_humidity,
                        weatherDataOneCall.current.humidity
                    )
                viewCreated.findViewById<TextView>(R.id.pressure).text =
                    getString(
                        R.string.current_pressure,
                        weatherDataOneCall.current.pressure
                    )

                //show local time in chosen city
                val currentCalendar: Calendar =
                    Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val currentDate =
                    Date(Calendar.getInstance().time.time + weatherDataOneCall.timezone_offset * 1000)
                currentCalendar.time = currentDate
                val dateFormatGmt =
                    SimpleDateFormat("HH:mm")
                dateFormatGmt.timeZone = TimeZone.getTimeZone("GMT")
                viewCreated.findViewById<TextView>(R.id.time).text =
                    getString(
                        R.string.current_time,
                        dateFormatGmt.format(currentCalendar.time)
                    )

                //load icon for current weather. If icon is sun or moon select from files
                val icon = weatherDataOneCall.current.weather[0].icon

                if (icon == "01d" || icon == "01n") {
                    if (icon == "01d") {
                        val currentWeatherIconImageView =
                            viewCreated.findViewById(R.id.weather_icon) as ImageView
                        currentWeatherIconImageView.setImageResource(R.drawable.sun)
                    } else {
                        val currentWeatherIconImageView =
                            viewCreated.findViewById(R.id.weather_icon) as ImageView
                        currentWeatherIconImageView.setImageResource(R.drawable.moon)
                    }
                } else {
                    val urlCurretnWeatherIcon =
                        "https://openweathermap.org/img/wn/" + icon + "@4x.png"
                    Picasso.get().load(urlCurretnWeatherIcon)
                        .into(viewCreated.findViewById<ImageView>(R.id.weather_icon));
                }


                //TODO reorganize this code---------------
                retrieveHourlyData()
                retrieveDailyData()

                RetrieveCityNameTask(
                    urlCurrent,
                    viewCreated,
                    progressDialog,
                    this.requireContext()
                ).execute()
                //-----------------------------------------
            },
            Response.ErrorListener { error ->
                Log.e("TAG", error.toString())
            }
        )


        // Access the RequestQueue through your singleton class.
        VolleySingleton.getInstance(this.requireContext())
            .addToRequestQueue(jsonObjectRequest)

    }

    private class RetrieveCityNameTask() : AsyncTask<Object?, Object?, Object?>() {
        private lateinit var url: String
        private lateinit var viewCreated: View
        private lateinit var progressDialog: ProgressDialog
        private lateinit var context: Context

        private lateinit var weatherDataCurrent: WeatherDataCurrent.WeatherData

        constructor(
            url: String,
            viewCreated: View,
            progressDialog: ProgressDialog,
            context: Context
        ) : this() {
            this.url = url
            this.viewCreated = viewCreated
            this.progressDialog = progressDialog
            this.context = context
        }

        override fun doInBackground(vararg p0: Object?): Object? {
            val future = RequestFuture.newFuture<JSONObject>();
            val request: JsonObjectRequest = JsonObjectRequest(
                this.url, JSONObject(), future, future
            )
            VolleySingleton.getInstance(context)
                .addToRequestQueue(request)
            val response = future.get(); // this will block
            var gson = Gson()
            weatherDataCurrent = gson.fromJson(
                response.toString(),
                WeatherDataCurrent.WeatherData::class.java
            )
            return null
        }

        protected override fun onPostExecute(result: Object?) {
            viewCreated.findViewById<TextView>(R.id.city).text =
                weatherDataCurrent.name

            progressDialog.cancel()
        }

    }

    private fun retrieveHourlyData() {
        weatherDataOneCall.hourly.forEach() {
            val hourlyHorizontalScrollViewLinearLayout: LinearLayout =
                viewCreated.findViewById(R.id.hourly_horizontal_scroll_view_linear_layout)
            val hourlyLinearLayout: LinearLayout =
                LinearLayout(this.requireContext())
            hourlyLinearLayout.layoutParams = LinearLayout.LayoutParams(
                resources.getDimension(R.dimen.cardview_linear_layout_width)
                    .toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hourlyLinearLayout.orientation = LinearLayout.VERTICAL

            val hourlyTimeTextView: TextView =
                TextView(this.requireContext())
            val hourlyTimeTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            hourlyTimeTextViewParams.gravity = Gravity.CENTER
            hourlyTimeTextView.layoutParams = hourlyTimeTextViewParams

            val hourlyDateTextView: TextView =
                TextView(this.requireContext())
            val hourlyDateTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            hourlyDateTextViewParams.gravity = Gravity.CENTER
            hourlyDateTextView.layoutParams = hourlyDateTextViewParams

            //load estimated temperatures for hourly weather
            val hourlyTempTextView: TextView =
                TextView(this.requireContext())
            val hourlyTempTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            hourlyTempTextViewParams.gravity = Gravity.CENTER
            hourlyTempTextView.layoutParams = hourlyTempTextViewParams
            if (unitsFromPreferences == "celsius") {
                hourlyTempTextView.text =
                    getString(
                        R.string.temp_celsius,
                        (it.temp - 273.15).roundToInt()
                    )
            } else {
                hourlyTempTextView.text =
                    getString(
                        R.string.temp_fahrenheit,
                        Utils.TemperatureUtil.convertKelvinToFahrenheit(it.temp)
                    )
            }

            //configure hourly icon imageView
            val hourlyIconImageView: ImageView =
                ImageView(this.requireContext())
            val hourlyIconImageViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.cardview_linear_layout_width)
                        .toInt(),
                    resources.getDimension(R.dimen.cardview_linear_layout_height)
                        .toInt()
                )
            hourlyIconImageView.layoutParams = hourlyIconImageViewParams
            //load icon for hourly weather
            val icon = it.weather[0].icon
            if (icon == "01d" || icon == "01n") {
                if (icon == "01d") {//day icon
                    hourlyIconImageView.setImageResource(R.drawable.sun)
                } else {//night icon
                    val currentWeatherIconImageView =
                        hourlyIconImageView.setImageResource(R.drawable.moon)
                }
            } else {
                val hourlyIconUrl =
                    "https://openweathermap.org/img/wn/" + icon + "@4x.png"
                Picasso.get().load(hourlyIconUrl)
                    .into(hourlyIconImageView)
            }
            //load chance of precipitation
            val hourlyPrecipitationTextView: TextView =
                TextView(this.requireContext())
            val hourlyPrecipitationTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            hourlyPrecipitationTextViewParams.gravity = Gravity.CENTER
            hourlyPrecipitationTextView.layoutParams =
                hourlyPrecipitationTextViewParams
            hourlyPrecipitationTextView.text =
                getString(
                    R.string.hourly_precipitation,
                    it.pop * 100
                )

            var weatherHourlyCalendar = Utils.DateUtil.convertTimestampToCityCalendar(
                it.dt,
                weatherDataOneCall.timezone_offset
            )

            hourlyTimeTextView.text =
                getString(
                    R.string.hourly_time,
                    weatherHourlyCalendar.get(Calendar.HOUR_OF_DAY)
                )
            hourlyDateTextView.text =
                getString(
                    R.string.hourly_date,
                    weatherHourlyCalendar.get(Calendar.DAY_OF_MONTH),
                    weatherHourlyCalendar.get(Calendar.MONTH) + 1
                )

            hourlyLinearLayout.addView(hourlyTimeTextView)
            hourlyLinearLayout.addView(hourlyDateTextView)
            hourlyLinearLayout.addView(hourlyTempTextView)
            hourlyLinearLayout.addView(hourlyIconImageView)
            hourlyLinearLayout.addView(hourlyPrecipitationTextView)
            hourlyHorizontalScrollViewLinearLayout.addView(
                hourlyLinearLayout
            )
        }
    }

    private fun retrieveDailyData() {
        Log.d("", "Retrieving daily data")
        weatherDataOneCall.daily.forEach() {
            val dailyHorizontalScrollViewLinearLayout: LinearLayout =
                viewCreated.findViewById(R.id.daily_horizontal_scroll_view_linear_layout)
            val dailyLinearLayout: LinearLayout =
                LinearLayout(this.requireContext())
            dailyLinearLayout.layoutParams = LinearLayout.LayoutParams(
                resources.getDimension(R.dimen.cardview_linear_layout_width)
                    .toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dailyLinearLayout.orientation = LinearLayout.VERTICAL


            val dateTextView: TextView =
                TextView(this.requireContext())
            val dateTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            dateTextViewParams.gravity = Gravity.CENTER
            dateTextView.layoutParams = dateTextViewParams

            //load estimated max and min temperatures for hourly weather
            //max temp
            val maxTempTextView: TextView =
                TextView(this.requireContext())
            val tempTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            tempTextViewParams.gravity = Gravity.CENTER
            maxTempTextView.layoutParams = tempTextViewParams
            //min temp
            val minTempTextView: TextView =
                TextView(this.requireContext())
            minTempTextView.layoutParams = tempTextViewParams
            if (unitsFromPreferences == "celsius") {
                maxTempTextView.text =
                    getString(
                        R.string.temp_celsius,
                        (it.temp.max - 273.15).roundToInt()
                    )
                minTempTextView.text =
                    getString(
                        R.string.temp_celsius,
                        (it.temp.min - 273.15).roundToInt()
                    )
            } else {
                maxTempTextView.text =
                    getString(
                        R.string.temp_fahrenheit,
                        Utils.TemperatureUtil.convertKelvinToFahrenheit(it.temp.max)
                    )
                minTempTextView.text =
                    getString(
                        R.string.temp_fahrenheit,
                        Utils.TemperatureUtil.convertKelvinToFahrenheit(it.temp.min)
                    )
            }

            //configure hourly icon imageView
            val iconHourlyImageView: ImageView =
                ImageView(this.requireContext())
            val iconImageViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    resources.getDimension(R.dimen.cardview_linear_layout_width)
                        .toInt(),
                    resources.getDimension(R.dimen.cardview_linear_layout_height)
                        .toInt()
                )
            iconHourlyImageView.layoutParams = iconImageViewParams
            //load icon for hourly weather
            val icon = it.weather[0].icon
            if (icon == "01d" || icon == "01n") {
                if (icon == "01d") {//day icon
                    iconHourlyImageView.setImageResource(R.drawable.sun)
                } else {//night icon
                    iconHourlyImageView.setImageResource(R.drawable.moon)
                }
            } else {
                val iconUrl =
                    "https://openweathermap.org/img/wn/" + icon + "@4x.png"
                Picasso.get().load(iconUrl)
                    .into(iconHourlyImageView)
            }

            //load chance of precipitation
            val dailyPrecipitationTextView: TextView =
                TextView(this.requireContext())
            val dailyPrecipitationTextViewParams: LinearLayout.LayoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            dailyPrecipitationTextViewParams.gravity = Gravity.CENTER
            dailyPrecipitationTextView.layoutParams =
                dailyPrecipitationTextViewParams
            dailyPrecipitationTextView.text =
                getString(
                    R.string.hourly_precipitation,
                    it.pop * 100
                )

            val dailyCalendar =
                Utils.DateUtil.convertTimestampToCityCalendar(
                    it.dt,
                    weatherDataOneCall.timezone_offset
                )

            dateTextView.text =
                getString(
                    R.string.hourly_date,
                    dailyCalendar.get(Calendar.DAY_OF_MONTH),
                    dailyCalendar.get(Calendar.MONTH) + 1
                )

            dailyLinearLayout.addView(dateTextView)
            dailyLinearLayout.addView(maxTempTextView)
            dailyLinearLayout.addView(minTempTextView)
            dailyLinearLayout.addView(iconHourlyImageView)
            dailyLinearLayout.addView(dailyPrecipitationTextView)
            dailyHorizontalScrollViewLinearLayout.addView(
                dailyLinearLayout
            )
        }
    }

    // Reload current fragment
    private fun reloadView() {
        var frg: Fragment? = this
        val ft: FragmentTransaction? = fragmentManager?.beginTransaction()
        frg?.let { ft?.detach(it) }
        frg?.let { ft?.attach(it) }
        ft?.commit()
    }

    private fun getSharedPreference(preference: Int): String? {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val preferenceValue = sharedPref?.getString(getString(preference), null)
        return preferenceValue
    }

    private fun setSharedPreference(preference: Int, value: String) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        if (sharedPref != null) {
            with(sharedPref.edit()) {
                putString(getString(preference), value)
                commit()
            }
        }
    }
}