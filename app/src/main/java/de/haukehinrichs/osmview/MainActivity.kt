package de.haukehinrichs.osmview

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider

import java.net.MalformedURLException
import java.net.URL
import java.util.Random

class MainActivity : FragmentActivity(), OnMapReadyCallback {
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null
    private var mMap: GoogleMap? = null
    private var mMapFragment: MapFragment? = null
    private var mModes: Array<String>? = null
    private var mOverlay: TileOverlay? = null
    private var mMode = 0
    private val mUrls = arrayOf("http://%s.tile.openstreetmap.org/%d/%d/%d.png", "https://%s.tile.thunderforest.com/cycle/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/transport/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/landscape/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/outdoors/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/pioneer/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/mobile-atlas/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2", "https://%s.tile.thunderforest.com/neighbourhood/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2")
    private val mServers = arrayOf("a", "b", "c")
    private var mFab: View? = null
    private var trackLocation = false
    private var mLastLocation: Location? = null
    private var mMarker: Marker? = null
    private var mSearchMarker: Marker? = null
    private var mSearchView: SearchView? = null
    private var inputListener: SearchInputListener? = null
    private var mLocationListener : LocationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer)
        mMapFragment = MapFragment.newInstance()
        mMapFragment!!.getMapAsync(this)
        fragmentManager.beginTransaction().add(R.id.content_frame, mMapFragment).commit()
        mModes = resources.getStringArray(R.array.map_modes_array)
        mDrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        mDrawerList = findViewById(R.id.left_drawer) as ListView

        // Set the adapter for the list view
        mDrawerList!!.adapter = ArrayAdapter(this,
                R.layout.drawer_list_item, mModes!!)
        // Set the list's click listener
        mDrawerList!!.setOnItemClickListener(DrawerItemClickListener())
        mFab = findViewById(R.id.track_location)
        mFab!!.setOnClickListener { v ->
            trackLocation = true
            v.visibility = View.GONE
            updateLocation(mLastLocation)
        }
        mSearchView = findViewById(R.id.search_input) as SearchView
        val suggestionView = findViewById(R.id.search_suggestions) as ListView
        inputListener = SearchInputListener(getString(R.string.google_locations_key), suggestionView, this)
        mSearchView!!.setOnSuggestionListener(inputListener)
        mSearchView!!.setOnQueryTextListener(inputListener)
    }

    override fun onStart() {
        super.onStart()
        initLocationTracker()
        Log.d("location", "started listening")
    }

    override fun onStop() {
        super.onStop()
        // Acquire a reference to the system Location Manager
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(mLocationListener)
        Log.d("location", "stopped listening")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

    }

    protected fun initLocationTracker() {
        // Acquire a reference to the system Location Manager
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (mLocationListener == null) {
            // Define a listener that responds to location updates
            mLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d("location", "location update")
                    mLastLocation = location
                    updateLocation(location)
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}
            }
        }

        // Register the listener with the Location Manager to receive location updates

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_LOCATION)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 1f, mLocationListener)
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    private fun updateLocation(location: Location?) {
        if (location == null) {
            return;
        }
        val latLng = LatLng(location.latitude, location.longitude)
        inputListener!!.updateLocation(latLng)
        // Called when a new location is found by the network location provider.
        if (mMarker != null) {
            mMarker!!.remove()
        }
        val icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_circle)
        mMarker = mMap!!.addMarker(MarkerOptions().position(latLng).title(getString(R.string.you_are_here)).icon(icon).anchor(.5f, .5f))
        if (trackLocation) {
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f), 1500, null)
        }
    }

    fun addMarker(latLng: LatLng, title: String) {
        if (mSearchMarker != null) {
            mSearchMarker!!.remove()
        }
        mSearchView!!.onActionViewCollapsed()
        stopTrackingLocation()
        mSearchMarker = mMap!!.addMarker(MarkerOptions().position(latLng).title(title))
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f), 1500, null)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mOverlay = mMap!!.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
        // hide actual maps so labels don't overlap
        mMap!!.mapType = GoogleMap.MAP_TYPE_NONE
        mMap!!.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && trackLocation) {
                stopTrackingLocation()
            }
        }
    }

    private fun stopTrackingLocation() {
        trackLocation = false
        mFab!!.visibility = View.VISIBLE
    }

    private /* Define the URL pattern for the tile images *//*
             * Check that the tile server supports the requested x, y and zoom.
             * Complete this stub according to the tile range you support.
             * If you support a limited range of tiles at different zoom levels, then you
             * need to define the supported x, y range at each zoom level.
             */ val tileProvider: UrlTileProvider
        get() = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
                val s = String.format(mUrls[mMode], mServers[Random().nextInt(3)], zoom, x, y)
                Log.d("tile", "choose url " + s);
                if (!checkTileExists(x, y, zoom)) {
                    return null
                }

                try {
                    return URL(s)
                } catch (e: MalformedURLException) {
                    throw AssertionError(e)
                }

            }

            private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
                val minZoom = 1
                val maxZoom = 19

                if (zoom < minZoom || zoom > maxZoom) {
                    return false
                }

                return true
            }
        }

    private inner class DrawerItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            //Log.d("style", String.format("changed map style: %d", position));
            mMode = position
            mOverlay!!.clearTileCache()
            mDrawerLayout!!.closeDrawers()
        }
    }

    companion object {

        private val PERMISSION_LOCATION = 935
    }
}
