package com.example.savaari;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

// This class holds static functions for interacting with the API Layer
public class NetworkUtil
{
    // Main Attributes
    private static final NetworkUtil networkUtil = new NetworkUtil();
    private static final String TAG = "NetworkUtil";

    // Private Constructor
    private NetworkUtil()
    {
        // Empty
    }

    // -------------------------------------------------------------------------------
    //                                 Main Methods
    // -------------------------------------------------------------------------------

    // Sending POST Requests
    public static JSONObject sendPost(String urlAddress, JSONObject jsonParam, boolean needResponse) throws JSONException {

        JSONObject result = new JSONObject();
        try
        {
            // Creating the HTTP Connection
            URL url = new URL(urlAddress);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Sending the Data and Receiving Output
            Log.i(TAG, "sendPost: " + jsonParam.toString());
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(jsonParam.toString());

            // Flushing output streams
            os.flush();
            os.close();

            Log.i(TAG, "sendPost: Status: " + String.valueOf(conn.getResponseCode()));
            Log.i(TAG, "sendPost: Response Message: " + conn.getResponseMessage());

            // Sending the Response Back to the User in JSON
            if (needResponse)
            {
                Scanner scanner = null;
                try
                {
                    scanner = new Scanner(conn.getInputStream());
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                String response = scanner.useDelimiter("\\Z").next();
                JSONObject results = new JSONObject(response);
                Log.d(TAG, "sendPost: " + response);
                scanner.close();
                conn.disconnect();
                return results;
            }
            result.put("result", true);
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            result.put("result", false);
            return result;
        }
    }

    // Send Last Location
    public static int sendLastLocation(String urladdress, int currentUserID, double latitude, double longitude)
    {
        try
        {
            // TimeStamp
            long tsLong = System.currentTimeMillis() / 1000;
            String currentTimeStamp = Long.toString(tsLong);

            // JSON
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("USER_ID", currentUserID);
            jsonParam.put("LATITUDE", latitude);
            jsonParam.put("LONGITUDE", longitude);
            jsonParam.put("TIMESTAMP", currentTimeStamp);

            // Logging
            Log.d(TAG, "sendLastLocation: User_ID: " + currentUserID);
            Log.d(TAG, "sendLastLocation: Latitude: " + latitude);
            Log.d(TAG, "sendLastLocation: Longitude: " + longitude);
            Log.d(TAG, "sendLastLocation: TimeStamp: " + currentTimeStamp);

            // Sending JSON
            return NetworkUtil.sendPost(urladdress, jsonParam, false).getBoolean("result") ? 1 : 0;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
            return -1;
        }
    }
}
