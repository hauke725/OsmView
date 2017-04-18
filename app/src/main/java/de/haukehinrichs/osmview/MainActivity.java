package de.haukehinrichs.osmview;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int PERMISSION_LOCATION = 935;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private GoogleMap mMap;
    private MapFragment mMapFragment;
    private String[] mModes;
    private TileOverlay mOverlay;
    private int mMode = 0;
    private String[] mUrls = {
            "http://%s.tile.openstreetmap.org/%d/%d/%d.png",
            "https://%s.tile.thunderforest.com/cycle/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/transport/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/landscape/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/outdoors/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/pioneer/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/mobile-atlas/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2",
            "https://%s.tile.thunderforest.com/neighbourhood/%d/%d/%d.png?apikey=618a5839a59a4ef4adbea93e1f3a49e2"
    };
    private final String[] mServers = {"a", "b", "c"};
    private View mFab;
    private boolean trackLocation = false;
    private Location mLastLocation;
    private Marker mMarker;
    private Marker mSearchMarker;
    private SearchView mSearchView;
    private SearchInputListener inputListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);
        mMapFragment = MapFragment.newInstance();
        mMapFragment.getMapAsync(this);
        getFragmentManager().beginTransaction().add(R.id.content_frame, mMapFragment).commit();
        mModes = getResources().getStringArray(R.array.map_modes_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mModes));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mFab = findViewById(R.id.track_location);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackLocation = true;
                v.setVisibility(View.GONE);
                if (mLastLocation != null) {
                    updateLocation(mLastLocation);
                }
            }
        });
        mSearchView = (SearchView) findViewById(R.id.search_input);
        ListView suggestionView = (ListView) findViewById(R.id.search_suggestions);
        inputListener = new SearchInputListener(getString(R.string.google_locations_key), suggestionView, this);
        mSearchView.setOnSuggestionListener(inputListener);
        mSearchView.setOnQueryTextListener(inputListener);
        initLocationTracker();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    protected void initLocationTracker() {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

// Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.d("location", "location update");
                mLastLocation = location;
                updateLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

// Register the listener with the Location Manager to receive location updates

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 1, locationListener);
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private void updateLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        inputListener.updateLocation(latLng);
        // Called when a new location is found by the network location provider.
        if (mMarker != null) {
            mMarker.remove();
        }
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_circle);
        mMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.you_are_here)).icon(icon).anchor(.5f, .5f));
        if (trackLocation) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16), 1500, null);
        }
    }

    public void addMarker(LatLng latLng, String title) {
        if (mSearchMarker != null) {
            mSearchMarker.remove();
        }
        mSearchView.onActionViewCollapsed();
        stopTrackingLocation();
        mSearchMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16), 1500, null);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(getTileProvider()));
        // hide actual maps so labels don't overlap
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == REASON_GESTURE && trackLocation) {
                    stopTrackingLocation();
                }
            }
        });
    }

    private void stopTrackingLocation() {
        trackLocation = false;
        mFab.setVisibility(View.VISIBLE);
    }

    private UrlTileProvider getTileProvider() {
        return new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {

                /* Define the URL pattern for the tile images */
                String s = String.format(mUrls[mMode], mServers[new Random().nextInt(3)],zoom, x, y);

                if (!checkTileExists(x, y, zoom)) {
                    return null;
                }

                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
            }

            /*
             * Check that the tile server supports the requested x, y and zoom.
             * Complete this stub according to the tile range you support.
             * If you support a limited range of tiles at different zoom levels, then you
             * need to define the supported x, y range at each zoom level.
             */
            private boolean checkTileExists(int x, int y, int zoom) {
                int minZoom = 1;
                int maxZoom = 19;

                if ((zoom < minZoom || zoom > maxZoom)) {
                    return false;
                }

                return true;
            }
        };
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //Log.d("style", String.format("changed map style: %d", position));
            mMode = position;
            mOverlay.clearTileCache();
            mDrawerLayout.closeDrawers();
        }
    }
}
