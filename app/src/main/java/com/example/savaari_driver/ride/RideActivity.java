package com.example.savaari_driver.ride;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.savaari_driver.R;
import com.example.savaari_driver.UserLocation;
import com.example.savaari_driver.Util;
import com.example.savaari_driver.services.location.LocationUpdateUtil;
import com.example.savaari_driver.services.network.NetworkServiceUtil;
import com.example.savaari_driver.settings.SettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RideActivity extends Util implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener,
        GoogleMap.OnPolylineClickListener, GoogleMap.OnInfoWindowClickListener, RideActionResponseListener {

    private RideViewModel rideViewModel = null;

    private static final String TAG = "RideActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private Boolean locationPermissionGranted = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int ERROR_DIALOG_REQUEST = 9001;

    private static final float DEFAULT_ZOOM = 15;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private GeoApiContext geoApiContext = null;

    private ImageView centerGPSButton;

    /* Drawing the route on Maps*/
    private Polyline destinationPolyline = null;
    private Marker destinationMarker = null;

    private Marker pickupMarker = null;
    private Polyline pickupPolyline = null;


    private DrawerLayout drawer;
    private ImageButton menuButton;
    private NavigationView navigationView;
    private View headerView;
    private TextView navUsername, navEmail;

    private int USER_ID = -1;
    private ArrayList<UserLocation> mUserLocations;
    private Location mUserLocation;

    private Button searchRideButton;
    private UserLocation driverLocation = new UserLocation();


    // Main onCreate Function to override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeSelect(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride);

        /* Register Broadcast Receiver for NetworkService */
        registerRideReceiver();

        Intent recvIntent = getIntent();
        USER_ID = recvIntent.getIntExtra("USER_ID", -1);

        mUserLocations = new ArrayList<>();

        if (USER_ID == -1) {
            SharedPreferences sh
                    = getSharedPreferences("AuthSharedPref",
                    MODE_PRIVATE);

            USER_ID = sh.getInt("USER_ID", -1);
        }

        if (USER_ID == -1) {
            Toast.makeText(RideActivity.this, "Sorry. We can not authenticate you", Toast.LENGTH_LONG).show();
        }
        else {
            centerGPSButton = findViewById(R.id.user_location);
            rideViewModel = new ViewModelProvider(this, new RideViewModelFactory(USER_ID)).get(RideViewModel.class);
            getLocationPermission();
        }
    }


    /*
     * initMap() if permissions granted
     * else, explicitly ask for permission
     * */
    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initMap();
                return;
            }
        }

        ActivityCompat.requestPermissions(this, permissions,
                LOCATION_PERMISSION_REQUEST_CODE); //Doesn't matter
    }

    /* Callback for when permissions have been granted/denied */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;

        switch(requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int permissionIndex = 0 ; permissionIndex < grantResults.length ; ++permissionIndex) {
                        if (grantResults[permissionIndex] != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = false;
                            return;
                        }
                    }
                    locationPermissionGranted = true;
                    initMap();
                }
            }
        }
    }


    /*
     * Prerequisite: Map permissions granted
     * Initializes map fragment
     * */
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(RideActivity.this);

        if (geoApiContext == null) {
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.directions_api_key))
                    .build();
        }
    }

    /*
     * Callback from initMap()'s getMapAsync()
     * Initialize GoogleMap Object
     * */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(RideActivity.this, "Map is ready", Toast.LENGTH_SHORT).show();
        this.googleMap = googleMap;
        googleMap.setOnPolylineClickListener(this);

        if (locationPermissionGranted) {

            // Calling the Get Device Location to retrieve the location
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissi ons
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setOnInfoWindowClickListener(this);

            init();
        }
    }

    /*
     * Moves camera to param: (latLng, zoom)
     * Adds marker if title specified
     * */
    private void moveCamera(LatLng latLng, float zoom, String title) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    /* Method for adding a destination */
    private void setDestination(LatLng latLng, String title) {
        moveCamera(latLng, DEFAULT_ZOOM, title);

        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title);
        destinationMarker = googleMap.addMarker(options);

        calculateDirections(new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude()), destinationMarker, true);
    }

    /*
     * Initialize Autocomplete Support Fragment
     * onPlaceSelected() implementation
     */
    private void initializeAutocomplete() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_api_key), Locale.US);
        }

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        assert autocompleteFragment != null;
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.ID, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                String title = ((place.getName() == null)?
                        ((place.getAddress() == null)?  "" : place.getAddress()) : place.getName());

                setDestination(Objects.requireNonNull(place.getLatLng()), title);

                Log.d("onPlaceSelected: ", "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Toast.makeText(RideActivity.this, "Could not navigate to selected place", Toast.LENGTH_SHORT).show();
                Log.d("init(): ", "onPlaceSelectedListener(): An error occurred: " + status);
            }
        });
    }

    private void initializeNavigationBar() {
        drawer = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        headerView = navigationView.getHeaderView(0);
        navUsername = headerView.findViewById(R.id.header_nickname);
        navEmail = headerView.findViewById(R.id.header_email);
        menuButton = findViewById(R.id.menu_btn);

        menuButton.setOnClickListener(v -> {
            if (!drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.openDrawer(GravityCompat.START);
            }
        });
    }


    /*
     * Initializes View Objects including:
     * centerGPSButton
     * autocompleteFragment
     */
    private void init() {
        Log.d(TAG, "init: initializing");


        initializeNavigationBar();
        loadUserData();
        loadUserLocations();

        initializeAutocomplete();

        searchRideButton = findViewById(R.id.go_btn);
        searchRideButton.setEnabled(false);

        searchRideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

                LocationUpdateUtil.stopLocationService(RideActivity.this);

                /*
                new LoadDataTask(null, object ->
                {
                    try {
                        JSONObject results = (JSONObject) object;
                        driverLocation.setUserID(results.getInt("USER_ID"));
                        driverLocation.setLatitude(Double.parseDouble(results.getString("LATITUDE")));
                        driverLocation.setLongitude(Double.parseDouble(results.getString("LONGITUDE")));
                        ;

                        MarkerOptions options = new MarkerOptions()
                                .position(new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude()))
                                .title("Pickup point");
                        pickupMarker = googleMap.addMarker(options);

                        calculateDirections(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude()),
                                pickupMarker, false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        Log.d("searchRideButton: ", "OnClick: error");
                    }
                }).execute("findDriver", String.valueOf(USER_ID), String.valueOf(mUserLocation.getLatitude()),
                        String.valueOf(mUserLocation.getLongitude()));*/
            }
        });

        centerGPSButton.setOnClickListener(v -> getDeviceLocation()); //moveCamera to user location


    }

    /* Loads user data from database */
    private void loadUserData() {
        NetworkServiceUtil.loadUserData(RideActivity.this, USER_ID);

        rideViewModel.isLiveUserDataLoaded().observe(this, aBoolean -> {

            if (aBoolean) {
                navUsername.setText(rideViewModel.getUsername());
                navEmail.setText(rideViewModel.getEmailAddress());
                Toast.makeText(RideActivity.this, "User data loaded!", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(RideActivity.this, "Data could not be loaded", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /* Function for loading User Location Data */
    private void loadUserLocations()
    {
        NetworkServiceUtil.getUserLocations(RideActivity.this);
        rideViewModel.isLiveUserLocationsLoaded().observe(this, aBoolean -> {

            if (aBoolean)
            {
                mUserLocations = rideViewModel.getUserLocations();
                Log.d(TAG, "loadUserLocations: Started!");

                // Testing Code
                Log.d(TAG, "loadUserLocations: mUserLocations.size(): " + mUserLocations.size());
                for (int i = 0; i < mUserLocations.size(); ++i) {
                    Log.d(TAG, "loadUserLocations: setting Markers");
                    MarkerOptions option = new MarkerOptions()
                            .position(new LatLng(mUserLocations.get(i).getLatitude(), mUserLocations.get(i).getLongitude()));
                    googleMap.addMarker(option);
                }
                Toast.makeText(RideActivity.this, "User locations loaded!", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(RideActivity.this, "User locations could not be loaded", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // End of Function: loadUserLocations()


    /*
     * Receives autocompleteFragment's result (callback)
     * Gets Place Object using 'getPlaceFromIntent()'
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {

            if (resultCode == AutocompleteActivity.RESULT_OK) {
                assert data != null;
                Place place = Autocomplete.getPlaceFromIntent(data);
                String title = ((place.getName() == null) ?
                        ((place.getAddress() == null) ? "" : place.getAddress()) : place.getName());

                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId() + ", lat: " + place.getLatLng().latitude
                        + ", lon: " + place.getLatLng().longitude);
                moveCamera(Objects.requireNonNull(place.getLatLng()), DEFAULT_ZOOM, title);
            }
            else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            }
            // The user canceled the operation.
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /* Get's device location, calls moveCamera()*/
    private void getDeviceLocation() {
        Log.d("getDeviceLocation", "getting device location");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (locationPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: found location!");
                        Location currentLocation = (Location) task.getResult();

                        moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "");

                        // Calling User Location Save Function
                        try
                        {
                            mUserLocation = currentLocation;
                            rideViewModel.setUserCoordinates(mUserLocation.getLatitude(), mUserLocation.getLongitude());
                            LocationUpdateUtil.saveUserLocation(mUserLocation, RideActivity.this);

                            // Starting Background Location Service
                            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                            LocationUpdateUtil.startLocationService(manager, RideActivity.this);

                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    } else {
                        Log.d(TAG, "onComplete: current location is null");
                        Toast.makeText(RideActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    /*
     * Calculates directions from userLocation to marker
     */
    private void calculateDirections(LatLng mUserLocation, Marker marker, boolean sourceToDest){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.alternatives(false);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.latitude,
                        mUserLocation.longitude
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                addPolylinesToMap(result, sourceToDest);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

            }
        });
    }

    /* Given a list of 'checkpoints, this zooms in on the route '*/
    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (googleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    private void addPolylinesToMap(final DirectionsResult result, boolean sourceToDest){

        /*
         * Posting to main thread
         * since this method is called from a different context
         * changes to google map must be made on the same thread as the one it is on
         */
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d(TAG, "run: result routes: " + result.routes.length);

            /* Loops through possible routes*/
            //for(DirectionsRoute route: result.routes){
            DirectionsRoute route = result.routes[0];
            Log.d(TAG, "run: leg: " + route.legs[0].toString());

            /* get list of LatLng corresponding to each 'checkpoint' along the route */
            List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

            List<LatLng> newDecodedPath = new ArrayList<>();

            // This loops through all the LatLng coordinates of ONE polyline.
            for(com.google.maps.model.LatLng latLng: decodedPath){

                newDecodedPath.add(new LatLng(
                        latLng.lat,
                        latLng.lng
                ));
            }

            /* Add all the 'checkpoints' to the polyline */
            if (sourceToDest) {
                if (destinationPolyline != null) {
                    destinationPolyline.remove();

                    if (pickupPolyline != null) {
                        pickupPolyline.remove();
                    }
                }

                destinationPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                destinationPolyline.setColor(ContextCompat.getColor(RideActivity.this, R.color.maps_blue));
                destinationMarker.setSnippet("Duration: " + route.legs[0].duration);
                destinationMarker.showInfoWindow();
            }
            else {
                if (pickupPolyline != null) {
                    destinationPolyline.remove();
                }
                pickupPolyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                pickupPolyline.setColor(ContextCompat.getColor(RideActivity.this, R.color.success_green));
                pickupMarker.setSnippet("Duration: " + route.legs[0].duration);
                pickupMarker.showInfoWindow();
            }

            zoomRoute(newDecodedPath);

            searchRideButton.setEnabled(true);

            //}
        });
    }

    /* listener for polyline clicks */
    @Override
    public void onPolylineClick(Polyline polyline) {
        //TODO: Highlight more specific details (maybe?)
        //polyline.setColor(ContextCompat.getColor(RideActivity.this, R.color.maps_blue));
        //polyline.setZIndex(1);

    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(RideActivity.this);
        builder.setMessage("Open Google Maps?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        String latitude = String.valueOf(marker.getPosition().latitude);
                        String longitude = String.valueOf(marker.getPosition().longitude);
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");

                        try{
                            if (mapIntent.resolveActivity(RideActivity.this.getPackageManager()) != null) {
                                startActivity(mapIntent);
                            }
                        }catch (NullPointerException e){
                            Log.e(TAG, "onClick: NullPointerException: Couldn't open map." + e.getMessage() );
                            Toast.makeText(RideActivity.this, "Couldn't open map", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }


    /*
     * Checks if device's Google Play Services are available
     * TODO: call this before getLocationPermission() in onCreate()
     *  */
    public boolean isServicesOK() {
        Log.d("isServicesOK: ", "checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(RideActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d("PLAY SERVICES: ", "WORKING");
            return true;
        }
        else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d("PLAY SERVICES", "ERROR, BUT FIXABLE");

            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(RideActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
            return true;
        }
        else {
            Toast.makeText(this, "Error. Map services unavailable", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /* ----------- NETWORKSERVICE & BROADCAST RECEIVER FOR NETWORK OPERATIONS ----*/

    /* Receives response from NetworkService methods */
    private static class RideReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String task = intent.getExtras().getString("TASK");
            RideActionResponseListener rideActionResponseListener;
            rideActionResponseListener = (RideActionResponseListener) context;

            // Calling Appropriate Listener Function based on the Result received.
            if (task.equals("loadData")) {
                rideActionResponseListener.onDataLoaded(intent);
            }
            else if (task.equals("getUserLocations"))
            {
                rideActionResponseListener.onLocationsLoaded(intent);
            }
        }
    }

    RideReceiver rideReceiver;

    /* Register receiver */
    public void registerRideReceiver() {
        rideReceiver = new RideReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("RESULT");

        registerReceiver(rideReceiver, intentFilter);
    }

    @Override
    public void onDataLoaded(Intent intent) {
        String resultString = intent.getExtras().getString("RESULT");
        rideViewModel.onUserDataLoaded(resultString);
    }

    @Override
    public void onLocationsLoaded(Intent intent)
    {
        String result = intent.getExtras().getString("RESULT");
        rideViewModel.onUserLocationsLoaded(result);
    }

    /*------------ END OF NETWORK SERVICE & BROADCAST RECEIVER SECTION ----------*/

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        drawer.closeDrawer(GravityCompat.START);

        switch (item.getItemId()) {
            case (R.id.nav_your_trips):
            case (R.id.nav_help):
            case (R.id.nav_wallet):
                break;
            case (R.id.nav_settings):
                Intent i = new Intent(RideActivity.this, SettingsActivity.class);
                startActivity(i);
                finish();
                break;
        }
        return true;
    }
}