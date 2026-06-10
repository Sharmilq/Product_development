package com.dentnova.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager — Java equivalent of SharedPreferences session logic in api_service.dart
 * Stores: user_id, token, name, email
 */
public class SessionManager {
    private static final String PREF_NAME   = "dentnova_session";
    private static final String KEY_TOKEN   = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME    = "name";
    private static final String KEY_EMAIL   = "email";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context ctx) {
        prefs  = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /** Saves login session — equivalent to ApiService.saveSession() */
    public void saveSession(int userId, String token, String name, String email) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
    public String getUserEmail() {
        return prefs.getString("email", "");
    }
    public String getUserName() {
        return prefs.getString("name", "");
    }
    public String getToken()  { return prefs.getString(KEY_TOKEN, null); }
    public int    getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
    public String getName()   { return prefs.getString(KEY_NAME, "User"); }
    public String getEmail()  { return prefs.getString(KEY_EMAIL, ""); }

    /** Equivalent to ApiService.isLoggedIn() */
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /** Equivalent to ApiService.clearSession() */
    public void clearSession() {
        editor.clear().apply();
    }
}
