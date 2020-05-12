package org.tensorflow.lite.examples.detection.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.MainActivity;
import org.tensorflow.lite.examples.detection.R;


import static android.content.Context.MODE_PRIVATE;

public class VolleyRequest {
    private static String baseUrl = "http://snapcrack.ngrok.io";
    private static String KEY_USERNAME = "username";
    private static String KEY_PASSWORD = "password";
    private static String KEY_IS_LOGGED_IN = "isLoggedIn";

    public static void jsonObjectPostRequest(JSONObject js, String url, Context context) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                js,
                response -> Log.d("jsonObjectRequest", response.toString()),
                error -> {
                    if(error.getMessage() != null) {
                        Log.e("jsonObjectPostRequest", error.getMessage());
                    } else {
                        Log.e("jsonObjectPostRequest", "Message was null");
                    }
                }
        );

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    public static void createUser(JSONObject js, Context context) {
        String TAG = "VolleyRequest.createUsr";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                baseUrl + "/user",
                js,
                response -> {
                    try {
                        if(response.getBoolean("success")) {
                            Log.d(TAG, "User successfully created");
                            Intent intent = new Intent(context, CameraActivity.class);
                            context.startActivity(intent);
                        } else {
                            Log.e(TAG, response.getString("message"));
                        }
                    } catch (Exception e) {
                        Log.e("createUser", e.getMessage());
                    }
                },
                error -> {
                    if(error.getMessage() != null) {
                        Log.e("createUser", error.getMessage());
                    }
                }
        );

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }


    public static void loginUser(JSONObject js, Context context) {

        String TAG = "VolleyRequest.loginUser";

        String username = "";
        try {
            username = js.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String finalUsername = username;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                baseUrl + "/login" ,
                js,
                response -> {
                    Log.d("loginUser", response.toString());
                    Intent intent = new Intent(context, MainActivity.class);

                    try {
                        if (response.getBoolean("success")) {
                            Log.d(TAG, "Login success");
                            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_USERNAME, finalUsername);
                            editor.putBoolean(KEY_IS_LOGGED_IN, true);
                            editor.apply();
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, response.getString("message"), Toast.LENGTH_SHORT).show();
                        }

                    } catch(JSONException e){
                        e.printStackTrace();
                    }
                },
                error -> {
                    if(error.getMessage() != null) {
                        Log.e("loginUser", error.getMessage());

                    } else {
                        Log.e("loginUser", "error was null");
                    }
                }
        );

        VolleySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

}
