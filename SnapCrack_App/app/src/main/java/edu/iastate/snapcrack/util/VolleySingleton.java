package edu.iastate.snapcrack.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


/**
 * Singleton class that creates a single RequestQueue for the whole application to be used
 * for Volley requests
 */
public class VolleySingleton {

    private String TAG = "VolleySingleton";

    private static VolleySingleton mInstance;
    private static Context mContext;
    private RequestQueue requestQueue;

    private VolleySingleton(Context context) {
        mContext = context;
        requestQueue = getRequestQueue();
    }

    public static VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            //To make thread safe
            synchronized (VolleySingleton.class) {
                //check again as multiple threads
                // can reach above step
                if (mInstance == null) {
                    mInstance = new VolleySingleton(context);
                }
            }
        }
        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if(requestQueue == null) {
            requestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return requestQueue;
    }

    public<T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }

}
