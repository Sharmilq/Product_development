package com.dentnova.app.services;

import android.content.Context;
import com.google.gson.JsonArray;
import com.dentnova.app.utils.SessionManager;
import com.google.gson.Gson;
import com.dentnova.app.SupabaseConfig;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import java.nio.charset.Charset;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

/**
 * ApiService.java — exact Java equivalent of api_service.dart
 * Uses OkHttp (replaces dart:http) + Gson (replaces dart jsonDecode/jsonEncode)
 *
 * All methods are synchronous — call from background thread (AsyncTask /
 * ExecutorService)
 * Equivalent Flutter method listed in each Javadoc comment.
 */
public class ApiService {

    /**
     * Replace with your server IP — same as baseUrl in api_service.dart
     */
    // public static final String BASE_URL = "http://10.46.55.51/dentnova";

    // ── LOCAL BACKEND URL ────────────────────────────────────────────────────
    // Replace 192.168.x.x with your laptop's actual Wi-Fi IP (run: ipconfig).
    // Must match the IP set in SupabaseConfig.BACKEND_URL.
    public static final String ML_BASE_URL = "http://10.33.82.51:5000";
    public static final String SUPABASE_AUTH_URL = SupabaseConfig.SUPABASE_URL + "/auth/v1";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(new SupabaseLoggingInterceptor())
            .build();

    private static final Gson gson = new Gson();

    // ── Auth header helper ─────────────────────────────────────────────────
    private static Headers authHeaders(Context ctx) {
        SessionManager s = new SessionManager(ctx);
        return new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Authorization", "Bearer " + s.getToken())
                .build();
    }

    // supabase//
    private static Headers supabaseHeaders() {
        return new Headers.Builder()
                .add("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .add("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                .add("Content-Type", "application/json")
                .add("Prefer", "return=representation")
                .build();
    }

    // ── Register ── ApiService.register() ─────────────────────────────────
    public static JsonObject register(Context ctx, String name, String email, String password)
            throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("name", name);
        body.add("data", metadata);

        Request req = new Request.Builder()
                .url(SUPABASE_AUTH_URL + "/signup")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("SUPABASE_REGISTER", "Register response: " + raw);

        JsonObject result = new JsonObject();

        if (raw.contains("\"id\"")) {
            // Also insert a row in the users table so user_id is consistent
            try {
                int localUserId = email.hashCode();
                if (localUserId < 0)
                    localUserId = -localUserId;

                JsonObject userRow = new JsonObject();
                userRow.addProperty("user_id", localUserId);
                userRow.addProperty("name", name);
                userRow.addProperty("email", email);
                userRow.addProperty("age", 20);
                userRow.addProperty("gender", "Female");
                userRow.addProperty("concerns", "");
                userRow.addProperty("photo_url", "");

                Request insertReq = new Request.Builder()
                        .url(SupabaseConfig.REST_URL + "users")
                        .post(RequestBody.create(userRow.toString(), JSON))
                        .headers(supabaseHeaders())
                        .build();
                String insertRaw = client.newCall(insertReq).execute().body().string();
                android.util.Log.d("SUPABASE_REGISTER", "User row insert: " + insertRaw);

                // Save session so ProfileSetupActivity can immediately update the profile
                new SessionManager(ctx).saveSession(localUserId, "", name, email);

            } catch (Exception e) {
                android.util.Log.e("SUPABASE_REGISTER", "Failed to insert user row: " + e.getMessage());
            }
            result.addProperty("success", true);
            result.addProperty("message", "Account created successfully");
        } else {
            result.addProperty("success", false);
            android.util.Log.e("SUPABASE_REGISTER", "Register failed: " + raw);
            result.addProperty("message", raw);
        }

        return result;
    }

