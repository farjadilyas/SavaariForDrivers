package com.example.savaari_driver.register.fragments.driver;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.savaari_driver.R;
import com.example.savaari_driver.SavaariApplication;
import com.example.savaari_driver.Util;
import com.example.savaari_driver.entity.Driver;
import com.example.savaari_driver.register.fragments.DatePickerFragment;
import com.example.savaari_driver.register.fragments.RegistrationClickListener;
import com.example.savaari_driver.register.fragments.vehicle.VehicleRegistrationFragment;
import com.google.common.base.Strings;

public class DriverRegistrationFragment extends Fragment
{
    private static final int REQUEST_CODE = 11;
    // Main Attributes
    private DriverRegistrationViewModel mViewModel;
    private RegistrationClickListener registrationClickListener;
    private int formNumber = 0;

    // UI Elements
    private EditText firstNameText;
    private EditText lastNameText;
    private EditText dobText;
    private EditText phoneNumberText;
    private EditText cnicText;
    private EditText liceneseNumberText;
    private LinearLayout firstForm;
    private LinearLayout secondForm;
    private Button navFormButton;
    private Button registerButton;
    private ProgressBar loadingCircle;
    private TextView requestSentText;

    // Data Attributes
    String dateOfBirth;

    // ----------------------------------------------------------------------------------------------
    //                                   MAIN METHODS
    // ----------------------------------------------------------------------------------------------
    public static DriverRegistrationFragment newInstance(RegistrationClickListener registrationClickListener) {
        return new DriverRegistrationFragment(registrationClickListener);
    }

    public DriverRegistrationFragment() {
        // Empty Constructor
    }
    public DriverRegistrationFragment(RegistrationClickListener registrationClickListener)
    {
        this.registrationClickListener = registrationClickListener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // check for the results
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // get date from string
            int year = data.getIntExtra("YEAR", 0);
            int month = data.getIntExtra("MONTH", 0);
            int day = data.getIntExtra("DAY", 0);

            if (year != 0) {
                dateOfBirth = padWithZeroes(Integer.toString(year), 4) + "/" + ((month + 1 < 10) ? "0" + (month + 1) : "" + (month + 1))
                        + "/" + ((day < 10) ? "0" + day : "" + day);

                dobText.setText(dateOfBirth);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        // Get View
        View view = inflater.inflate(R.layout.driver_registration_fragment, container, false);

        // Setting UI Elements
        // Setting Forms
        firstForm = view.findViewById(R.id.first_form);
        secondForm = view.findViewById(R.id.second_form);

        // Setting Text Views
        firstNameText = view.findViewById(R.id.first_name);
        lastNameText = view.findViewById(R.id.last_name);
        dobText = view.findViewById(R.id.dob);
        phoneNumberText = view.findViewById(R.id.phone_number);
        cnicText = view.findViewById(R.id.CNIC);
        liceneseNumberText = view.findViewById(R.id.licenseNumber);
        requestSentText = view.findViewById(R.id.request_sent_msg);

        // Setting Loading Circle
        loadingCircle = view.findViewById(R.id.form_sent_circle);

        // Setting Date of Birth
        dobText.setOnClickListener(view1 -> {

            // create the datePickerFragment
            AppCompatDialogFragment newFragment = new DatePickerFragment();
            // set the targetFragment to receive the results, specifying the request code
            newFragment.setTargetFragment(DriverRegistrationFragment.this, REQUEST_CODE);
            // show the datePicker
            newFragment.show(this.getActivity().getSupportFragmentManager(), "datePicker");
        });

        // Setting Buttons
        navFormButton = view.findViewById(R.id.form_nav_button);
        navFormButton.setOnClickListener(view1 -> {
            formNumber = (formNumber + 1) % 2;
            toggleForms();
        });

        registerButton = view.findViewById(R.id.driver_registration_button);
        registerButton.setOnClickListener(view1 -> {

            loadingCircle.setVisibility(View.VISIBLE);

            // Calling View Model Function
            mViewModel.registerDriver(firstNameText.getText().toString(),
                    lastNameText.getText().toString(),
                    dobText.getText().toString(),
                    phoneNumberText.getText().toString(),
                    cnicText.getText().toString(),
                    liceneseNumberText.getText().toString());
        });

        return view;
    }

    // Function to perform checks from viewModel
    private void init()
    {
        // Setting Flags
        requestSentText.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Request Sent", Toast.LENGTH_SHORT).show();

        // Disabling UI
        registerButton.setEnabled(false);
        firstNameText.setKeyListener(null);
        lastNameText.setKeyListener(null);
        phoneNumberText.setKeyListener(null);
        dobText.setKeyListener(null);
        cnicText.setKeyListener(null);
        liceneseNumberText.setKeyListener(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this, new DriverRegistrationViewModelFactory(
                ((SavaariApplication) this.getActivity().getApplication()).getRepository())
        ).get(DriverRegistrationViewModel.class);

        // Setting Action for Request Sent
        mViewModel.getIsRequestSent().observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean != null) {
                loadingCircle.setVisibility(View.INVISIBLE);
                if (aBoolean)
                {
                    registrationClickListener.onVehicleRegistrationClick();
                } else {
                    Toast.makeText(getContext(), "Request Sent Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Function to pad a String with zeros
    public String padWithZeroes(String text, int length) {
        String pad = Strings.repeat("0", length);
        return (pad + text).substring(text.length());
    }

    private void toggleForms()
    {
        if (formNumber == 0) {
            firstForm.setAnimation(Util.inFromLeftAnimation(400));
            firstForm.setVisibility(View.VISIBLE);

            secondForm.setAnimation(Util.outToRightAnimation(400));
            secondForm.setVisibility(View.GONE);

            navFormButton.setText(R.string.previous_form);

        } else {
            firstForm.setAnimation(Util.outToLeftAnimation(400));
            firstForm.setVisibility(View.GONE);

            secondForm.setAnimation(Util.inFromRightAnimation(400));
            secondForm.setVisibility(View.VISIBLE);

            navFormButton.setText(R.string.next_form);
        }
    }
}