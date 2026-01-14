//package com.abhik.urbanmanagementplatform.activities

//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.abhik.urbanmanagementplatform.R
//import com.ola.mapsdk.camera.MapControlSettings
//import com.ola.mapsdk.interfaces.OlaMapCallback
//import com.ola.mapsdk.view.OlaMap
//import com.ola.mapsdk.view.OlaMapView
//
//class MapActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_map)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//
//        val mapView: OlaMapView? = findViewById(R.id.mapView)
//
//        val mapControlSettings: MapControlSettings =
//            MapControlSettings.Builder()
//                .setScrollGesturesEnabled(true)
//                .setZoomGesturesEnabled(true)
//                .build()
//
//        mapView?.getMap(
//            getString(R.string.ola_map_api_key),
//            olaMapCallback = object : OlaMapCallback {
//                override fun onMapReady(olaMap: OlaMap) {
//                    // Map is ready to use
//                }
//
//                override fun onMapError(error: String) {
//                    // Handle map error
//                }
//            },
//
//            mapControlSettings = mapControlSettings
//        )
//    }
//}

package com.abhik.urbanmanagementplatform.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.abhik.urbanmanagementplatform.R
import com.ola.mapsdk.camera.MapControlSettings
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.listeners.OlaMapsListenerManager
import com.ola.mapsdk.model.OlaLatLng
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView

class MapActivity : AppCompatActivity() { // Class name changed to standard Kotlin PascalCase

    private var olaMap: OlaMap? = null // Use nullable type for lateinit safety
    private lateinit var currLocation: ImageButton // Use lateinit for non-null initialization in onCreate
    private var userName: String? = null // Store the username received from intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapView: OlaMapView = findViewById(R.id.mapView)
        currLocation = findViewById(R.id.btnChooseLocation)

        // Get username from intent using safe access
        userName = intent.getStringExtra("USER_NAME")

        // Build Map Control Settings
        val mapControlSettings = MapControlSettings.Builder()
            .setScrollGesturesEnabled(true)
            .setZoomGesturesEnabled(true)
            .build()

        // Initialize the Map asynchronously
        mapView.getMap(
            getString(R.string.ola_map_api_key),
            object : OlaMapCallback {

                override fun onMapReady(map: OlaMap) {
                    olaMap = map

                    // Set click listener for 'Current Location' button
                    currLocation.setOnClickListener {
                        map.showCurrentLocation()
                        val currentLocation: OlaLatLng? = map.getCurrentLocation()

                        if (currentLocation != null) {
                            // Move camera to current location
                            map.moveCameraToLatLong(currentLocation, 15.0, 2000)
                        } else {
                            Toast.makeText(this@MapActivity, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Set click listener for map interaction (selecting a location)
                    map.setOnMapClickedListener(object : OlaMapsListenerManager.OnOlaMapClickedListener {
                        override fun onOlaMapClicked(latLng: OlaLatLng) {
                            val resultIntent = Intent()
                            resultIntent.putExtra("latitude", latLng.latitude)
                            resultIntent.putExtra("longitude", latLng.longitude)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    })
                }

                override fun onMapError(error: String) {
                    Toast.makeText(this@MapActivity, "Map Error: $error", Toast.LENGTH_SHORT).show()
                }
            },
            mapControlSettings
        )
    }
}