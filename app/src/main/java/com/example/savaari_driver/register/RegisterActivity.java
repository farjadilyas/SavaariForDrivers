package com.example.savaari_driver.register;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.example.savaari_driver.MainActivity;
import com.example.savaari_driver.R;
import com.example.savaari_driver.SavaariApplication;
import com.example.savaari_driver.Util;
import com.example.savaari_driver.auth.login.LoginActivity;
import com.example.savaari_driver.entity.Vehicle;
import com.example.savaari_driver.register.fragments.driver.DriverRegistrationFragment;
import com.example.savaari_driver.register.fragments.RegistrationClickListener;
import com.example.savaari_driver.register.fragments.vehicle.VehicleRegistrationFragment;
import com.example.savaari_driver.entity.Driver;
import com.example.savaari_driver.ride.RideActivity;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends Util implements RegistrationClickListener
{
    private final String LOG_TAG = this.getClass().getCanonicalName();
    // Main Attributes
    private RegisterViewModel registerViewModel;
    private ProgressBar loadingCircle;
    private ScheduledFuture<?> future = null;

    // ------------------------------------------------------------------------------------------
    //                                      MAIN METHODS
    // ------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeSelect(this);
        super.onCreate(savedInstanceState);

        // Setting UI Elements
        setContentView(R.layout.activity_register);
        // loadingCircle = findViewById(R.id.progressBar_2);

        // Getting Stored Data
        Intent recvIntent = getIntent();
        if (!recvIntent.getBooleanExtra("API_CONNECTION", true)) {
            Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
        }
        int USER_ID = recvIntent.getIntExtra("USER_ID", -1);

        // Creating View Model
        registerViewModel = ViewModelProviders.of(this, new RegisterViewModelFactory(
                ((SavaariApplication) this.getApplication()).getRepository())
        ).get(RegisterViewModel.class);

        // Calling the Service again to Load Driver if not already
        if (registerViewModel.getDriver() == null) {
            future = ((SavaariApplication) getApplication()).scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> registerViewModel.loadUserData(USER_ID),
                    0L, 8L, TimeUnit.SECONDS);
        }
        // On Data Loaded
        registerViewModel.getUserdataLoaded().observe(this, aBoolean -> {
            if (aBoolean != null) {
                // loadingCircle.setVisibility(View.INVISIBLE);
                if (aBoolean) {
                    Toast.makeText(this, "User Data Loaded!", Toast.LENGTH_SHORT).show();
                    if (future != null) {
                        future.cancel(true);
                    }
                    checkDriverStatus();
                } else {
                    Toast.makeText(this, "User Data could not be loaded!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Main Function for driver account logic
    private void checkDriverStatus()
    {
        // Main Check depending on Driver Account Status
        switch (registerViewModel.getDriver().getStatus())
        {
            case Driver.DV_DEFAULT:
            {
                // Driver needs to fill the form and send it
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.register_frame, DriverRegistrationFragment.newInstance(this))
                        .commit();
                break;
            }
            case Driver.DV_REQ_SENT:
            {
                // Request is sent, Driver can add vehicles or see vehicles status
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.register_frame, VehicleRegistrationFragment.newInstance(this))
                        .commit();
                break;
            }
            case Driver.DV_REQ_REJECTED:
            {
                // Request Rejected: handle later
                break;
            }
            case Driver.DV_REQ_APPROVED:
            {
                // Request Approved, check vehicles
                if (registerViewModel.getDriver().getActiveVehicleID() != Vehicle.DEFAULT_ID) {
                    // Goto Ride Activity
                    launchRideActivity();
                }
                break;
            }
        }
    }

    private void launchRideActivity() {
        Log.d(LOG_TAG, "launchRideActivity: Launching Ride Activity");
        Intent i = new Intent(RegisterActivity.this, RideActivity.class);
        i.putExtra("API_CONNECTION", true);
        i.putExtra("USER_ID", registerViewModel.getDriver().getUserID());
        startActivity(i);
        finish();
    }

    // Interface Implementation
    @Override
    public void onDriverRegistrationClick() {
        // Driver needs to fill the form and send it
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.register_frame, DriverRegistrationFragment.newInstance(this))
                .commit();
    }

    @Override
    public void onVehicleRegistrationClick() {
        // Driver needs to fill the form and send it
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.register_frame, VehicleRegistrationFragment.newInstance(this))
                .commit();
    }
}
