package com.lorenzolerate.weather

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        (menu.findItem(R.id.search).actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsActivityIntent = Intent(this, SettingsActivity::class.java).apply {}
                startActivity(settingsActivityIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            var query = intent.getStringExtra(SearchManager.QUERY)
            if (query == null) //if query is null then the search comes
                query = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY)
            //use the query to search your data somehow
            if (query != null) {
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString(getString(R.string.location_from_search_key), query)
                    commit()
                }
            }
        }
    }
}

//Priority
//DONE detect when location is OFF
//DONE set icon
//TODO Adds
//-----------------------------------
//TODO multi-language support
//TODO recent searches
//TODO show search hints
//TODO click daily forecast to see hourly
//DONE Imperial units support
//DONE show latitude, longitude after search
//TODO (bug) loading forever
//DONE (bug) city reset after returning from options
//DONE (bug) city not found
//TODO loading screen
//TODO first use screen
//TODO migrate fragment to mainActivity
//TODO location permissions messages
//TODO review strings file
//TODO allow to use current location
//TODO show search city not found
//TODO 5 start message