    // ── Forgot Password → Node.js Render Backend OTP Flow ───────────────────
    // Calls Render Node.js backend /auth/request-password-otp
    public static JsonObject forgotPassword(String email) throws IOException {
        android.util.Log.d("REQUEST_OTP", "Email entered: " + email);
        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        String url = SupabaseConfig.BACKEND_URL + "/auth/request-password-otp";
        android.util.Log.d("REQUEST_OTP", "Request URL: " + url);
        android.util.Log.d("REQUEST_OTP", "HTTP method: POST");

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            try (okhttp3.Response response = client.newCall(req).execute()) {
                int code = response.code();
                android.util.Log.d("REQUEST_OTP", "Response code: " + code);

                String raw = "";
                if (response.body() != null) {
                    raw = response.body().string();
                }
                android.util.Log.d("REQUEST_OTP", "Response body: " + raw);

                JsonObject result = new JsonObject();
                if (response.isSuccessful()) {
                    android.util.Log.d("OTP_SENT", "OTP email request successful for: " + email);
                    result.addProperty("success", true);
                    try {
                        JsonObject data = gson.fromJson(raw, JsonObject.class);
                        if (data != null && data.has("message")) {
                            result.addProperty("message", data.get("message").getAsString());
                        } else {
                            result.addProperty("message", "OTP sent successfully.");
                        }
                    } catch (Exception parseEx) {
                        result.addProperty("message", "OTP sent successfully.");
                    }
                } else {
                    result.addProperty("success", false);
                    try {
                        JsonObject data = gson.fromJson(raw, JsonObject.class);
                        if (data != null && data.has("message")) {
                            result.addProperty("message", data.get("message").getAsString());
                        } else {
                            result.addProperty("message", "HTTP error " + code);
                        }
                    } catch (Exception parseEx) {
                        result.addProperty("message", "HTTP error " + code + " (Backend returned non-JSON response)");
                    }
                }
                return result;
            }
        } catch (Exception e) {
            android.util.Log.e("REQUEST_OTP", "Exception message: " + e.getMessage());
            android.util.Log.e("REQUEST_OTP", "Stack trace: ", e);
            throw new IOException(e);
        }
    }

    // ── Verify OTP → Render Custom OTP Flow ──────────────────────────────────
    // Calls Render backend /auth/verify-password-otp
    public static JsonObject verifyPasswordOtp(String email, String otp) throws IOException {
        android.util.Log.d("OTP_VERIFY", "Email entered for verification: " + email + ", OTP: " + otp);
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("otp", otp);

        String url = SupabaseConfig.BACKEND_URL + "/auth/verify-password-otp";
        android.util.Log.d("OTP_VERIFY", "Request URL: " + url);
        android.util.Log.d("OTP_VERIFY", "HTTP method: POST");

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            try (okhttp3.Response response = client.newCall(req).execute()) {
                int code = response.code();
                android.util.Log.d("OTP_VERIFY", "Response code: " + code);

                String raw = "";
                if (response.body() != null) {
                    raw = response.body().string();
                }
                android.util.Log.d("OTP_VERIFY", "Response body: " + raw);

                JsonObject result = new JsonObject();
                if (response.isSuccessful()) {
                    android.util.Log.d("OTP_VALID", "OTP successfully verified for: " + email);
                    result.addProperty("success", true);
                } else {
                    result.addProperty("success", false);
                    String msg = "Verification failed.";
                    try {
                        JsonObject data = gson.fromJson(raw, JsonObject.class);
                        if (data != null && data.has("message")) {
                            msg = data.get("message").getAsString();
                        } else {
                            msg = "HTTP error " + code;
                        }
                    } catch (Exception parseEx) {
                        msg = "HTTP error " + code + " (Backend returned non-JSON response)";
                    }
                    result.addProperty("message", msg);
                }
                return result;
            }
        } catch (Exception e) {
            android.util.Log.e("OTP_VERIFY", "Exception message: " + e.getMessage());
            android.util.Log.e("OTP_VERIFY", "Stack trace: ", e);
            throw new IOException(e);
        }
    }

    // ── Reset Password with OTP → Render Custom OTP Flow ───────────────────
    // Calls Render backend /auth/reset-password-with-otp
    public static JsonObject resetPasswordWithOtp(String email, String otp, String newPassword)
            throws IOException {
        android.util.Log.d("PASSWORD_RESET", "Email entered for reset: " + email);
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("otp", otp);
        body.addProperty("newPassword", newPassword);

        String url = SupabaseConfig.BACKEND_URL + "/auth/reset-password-with-otp";
        android.util.Log.d("PASSWORD_RESET", "Request URL: " + url);
        android.util.Log.d("PASSWORD_RESET", "HTTP method: POST");

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            try (okhttp3.Response response = client.newCall(req).execute()) {
                int code = response.code();
                android.util.Log.d("PASSWORD_RESET", "Response code: " + code);

                String raw = "";
                if (response.body() != null) {
                    raw = response.body().string();
                }
                android.util.Log.d("PASSWORD_RESET", "Response body: " + raw);

                JsonObject result = new JsonObject();
                if (response.isSuccessful()) {
                    android.util.Log.d("PASSWORD_RESET_SUCCESS", "Password updated successfully for: " + email);
                    result.addProperty("success", true);
                    result.addProperty("message", "Password updated successfully");
                } else {
                    android.util.Log.d("PASSWORD_RESET_FAILED", "Password update failed for: " + email);
                    result.addProperty("success", false);
                    String msg = "Password reset failed.";
                    try {
                        JsonObject data = gson.fromJson(raw, JsonObject.class);
                        if (data != null && data.has("message")) {
                            msg = data.get("message").getAsString();
                        } else {
                            msg = "HTTP error " + code;
                        }
                    } catch (Exception parseEx) {
                        msg = "HTTP error " + code + " (Backend returned non-JSON response)";
                    }
                    result.addProperty("message", msg);
                }
                return result;
            }
        } catch (Exception e) {
            android.util.Log.d("PASSWORD_RESET_FAILED", "Exception message: " + e.getMessage());
            android.util.Log.e("PASSWORD_RESET_FAILED", "Stack trace: ", e);
            throw new IOException(e);
        }
    }

    // ── Login ── ApiService.login() ────────────────────────────────────────
    public static JsonObject login(Context ctx, String email, String password)
            throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request req = new Request.Builder()
                .url(SUPABASE_AUTH_URL + "/token?grant_type=password")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("SUPABASE_LOGIN", "Auth response: " + raw);

        JsonObject data = gson.fromJson(raw, JsonObject.class);

        JsonObject result = new JsonObject();

        if (data.has("access_token")) {

            String token = data.get("access_token").getAsString();
            JsonObject user = data.getAsJsonObject("user");
            String userEmail = user.get("email").getAsString();

            String userName = "";
            if (user.has("user_metadata")) {
                JsonObject meta = user.getAsJsonObject("user_metadata");
                if (meta.has("name")) {
                    userName = meta.get("name").getAsString();
                }
            }

            // FIX: Look up real user_id from users table by email.
            // Do NOT use supabase_uuid.hashCode() — that differs from email.hashCode() used
            // at registration.
            int localUserId = lookupUserIdByEmail(userEmail);
            android.util.Log.d("SUPABASE_LOGIN", "Looked up user_id for " + userEmail + ": " + localUserId);

            new SessionManager(ctx).saveSession(
                    localUserId,
                    token,
                    userName,
                    userEmail);

            result.addProperty("success", true);

        } else {

            result.addProperty("success", false);
            android.util.Log.e("SUPABASE_LOGIN", "Login failed raw response: " + raw);

            if (raw.contains("Invalid login credentials")) {
                result.addProperty("message", "Invalid email or password");
            } else if (raw.contains("Email not confirmed")) {
                result.addProperty("message", "Please verify your email first");
            } else {
                result.addProperty("message", "Login failed. Please try again");
            }
        }

        return result;
    }

    /**
     * Looks up the integer user_id stored in the users table for the given email.
     * Returns email.hashCode() (with sign correction) as fallback if user not
     * found.
     */
    private static int lookupUserIdByEmail(String email) {
        try {
            Request req = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users?email=eq." + email + "&select=user_id&limit=1")
                    .get()
                    .headers(supabaseHeaders())
                    .build();
            String raw = client.newCall(req).execute().body().string();
            android.util.Log.d("SUPABASE_LOGIN", "lookupUserIdByEmail raw: " + raw);
            JsonArray arr = gson.fromJson(raw, JsonArray.class);
            if (arr != null && arr.size() > 0) {
                JsonObject row = arr.get(0).getAsJsonObject();
                if (row.has("user_id")) {
                    return row.get("user_id").getAsInt();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_LOGIN", "lookupUserIdByEmail failed: " + e.getMessage());
        }
        // Fallback: derive same way registration does
        int fallback = email.hashCode();
        if (fallback < 0)
            fallback = -fallback;
        return fallback;
    }

    // ── Logout ── ApiService.logout() ─────────────────────────────────────
    public static void logout(Context ctx) {
        new SessionManager(ctx).clearSession();
    }

    // ── getProfile ── ApiService.getProfile() ─────────────────────────────
    public static JsonObject getProfile(Context ctx) throws IOException {

        int userId = new SessionManager(ctx).getUserId();
        String email = new SessionManager(ctx).getUserEmail();

        android.util.Log.d("SUPABASE_PROFILE", "ANDROID_CURRENT_USER_ID: " + userId);
        android.util.Log.d("SUPABASE_PROFILE", "ANDROID_CURRENT_EMAIL: " + email);

        String url = SupabaseConfig.REST_URL + "users?user_id=eq." + userId + "&select=*";
        android.util.Log.d("SUPABASE_PROFILE", "ANDROID_PROFILE_REQUEST_URL: " + url);

        Request req = new Request.Builder()
                .url(url)
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = "";
        try (okhttp3.Response response = client.newCall(req).execute()) {
            int code = response.code();
            android.util.Log.d("SUPABASE_PROFILE", "ANDROID_PROFILE_RESPONSE_CODE: " + code);

            if (response.body() != null) {
                raw = response.body().string();
            }
            android.util.Log.d("SUPABASE_PROFILE", "ANDROID_PROFILE_RESPONSE_BODY: " + raw);

            if (!response.isSuccessful() || raw.contains("\"code\"")) {
                android.util.Log.e("SUPABASE_PROFILE", "ANDROID_SUPABASE_ERROR: " + raw);
            }

            com.google.gson.JsonArray arr = gson.fromJson(raw, com.google.gson.JsonArray.class);

            JsonObject result = new JsonObject();

            if (arr != null && arr.size() > 0) {
                result.addProperty("success", true);
                result.add("profile", arr.get(0).getAsJsonObject());
            } else {
                result.addProperty("success", false);
            }

            return result;
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_PROFILE", "ANDROID_SUPABASE_ERROR: " + e.getMessage(), e);
            throw e;
        }
    }

    // ── updateProfile ── ApiService.updateProfile() ───────────────────────
    public static JsonObject updateProfile(
            Context ctx,
            String name,
            int age,
            String gender,
            String concerns,
            String photoBase64) throws IOException {

        JsonObject result = new JsonObject();

        try {
            int userId = new SessionManager(ctx).getUserId();
            String email = new SessionManager(ctx).getUserEmail();

            android.util.Log.d("ApiService", "CURRENT_SESSION_USER_ID: " + userId);
            android.util.Log.d("ApiService", "CURRENT_SESSION_EMAIL: " + email);
            android.util.Log.d("ApiService", "PROFILE_UPDATE_EMAIL: " + email);

            JsonObject body = new JsonObject();
            body.addProperty("user_id", userId);
            body.addProperty("name", name);
            if (email != null && !email.trim().isEmpty()) {
                body.addProperty("email", email);
            }
            body.addProperty("age", age);
            body.addProperty("gender", gender);
            body.addProperty("concerns", concerns);
            if (photoBase64 != null && !photoBase64.isEmpty()) {
                body.addProperty("photo_url", photoBase64);
            }

            Request req = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId)
                    .patch(RequestBody.create(body.toString(), JSON))
                    .headers(supabaseHeaders())
                    .build();
            String raw = client.newCall(req)
                    .execute()
                    .body()
                    .string();

            android.util.Log.d("SUPABASE_PROFILE_SAVE", raw);

            result.addProperty("success", !raw.contains("code"));
            result.addProperty("message", raw);

        } catch (Exception e) {
            e.printStackTrace();
            result.addProperty("success", false);
            result.addProperty("message", e.getMessage());
        }

        return result;
    }

    // ── saveAssessment ── ApiService.saveAssessment() ─────────────────────
    public static JsonObject saveAssessment(Context ctx, int score, String label,
            Map<Integer, Integer> answers) throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("user_id", new SessionManager(ctx).getUserId());
        body.addProperty("score", score);
        body.addProperty("risk", label);

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "assessments")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req).execute().body().string();
        try {

            JsonObject notif = new JsonObject();

            notif.addProperty(
                    "user_id",
                    new SessionManager(ctx).getUserId());

            notif.addProperty(
                    "title",
                    "Assessment Completed 🦷");

            notif.addProperty(
                    "body",
                    "Your oral health score is "
                            + score +
                            " (" + label + ")");

            Request notifReq = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "notifications")
                    .post(
                            RequestBody.create(
                                    notif.toString(),
                                    JSON))
                    .headers(supabaseHeaders())
                    .build();

            client.newCall(notifReq)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e) {
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response) throws IOException {

                            if (response.body() != null) {
                                response.body().close();
                            }
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }

        android.util.Log.d("SUPABASE_SAVE_ASSESSMENT", raw);

        JsonObject result = new JsonObject();
        result.addProperty("success", !raw.contains("code"));
        result.addProperty("message", raw);

        return result;
    }

    // ── getAssessmentHistory ── ApiService.getAssessmentHistory() ─────────
    public static JsonObject getAssessmentHistory(Context ctx) throws IOException {
        int userId = new SessionManager(ctx).getUserId();
        android.util.Log.d("ASSESSMENT_QUERY_USER", "ASSESSMENT_QUERY_USER: " + userId);

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "assessments?user_id=eq." + userId + "&select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = "";
        try (okhttp3.Response response = client.newCall(req).execute()) {
            if (response.body() != null) {
                raw = response.body().string();
            }
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_GET_ASSESSMENTS", "Error fetching assessments", e);
        }

        android.util.Log.d("SUPABASE_GET_ASSESSMENTS", raw);

        com.google.gson.JsonArray array = null;
        if (!raw.isEmpty()) {
            try {
                com.google.gson.JsonElement jsonElement = gson.fromJson(raw, com.google.gson.JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonArray()) {
                    array = jsonElement.getAsJsonArray();
                } else {
                    android.util.Log.e("SUPABASE_GET_ASSESSMENTS", "Expected JsonArray, but got: " + raw);
                }
            } catch (Exception e) {
                android.util.Log.e("SUPABASE_GET_ASSESSMENTS",
                        "JSON parsing crash from Supabase response (assessments)", e);
            }
        }
        if (array == null) {
            array = new com.google.gson.JsonArray();
        }

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.add("assessments", array);

        return result;
    }

    // ── getReminders ── ApiService.getReminders() ─────────────────────────
    public static JsonObject getReminders(Context ctx) throws IOException {
        int userId = new SessionManager(ctx).getUserId();

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "reminders?user_id=eq." + userId + "&select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = "";
        try (okhttp3.Response response = client.newCall(req).execute()) {
            if (response.body() != null) {
                raw = response.body().string();
            }
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_GET_REMINDERS", "Error fetching reminders", e);
        }

        com.google.gson.JsonArray array = null;
        if (!raw.isEmpty()) {
            try {
                com.google.gson.JsonElement jsonElement = gson.fromJson(raw, com.google.gson.JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonArray()) {
                    array = jsonElement.getAsJsonArray();
                } else {
                    android.util.Log.e("SUPABASE_GET_REMINDERS", "Expected JsonArray, but got: " + raw);
                }
            } catch (Exception e) {
                android.util.Log.e("SUPABASE_GET_REMINDERS", "JSON parsing crash from Supabase response (reminders)",
                        e);
            }
        }
        if (array == null) {
            array = new com.google.gson.JsonArray();
        }

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.add("reminders", array);

        return result;
    }

    // ── addReminder ── ApiService.addReminder() ───────────────────────────
    public static JsonObject addReminder(Context ctx, String title, String time, String days)
            throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("user_id", new SessionManager(ctx).getUserId());
        body.addProperty("title", title);
        body.addProperty("time", time);
        body.addProperty("days", days);
        body.addProperty("enabled", true);

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "reminders")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("SUPABASE_ADD_REMINDER", raw);

        JsonObject result = new JsonObject();
        boolean success = !raw.contains("code");
        result.addProperty("success", success);
        result.addProperty("raw", raw);
        if (success) {
            try {
                com.google.gson.JsonArray arr = gson.fromJson(raw, com.google.gson.JsonArray.class);
                if (arr != null && arr.size() > 0) {
                    int reminderId = arr.get(0).getAsJsonObject().get("id").getAsInt();
                    result.addProperty("id", reminderId);
                }
            } catch (Exception e) {
                android.util.Log.e("addReminder", "Error parsing reminder ID", e);
            }
        }

        return result;
    }

    public static JsonObject predictAssessment(JsonObject data)
            throws Exception {

        okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json");

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                data.toString(),
                JSON);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(ML_BASE_URL + "/predict")
                .post(body)
                .build();

        okhttp3.Response response = client.newCall(request).execute();

        String responseBody = response.body().string();

        return gson.fromJson(
                responseBody,
                JsonObject.class);
    }

    // ── toggleReminder ── ApiService.toggleReminder() ─────────────────────
    public static JsonObject toggleReminder(
            Context ctx,
            int id,
            boolean enabled) throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("enabled", enabled);

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "reminders?id=eq." + id)
                .patch(
                        RequestBody.create(
                                body.toString(),
                                JSON))
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        JsonObject result = new JsonObject();

        result.addProperty("success", true);

        return result;
    }

    // ── deleteReminder ── ApiService.deleteReminder() ─────────────────────
    public static JsonObject deleteReminder(
            Context ctx,
            int id) throws IOException {

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "reminders?id=eq." + id)
                .delete()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        JsonObject result = new JsonObject();

        result.addProperty("success", true);

        return result;
    }

    public static void cleanupExpiredReminders(Context ctx) {
        android.util.Log.d("REMINDER_CLEANUP_STARTED", "REMINDER_CLEANUP_STARTED");

        SimpleDateFormat todaySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = todaySdf.format(new java.util.Date());
        android.util.Log.d("TODAY_DATE", "TODAY_DATE: " + todayStr);

        try {
            JsonObject response = getReminders(ctx);
            if (response.has("success") && response.get("success").getAsBoolean()) {
                JsonArray reminders = response.getAsJsonArray("reminders");
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);

                SimpleDateFormat parser = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                for (int i = 0; i < reminders.size(); i++) {
                    JsonObject r = reminders.get(i).getAsJsonObject();
                    int id = r.get("id").getAsInt();
                    String title = r.get("title").getAsString();
                    String timeStr = r.get("time").getAsString();
                    String days = r.get("days").getAsString();

                    boolean isExpired = false;
                    if ("ONCE".equalsIgnoreCase(days)
                            || (!timeStr.contains(":") && !timeStr.contains("AM") && !timeStr.contains("PM"))) {
                        try {
                            java.util.Date reminderDate = parser.parse(timeStr);
                            if (reminderDate != null) {
                                Calendar reminderCal = Calendar.getInstance();
                                reminderCal.setTime(reminderDate);
                                reminderCal.set(Calendar.HOUR_OF_DAY, 0);
                                reminderCal.set(Calendar.MINUTE, 0);
                                reminderCal.set(Calendar.SECOND, 0);
                                reminderCal.set(Calendar.MILLISECOND, 0);

                                if (reminderCal.before(todayCal)) {
                                    isExpired = true;
                                }
                            }
                        } catch (Exception e) {
                            // ignore parse error
                        }
                    }

                    if (isExpired) {
                        android.util.Log.d("EXPIRED_REMINDER_FOUND", "EXPIRED_REMINDER_FOUND: ID=" + id);

                        // Disable in Supabase
                        toggleReminder(ctx, id, false);
                        android.util.Log.d("EXPIRED_REMINDER_DISABLED", "EXPIRED_REMINDER_DISABLED: ID=" + id);

                        // Delete from Supabase
                        deleteReminder(ctx, id);
                        android.util.Log.d("EXPIRED_REMINDER_DELETED", "EXPIRED_REMINDER_DELETED: ID=" + id);

                        // Cancel alarm
                        com.dentnova.app.utils.ReminderScheduler.cancelReminderAlarm(ctx, id);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("cleanupExpiredReminders", "Error running reminders cleanup", e);
        }
    }

    // feedback
    public static JsonObject sendFeedback(
            Context ctx,
            String message) throws IOException {

        SessionManager session = new SessionManager(ctx);
        int userId = session.getUserId();
        String email = session.getUserEmail();
        String finalEmail = (email != null) ? email : "";

        android.util.Log.d("FEEDBACK_START", "Starting feedback submission");
        android.util.Log.d("FEEDBACK_USER_ID", String.valueOf(userId));
        android.util.Log.d("FEEDBACK_USER_EMAIL", finalEmail);
        android.util.Log.d("FEEDBACK_MESSAGE", message);

        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("user_email", finalEmail);
        body.addProperty("message", message);

        String url = SupabaseConfig.REST_URL + "feedback";
        android.util.Log.d("FEEDBACK_REQUEST_URL", url);
        android.util.Log.d("FEEDBACK_REQUEST_BODY", body.toString());

        Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        JsonObject result = new JsonObject();
        try {
            okhttp3.Response response = client.newCall(req).execute();
            int code = response.code();
            String raw = response.body() != null ? response.body().string() : "";

            android.util.Log.d("FEEDBACK_RESPONSE_CODE", String.valueOf(code));
            android.util.Log.d("FEEDBACK_RESPONSE_BODY", raw);

            boolean success = code >= 200 && code < 300;
            result.addProperty("success", success);
            if (!success) {
                result.addProperty("error", raw);
            } else {
                android.util.Log.d("FEEDBACK_INSERT_SUCCESS", "true");
            }
        } catch (Exception e) {
            android.util.Log.e("FEEDBACK_EXCEPTION", "Exception in sendFeedback", e);
            result.addProperty("success", false);
            result.addProperty("error", e.getMessage());
        }

        return result;
    }

    // ── saveVisitReminder ── ApiService.saveVisitReminder() ───────────────
    public static JsonObject saveVisitReminder(
            Context ctx,
            String date,
            String time,
            String note) throws IOException {

        JsonObject body = new JsonObject();

        body.addProperty(
                "user_id",
                new SessionManager(ctx).getUserId());

        body.addProperty("visit_date", date);
        body.addProperty("visit_time", time);
        body.addProperty("note", note);

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "visits")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        android.util.Log.d("SUPABASE_VISIT_SAVE", raw);

        JsonObject result = new JsonObject();
        boolean success = !raw.contains("code");
        result.addProperty("success", success);
        result.addProperty("message", raw);
        if (success) {
            try {
                com.google.gson.JsonArray arr = gson.fromJson(raw, com.google.gson.JsonArray.class);
                if (arr != null && arr.size() > 0) {
                    int visitId = arr.get(0).getAsJsonObject().get("id").getAsInt();
                    result.addProperty("id", visitId);
                }
            } catch (Exception e) {
                android.util.Log.e("saveVisitReminder", "Error parsing insert id", e);
            }
        }

        return result;
    }

    // ── deleteVisitReminder ── ApiService.deleteVisitReminder() ───────────
    public static JsonObject deleteVisitReminder(
            Context ctx,
            int id) throws IOException {

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "visits?id=eq." + id)
                .delete()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        JsonObject result = new JsonObject();

        result.addProperty("success", true);

        return result;
    }

    // ── getVisitReminder ── ApiService.getVisitReminder() ────────────────
    public static JsonObject getVisitReminder(
            Context ctx) throws IOException {

        int userId = new SessionManager(ctx)
                .getUserId();

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "visits?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        android.util.Log.d(
                "SUPABASE_VISITS",
                raw);

        JsonArray array = gson.fromJson(
                raw,
                JsonArray.class);

        JsonObject result = new JsonObject();

        result.addProperty("success", true);
        result.add("visits", array);

        return result;
    }

    // ── getNotifications ── ApiService.getNotifications() ────────────────
    public static JsonObject getNotifications(
            Context ctx) throws IOException {

        int userId = new SessionManager(ctx)
                .getUserId();

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "notifications?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        android.util.Log.d(
                "SUPABASE_NOTIFICATIONS",
                raw);

        JsonArray array = gson.fromJson(
                raw,
                JsonArray.class);

        JsonObject result = new JsonObject();

        result.addProperty("success", true);
        result.add("notifications", array);

        return result;
    }

    // ── saveToothScan ── ApiService.saveToothScan() (multipart) ──────────
    public static JsonObject saveToothScan(
            Context ctx,
            File imageFile,
            double plaqueScore,
            double gumScore,
            double cleanlinessScore,
            String resultLabel) throws IOException {

        JsonObject body = new JsonObject();

        body.addProperty(
                "user_id",
                new SessionManager(ctx).getUserId());

        body.addProperty(
                "plaque_score",
                plaqueScore);

        body.addProperty(
                "gum_score",
                gumScore);

        body.addProperty(
                "cleanliness_score",
                cleanlinessScore);

        body.addProperty(
                "result_label",
                resultLabel);
        byte[] imageBytes = java.nio.file.Files.readAllBytes(
                imageFile.toPath());

        String imageBase64 = android.util.Base64.encodeToString(
                imageBytes,
                android.util.Base64.NO_WRAP);

        body.addProperty("image_base64", imageBase64);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "tooth_scans")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req)
                .execute()
                .body()
                .string();

        android.util.Log.d(
                "SUPABASE_SCAN_SAVE",
                raw);

        JsonObject result = new JsonObject();

        result.addProperty(
                "success",
                !raw.contains("code"));

        result.addProperty(
                "message",
                raw);

        return result;
    }

    // predict tooth scan
    public static JsonObject predictToothScan(
            Context ctx,
            File imageFile) throws IOException {

        RequestBody reqBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "image",
                        imageFile.getName(),
                        RequestBody.create(
                                imageFile,
                                MediaType.parse("image/*")))
                .build();

        Request req = new Request.Builder()
                .url(ML_BASE_URL + "/predict-tooth")
                .post(reqBody)
                .build();

        Response response = client.newCall(req)
                .execute();

        String raw = response.body()
                .string();

        android.util.Log.d(
                "TOOTH_AI_RAW",
                raw);

        return gson.fromJson(
                raw,
                JsonObject.class);
    }

    // ── getToothScans ── ApiService.getToothScans() ───────────────────────
    public static JsonObject getToothScans(
            Context ctx) throws IOException {

        int userId = new SessionManager(ctx)
                .getUserId();
        android.util.Log.d("SCAN_QUERY_USER", "SCAN_QUERY_USER: " + userId);

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "tooth_scans?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = "";
        try (okhttp3.Response response = client.newCall(req).execute()) {
            if (response.body() != null) {
                raw = response.body().string();
            }
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_SCAN_HISTORY", "Error fetching tooth scans", e);
        }

        android.util.Log.d(
                "SUPABASE_SCAN_HISTORY",
                raw);

        JsonArray array = gson.fromJson(
                raw,
                JsonArray.class);

        JsonObject result = new JsonObject();

        result.addProperty("success", true);
        result.add("scans", array);

        return result;

    }

    // seetings
    public static JsonObject changePassword(Context ctx, String newPassword)
            throws IOException {
        return changePassword(ctx, newPassword, null);
    }

    public static JsonObject changePassword(Context ctx, String newPassword, String explicitToken)
            throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        String token = (explicitToken != null) ? explicitToken : new SessionManager(ctx).getToken();

        Request req = new Request.Builder()
                .url(SUPABASE_AUTH_URL + "/user")
                .put(RequestBody.create(body.toString(), JSON))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();

        JsonObject result = new JsonObject();
        result.addProperty("success", raw.contains("\"id\""));
        result.addProperty("message", raw);

        return result;
    }

    // ── Sync Google User with Supabase (links existing or inserts new profile) ──
    public static JsonObject syncGoogleUserWithSupabase(Context ctx, String name, String email, String firebaseUid,
            String photoUrl)
            throws IOException {
        JsonObject result = new JsonObject();

        // Check if user already exists in users table by email only
        // (firebase_uid and auth_provider columns do not exist in the DB schema)
        String queryUrl = SupabaseConfig.REST_URL + "users?email=eq." + email
                + "&select=user_id,name,photo_url&limit=1";
        Request checkReq = new Request.Builder()
                .url(queryUrl)
                .get()
                .headers(supabaseHeaders())
                .build();

        String checkRaw = "";
        try (okhttp3.Response response = client.newCall(checkReq).execute()) {
            if (response.body() != null) {
                checkRaw = response.body().string();
            }
        } catch (Exception e) {
            android.util.Log.e("SUPABASE_GOOGLE_SYNC", "Error executing check user request", e);
        }

        android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Check user raw: " + checkRaw);

        JsonArray users = null;
        if (!checkRaw.isEmpty()) {
            try {
                com.google.gson.JsonElement jsonElement = gson.fromJson(checkRaw, com.google.gson.JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonArray()) {
                    users = jsonElement.getAsJsonArray();
                } else {
                    android.util.Log.e("SUPABASE_GOOGLE_SYNC", "Expected JsonArray, but got: " + checkRaw);
                }
            } catch (Exception e) {
                android.util.Log.e("SUPABASE_GOOGLE_SYNC", "JSON parsing crash from Supabase response (check)", e);
            }
        }

        int localUserId;
        String displayName = (name == null || name.trim().isEmpty()) ? email.split("@")[0] : name;

        if (users != null && users.size() > 0) {
            // User already exists — use their existing user_id
            JsonObject existingUser = users.get(0).getAsJsonObject();
            localUserId = existingUser.has("user_id") && !existingUser.get("user_id").isJsonNull()
                    ? existingUser.get("user_id").getAsInt()
                    : -1;
            if (existingUser.has("name") && !existingUser.get("name").isJsonNull()) {
                displayName = existingUser.get("name").getAsString();
            }

            // Update photo_url if not set and Google provides one
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String existingPhoto = existingUser.has("photo_url") && !existingUser.get("photo_url").isJsonNull()
                        ? existingUser.get("photo_url").getAsString()
                        : "";
                if (existingPhoto.isEmpty()) {
                    JsonObject patchBody = new JsonObject();
                    patchBody.addProperty("photo_url", photoUrl);
                    Request patchReq = new Request.Builder()
                            .url(SupabaseConfig.REST_URL + "users?user_id=eq." + localUserId)
                            .patch(RequestBody.create(patchBody.toString(), JSON))
                            .headers(supabaseHeaders())
                            .build();

                    try (okhttp3.Response response = client.newCall(patchReq).execute()) {
                        if (response.body() != null) {
                            String patchRaw = response.body().string();
                            android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Patch photo_url raw: " + patchRaw);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SUPABASE_GOOGLE_SYNC", "Error executing patch request", e);
                    }
                }
            }

            result.addProperty("success", true);
            result.addProperty("user_id", localUserId);
            result.addProperty("name", displayName);
        } else {
            // User does not exist — insert new row with only valid columns
            localUserId = email.hashCode();
            if (localUserId < 0) {
                localUserId = -localUserId;
            }

            JsonObject body = new JsonObject();
            body.addProperty("user_id", localUserId);
            body.addProperty("name", displayName);
            body.addProperty("email", email);
            body.addProperty("age", 20);
            body.addProperty("gender", "Female");
            body.addProperty("concerns", "");
            body.addProperty("photo_url", (photoUrl != null) ? photoUrl : "");

            Request insertReq = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users")
                    .post(RequestBody.create(body.toString(), JSON))
                    .headers(supabaseHeaders())
                    .build();

            String insertRaw = "";
            try (okhttp3.Response response = client.newCall(insertReq).execute()) {
                if (response.body() != null) {
                    insertRaw = response.body().string();
                }
            } catch (Exception e) {
                android.util.Log.e("SUPABASE_GOOGLE_SYNC", "Error executing insert user request", e);
            }

            android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Insert user raw: " + insertRaw);

            result.addProperty("success", !insertRaw.isEmpty() && !insertRaw.contains("code"));
            result.addProperty("user_id", localUserId);
            result.addProperty("name", displayName);
            result.addProperty("message", insertRaw);
        }

        return result;
    }

    // ── Custom Backend OTP Verification for Reset Password ─────────────────
    public static JsonObject verifyResetOtp(Context ctx, String email, String token) throws IOException {
        return verifyPasswordOtp(email, token);
    }

    // ── getHabitStatus ── ApiService.getHabitStatus() ─────────────────────
    public static JsonObject getHabitStatus(Context ctx) throws IOException {
        int userId = new SessionManager(ctx).getUserId();
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId
                        + "&select=brushing_done,flossing_done,habit_date,streak_count,last_streak_date")
                .get()
                .headers(supabaseHeaders())
                .build();
        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("HABIT_STATUS", "Raw Supabase response: " + raw);
        JsonArray arr = gson.fromJson(raw, JsonArray.class);
        JsonObject result = new JsonObject();
        if (arr != null && arr.size() > 0) {
            result.addProperty("success", true);
            result.add("data", arr.get(0).getAsJsonObject());
        } else {
            result.addProperty("success", false);
        }
        return result;
    }

    // ── updateStreakInSupabase ── patches streak_count + last_streak_date ──
    public static JsonObject updateStreakInSupabase(
            Context ctx, int streakCount, String lastStreakDate) throws IOException {
        int userId = new SessionManager(ctx).getUserId();
        JsonObject body = new JsonObject();
        body.addProperty("streak_count", streakCount);
        body.addProperty("last_streak_date", lastStreakDate);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId)
                .patch(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();
        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("STREAK_UPDATE", "Patch raw: " + raw);
        JsonObject result = new JsonObject();
        result.addProperty("success", !raw.contains("code"));
        return result;
    }

    // ── updateHabitStatus ── ApiService.updateHabitStatus() ───────────────
    public static JsonObject updateHabitStatus(
            Context ctx,
            boolean brushingDone,
            boolean flossingDone,
            String habitDate) throws IOException {
        int userId = new SessionManager(ctx).getUserId();
        JsonObject body = new JsonObject();
        body.addProperty("brushing_done", brushingDone);
        body.addProperty("flossing_done", flossingDone);
        body.addProperty("habit_date", habitDate);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId)
                .patch(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();
        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("HABIT_UPDATE", "Patch raw: " + raw);
        JsonObject result = new JsonObject();
        result.addProperty("success", !raw.contains("code"));
        return result;
    }

    public static void updateDailyStreak(Context ctx) {
        int userId = new SessionManager(ctx).getUserId();
        if (userId <= 0) {
            android.util.Log.d("DAILY_STREAK", "Invalid or dummy user ID: " + userId);
            return;
        }

        try {
            // 1. Fetch current streak and last active date
            Request req = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId
                            + "&select=streak_count,last_active_date")
                    .get()
                    .headers(supabaseHeaders())
                    .build();

            String raw = client.newCall(req).execute().body().string();
            android.util.Log.d("DAILY_STREAK", "Fetch user streak raw response: " + raw);
            JsonArray arr = gson.fromJson(raw, JsonArray.class);
            if (arr == null || arr.size() == 0) {
                android.util.Log.e("DAILY_STREAK", "User not found in users table for ID: " + userId);
                return;
            }

            JsonObject userObj = arr.get(0).getAsJsonObject();
            int currentStreak = 0;
            if (userObj.has("streak_count") && !userObj.get("streak_count").isJsonNull()) {
                currentStreak = userObj.get("streak_count").getAsInt();
            }

            String lastActiveDateStr = "";
            if (userObj.has("last_active_date") && !userObj.get("last_active_date").isJsonNull()) {
                lastActiveDateStr = userObj.get("last_active_date").getAsString();
            }

            // 2. Compute today's date in local time "yyyy-MM-dd"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String todayStr = sdf.format(new Date());

            // 3. Compute new streak count based on rules
            int newStreak = currentStreak;
            if (lastActiveDateStr == null || lastActiveDateStr.trim().isEmpty()) {
                newStreak = 1;
            } else {
                int diff = getDaysDifference(lastActiveDateStr, todayStr);
                if (diff == 1) {
                    newStreak = currentStreak + 1;
                } else if (diff == 0) {
                    newStreak = currentStreak; // stays same
                } else {
                    newStreak = 1; // resets to 1 if older than yesterday
                }
            }

            // 4. Update the user row in Supabase
            JsonObject body = new JsonObject();
            body.addProperty("streak_count", newStreak);
            body.addProperty("last_active_date", todayStr);

            Request patchReq = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId)
                    .patch(RequestBody.create(body.toString(), JSON))
                    .headers(supabaseHeaders())
                    .build();

            String patchRaw = client.newCall(patchReq).execute().body().string();
            android.util.Log.d("DAILY_STREAK", "Patch user streak raw response: " + patchRaw);

            // 5. Add logs as requested
            android.util.Log.d("DAILY_STREAK", "CURRENT_USER_ID: " + userId);
            android.util.Log.d("DAILY_STREAK", "LAST_ACTIVE_DATE: " + lastActiveDateStr);
            android.util.Log.d("DAILY_STREAK", "TODAY_DATE: " + todayStr);
            android.util.Log.d("DAILY_STREAK", "UPDATED_STREAK_COUNT: " + newStreak);

        } catch (Exception e) {
            android.util.Log.e("DAILY_STREAK", "Error updating daily streak", e);
        }
    }

    private static int getDaysDifference(String dateStr1, String dateStr2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            java.util.Date date1 = sdf.parse(dateStr1);
            java.util.Date date2 = sdf.parse(dateStr2);
            if (date1 == null || date2 == null)
                return -1;

            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            cal1.setTime(date1);
            cal1.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal1.set(java.util.Calendar.MINUTE, 0);
            cal1.set(java.util.Calendar.SECOND, 0);
            cal1.set(java.util.Calendar.MILLISECOND, 0);

            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal2.setTime(date2);
            cal2.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal2.set(java.util.Calendar.MINUTE, 0);
            cal2.set(java.util.Calendar.SECOND, 0);
            cal2.set(java.util.Calendar.MILLISECOND, 0);

            long diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
            return (int) (diff / (24L * 60L * 60L * 1000L));
        } catch (Exception e) {
            return -1;
        }
    }

    public static class SupabaseLoggingInterceptor implements Interceptor {
        private static final String TAG = "SUPABASE_HTTP";

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String url = request.url().toString();
            String method = request.method();

            android.util.Log.d(TAG, "==================== SUPABASE REQUEST ====================");
            android.util.Log.d(TAG, "Request URL: " + url);
            android.util.Log.d(TAG, "HTTP Method: " + method);

            // Log Headers (excluding secrets)
            Headers headers = request.headers();
            for (int i = 0; i < headers.size(); i++) {
                String name = headers.name(i);
                String value = headers.value(i);
                if (name.equalsIgnoreCase("apikey") || name.equalsIgnoreCase("Authorization")) {
                    android.util.Log.d(TAG, "Header: " + name + ": [HIDDEN SECRET]");
                } else {
                    android.util.Log.d(TAG, "Header: " + name + ": " + value);
                }
            }

            // Log Request Body (if any)
            if (request.body() != null) {
                try {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    String requestBody = buffer.readUtf8();
                    // Mask passwords in request body if present
                    if (requestBody.contains("\"password\"")) {
                        requestBody = requestBody.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"",
                                "\"password\":\"[HIDDEN SECRET]\"");
                    }
                    android.util.Log.d(TAG, "Request Body: " + requestBody);
                } catch (Exception e) {
                    android.util.Log.d(TAG, "Request Body: [Could not read: " + e.getMessage() + "]");
                }
            }

            long startTime = System.nanoTime();
            Response response;
            try {
                response = chain.proceed(request);
            } catch (IOException e) {
                android.util.Log.e(TAG, "==================== SUPABASE EXCEPTION ====================");
                android.util.Log.e(TAG,
                        "ANDROID_SUPABASE_ERROR: Exception during call to: " + url + " - " + e.getMessage());
                android.util.Log.e(TAG, "Exception during call to: " + url, e);
                android.util.Log.e(TAG, "Exception Message: " + e.getMessage());
                // Log the stack trace elements
                for (StackTraceElement element : e.getStackTrace()) {
                    android.util.Log.e(TAG, "    at " + element.toString());
                }
                throw e;
            }

            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1e6;

            android.util.Log.d(TAG, "==================== SUPABASE RESPONSE ====================");
            android.util.Log.d(TAG, "Response Code: " + response.code());
            android.util.Log.d(TAG, "Response Message: " + response.message());
            android.util.Log.d(TAG, "Time taken: " + String.format("%.1f", durationMs) + "ms");

            if (!response.isSuccessful()) {
                android.util.Log.e(TAG, "ANDROID_SUPABASE_ERROR: HTTP " + response.code() + " " + response.message());
            }

            if (response.body() != null) {
                try {
                    BufferedSource source = response.body().source();
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.getBuffer();
                    String body = buffer.clone().readString(Charset.forName("UTF-8"));
                    android.util.Log.d(TAG, "Response Body: " + body);
                    if (body.contains("\"code\"") && (body.contains("\"message\"") || body.contains("\"hint\""))) {
                        android.util.Log.e(TAG, "ANDROID_SUPABASE_ERROR: " + body);
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Failed to read/log response body: " + e.getMessage());
                }
            }
            android.util.Log.d(TAG, "==========================================================");

            return response;
        }
    }
}
