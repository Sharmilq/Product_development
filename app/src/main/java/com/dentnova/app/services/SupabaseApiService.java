package com.dentnova.app.services;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class SupabaseApiService {

    private static RequestQueue requestQueue;

    public static RequestQueue getQueue(Context context) {

        if (requestQueue == null) {
            requestQueue =
                    Volley.newRequestQueue(
                            context.getApplicationContext()
                    );
        }

        return requestQueue;
    }
}