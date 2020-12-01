package com.example.savaari_driver.ride;

import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.savaari_driver.Repository;
import com.example.savaari_driver.Ride;
import com.example.savaari_driver.UserLocation;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class RideViewModel extends ViewModel {

    private static String LOG_TAG = RideViewModel.class.getSimpleName();

    /* CCredentials for network operations */
    private int USER_ID = -1;
    private int ACTIVE_STATUS = 0;
    private final Repository repository;

    /* User account data*/
    private String username;
    private String emailAddress;
    private LatLng userCoordinates = new LatLng(0,0);

    /* Ride Data */
    private final MutableLiveData<Integer> IS_TAKING_RIDE = new MutableLiveData<Integer>(0);
    private Ride ride;

    /* User locations data for pinging */
    private final ArrayList<UserLocation> mUserLocations = new ArrayList<>();

    /* Data Loaded status flags */
    private final MutableLiveData<Boolean> userDataLoaded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> userLocationsLoaded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> markedActive = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> rideFound = new MutableLiveData<>(false);

    public RideViewModel(int USER_ID, Repository repository)
    {
        this.USER_ID = USER_ID;
        this.repository = repository;
    }

    /* Get user data */
    public String getUsername() {
        return username;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public LatLng getUserCoordinates() {
        return userCoordinates;
    }
    public ArrayList<UserLocation> getUserLocations() {
        return mUserLocations;
    }

    /* Set USER_ID */
    //public void setUserID(int USER_ID) { this.USER_ID = USER_ID; }

    /* Return LiveData to observe Data Loaded Flags */
    public LiveData<Boolean> isLiveUserDataLoaded() {
        return userDataLoaded;
    }
    public LiveData<Boolean> isLiveUserLocationsLoaded() { return userLocationsLoaded; }
    public LiveData<Boolean> isMarkedActive() {
        return markedActive;
    }
    public LiveData<Boolean> isRideFound() { return rideFound; }

    /* Need a setter since coordinates are received from activity */
    public void setUserCoordinates(double latitude, double longitude) {
        userCoordinates = new LatLng(latitude, longitude);
    }

    // Function using the repository
    public void loadUserData()
    {
        repository.loadUserData(this::onUserDataLoaded, USER_ID);
    }
    public void loadUserLocations()
    {
        repository.getUserLocations(this::onUserLocationsLoaded);
    }
    public void setMarkActive()
    {
        repository.setMarkActive(object -> {
            try
            {
                if (object != null)
                {
                    markedActive.setValue(true);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }, USER_ID, ACTIVE_STATUS);
    }
    public void checkRideStatus()
    {
        repository.checkRideStatus(object -> {
            try
            {
                JSONObject ride = (JSONObject) object;
                int status = ride.getInt("STATUS");
                if (status == 200)
                {
                    Log.d(TAG, "checkRideStatus(): Ride Found!");
                    Log.d(TAG, "checkRideStatus(): " + ride.toString());
                    setRideData(ride);
                    rideFound.setValue(true);
                }
                else
                {
                    // Calling the Function Again
                    checkRideStatus();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.d(TAG, "onCheckRideStatus(): Exception thrown!");
            }
        }, USER_ID);
    }
    public void confirmRideRequest()
    {
        repository.confirmRideRequest(object -> {
            // TODO Complete what happens after you confirm ride request
        },USER_ID, IS_TAKING_RIDE.getValue(), ride.getRiderID());
    }

    // Function on User Data Loaded
    public void onUserDataLoaded(Object r) {
        try {
            JSONObject result = (JSONObject) r;
            if (result == null) {
                Log.d(LOG_TAG, "onDataLoaded(): resultString is null");
                userDataLoaded.setValue(false);
            }
            else {
                username = result.getString("USER_NAME");
                emailAddress = result.getString("EMAIL_ADDRESS");
                Log.d("loadUserData(): ", username + ", " + emailAddress);
                userDataLoaded.setValue(true);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            userDataLoaded.setValue(false);
            Log.d(LOG_TAG, "onDataLoaded(): exception thrown");
        }
    }

    // Function on User Locations Loaded
    public void onUserLocationsLoaded(Object r)
    {
        try {
            JSONArray resultArray = (JSONArray) r;
            if (resultArray == null)
            {
                Log.d(TAG, "onUserLocationsLoaded(): resultString is null");
                userLocationsLoaded.setValue(false);
            }
            else {
                Log.d(TAG, "loadUserLocations: " + resultArray.toString());
                Log.d(TAG, "loadUserLocations: found JSON Array");

                // Appending the User Locations in Array
                for (int i = 0; i < resultArray.length(); i++) {
                    JSONObject obj = resultArray.getJSONObject(i);
                    UserLocation userLocation = new UserLocation();

                    // Assigning User Objects
                    userLocation.setUserID(obj.getInt("USER_ID"));
                    userLocation.setLatitude(obj.getDouble("LATITUDE"));
                    userLocation.setLongitude(obj.getDouble("LONGITUDE"));
                    userLocation.setTimestamp(obj.getString("TIMESTAMP"));

                    // Adding Final Object
                    mUserLocations.add(userLocation);
                }
                userLocationsLoaded.setValue(true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d(TAG, "onUserLocationsLoaded(): Exception thrown!");
            userLocationsLoaded.setValue(false);
        }
    }
    // End of Function: onUserLocationsLoaded();

    // Function for Storing Ride Data
    public void setRideData(JSONObject jsonObject)
    {
        ride = new Ride();
        try
        {
            ride.setRiderID(jsonObject.getInt("RIDER_ID"));
            ride.setUserName(jsonObject.getString("USER_NAME"));

            double sourceLat = jsonObject.getDouble("SOURCE_LAT");
            double sourceLong = jsonObject.getDouble("SOURCE_LONG");
            double destLat = jsonObject.getDouble("DEST_LAT");
            double destLong = jsonObject.getDouble("DEST_LONG");

            ride.setPickupLocation(new LatLng(sourceLat, sourceLong));
            ride.setDestinationLocation(new LatLng(destLat, destLong));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d(TAG, "setRideData(): Exception thrown!");
        }
    }

    // Getters and Setters
    public Ride getRide()
    {
        return ride;
    }
    public void setRide(Ride ride)
    {
        this.ride = ride;
    }

    // Observe for IS_TAKING_RIDE
    public LiveData<Integer> getIsTakingRide()
    {
        return IS_TAKING_RIDE;
    }

    public void setIsTakingRide(Integer IS_TAKING_RIDE)
    {
        this.IS_TAKING_RIDE.setValue(IS_TAKING_RIDE);
    }

    public int getACTIVE_STATUS() {
        return ACTIVE_STATUS;
    }

    public void setACTIVE_STATUS(int ACTIVE_STATUS) {
        this.ACTIVE_STATUS = ACTIVE_STATUS;
    }
}
