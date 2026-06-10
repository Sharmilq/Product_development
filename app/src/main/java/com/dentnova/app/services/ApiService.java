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

/**
 * ApiService.java — exact Java equivalent of api_service.dart
 * Uses OkHttp (replaces dart:http) + Gson (replaces dart jsonDecode/jsonEncode)
 *
 * All methods are synchronous — call from background thread (AsyncTask / ExecutorService)
 * Equivalent Flutter method listed in each Javadoc comment.
 */
public class ApiService {

    /**
     * Replace with your server IP — same as baseUrl in api_service.dart
     */
    //public static final String BASE_URL = "http://10.46.55.51/dentnova";
    public static final String ML_BASE_URL = "https://dentnova-ml.onrender.com";
    public static final String SUPABASE_AUTH_URL =
            SupabaseConfig.SUPABASE_URL + "/auth/v1";

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

    //supabase//
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
                if (localUserId < 0) localUserId = -localUserId;

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
    // ── Forgot Password → Backend OTP Flow ─────────────────────────────────
    // Calls the Render backend which emails a 6-digit OTP (hashed server-side).
    // NEVER calls Supabase /auth/v1/recover or exposes service_role key.
    public static JsonObject forgotPassword(String email) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        Request req = new Request.Builder()
                .url(SupabaseConfig.BACKEND_URL + "/auth/request-password-otp")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("OTP_REQUEST", "request-password-otp response: " + raw);

        JsonObject data = gson.fromJson(raw, JsonObject.class);
        JsonObject result = new JsonObject();
        boolean success = data.has("success") && data.get("success").getAsBoolean();
        result.addProperty("success", success);
        result.addProperty("message", data.has("message") ? data.get("message").getAsString() : "");
        return result;
    }

    // ── Verify OTP (Backend) ── verifyPasswordOtp() ──────────────────────────
    public static JsonObject verifyPasswordOtp(String email, String otp) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("otp", otp);

        Request req = new Request.Builder()
                .url(SupabaseConfig.BACKEND_URL + "/auth/verify-password-otp")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("OTP_VERIFY", "verify-password-otp response: " + raw);

        JsonObject data = gson.fromJson(raw, JsonObject.class);
        JsonObject result = new JsonObject();
        boolean success = data.has("success") && data.get("success").getAsBoolean();
        result.addProperty("success", success);
        result.addProperty("message", data.has("message") ? data.get("message").getAsString() : "");
        return result;
    }

    // ── Reset Password with OTP (Backend) ── resetPasswordWithOtp() ──────────
    public static JsonObject resetPasswordWithOtp(String email, String otp, String newPassword)
            throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("otp", otp);
        body.addProperty("newPassword", newPassword);

        Request req = new Request.Builder()
                .url(SupabaseConfig.BACKEND_URL + "/auth/reset-password-with-otp")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();
        android.util.Log.d("OTP_RESET", "reset-password-with-otp response: " + raw);

        JsonObject data = gson.fromJson(raw, JsonObject.class);
        JsonObject result = new JsonObject();
        boolean success = data.has("success") && data.get("success").getAsBoolean();
        result.addProperty("success", success);
        result.addProperty("message", data.has("message") ? data.get("message").getAsString() : "");
        return result;
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
            // Do NOT use supabase_uuid.hashCode() — that differs from email.hashCode() used at registration.
            int localUserId = lookupUserIdByEmail(userEmail);
            android.util.Log.d("SUPABASE_LOGIN", "Looked up user_id for " + userEmail + ": " + localUserId);

            new SessionManager(ctx).saveSession(
                    localUserId,
                    token,
                    userName,
                    userEmail
            );

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
     * Returns email.hashCode() (with sign correction) as fallback if user not found.
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
        if (fallback < 0) fallback = -fallback;
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

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId + "&select=*")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req).execute().body().string();

        android.util.Log.d("SUPABASE_PROFILE", raw);

        com.google.gson.JsonArray arr =
                gson.fromJson(raw, com.google.gson.JsonArray.class);

        JsonObject result = new JsonObject();

        if (arr.size() > 0) {

            result.addProperty("success", true);
            result.add("profile", arr.get(0).getAsJsonObject());

        } else {

            result.addProperty("success", false);
        }

        return result;
    }

    // ── updateProfile ── ApiService.updateProfile() ───────────────────────
    public static JsonObject updateProfile(
            Context ctx,
            String name,
            int age,
            String gender,
            String concerns,
            String photoBase64
    ) throws IOException {

        JsonObject result = new JsonObject();

        try {

            int userId = new SessionManager(ctx).getUserId();
            String email =
                    new SessionManager(ctx)
                            .getUserEmail();

            JsonObject body = new JsonObject();

            body.addProperty("user_id", userId);
            body.addProperty("name", name);
            body.addProperty("email", email);
            body.addProperty("age", age);
            body.addProperty("gender", gender);
            body.addProperty("concerns", concerns);
            body.addProperty("photo_url", photoBase64);

            Request req = new Request.Builder()
                    .url(SupabaseConfig.REST_URL + "users?user_id=eq." + userId)
                    .patch(RequestBody.create(body.toString(), JSON))
                    .headers(supabaseHeaders())
                    .build();
            String raw =
                    client.newCall(req)
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
                    new SessionManager(ctx).getUserId()
            );

            notif.addProperty(
                    "title",
                    "Assessment Completed 🦷"
            );

            notif.addProperty(
                    "body",
                    "Your oral health score is "
                            + score +
                            " (" + label + ")"
            );

            Request notifReq =
                    new Request.Builder()
                            .url(SupabaseConfig.REST_URL + "notifications")
                            .post(
                                    RequestBody.create(
                                            notif.toString(),
                                            JSON
                                    )
                            )
                            .headers(supabaseHeaders())
                            .build();

            client.newCall(notifReq)
                    .enqueue(new Callback() {

                        @Override
                        public void onFailure(
                                Call call,
                                IOException e
                        ) {
                        }

                        @Override
                        public void onResponse(
                                Call call,
                                Response response
                        ) throws IOException {

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

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "assessments?select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req).execute().body().string();

        android.util.Log.d("SUPABASE_GET_ASSESSMENTS", raw);

        com.google.gson.JsonArray array =
                gson.fromJson(raw, com.google.gson.JsonArray.class);

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.add("assessments", array);

        return result;
    }

    // ── getReminders ── ApiService.getReminders() ─────────────────────────
    public static JsonObject getReminders(Context ctx) throws IOException {

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "reminders?select=*&order=created_at.desc")
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw = client.newCall(req).execute().body().string();

        com.google.gson.JsonArray array =
                gson.fromJson(raw, com.google.gson.JsonArray.class);

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
        result.addProperty("success", true);
        result.addProperty("raw", raw);

        return result;
    }

    public static JsonObject predictAssessment(JsonObject data)
            throws Exception {

        okhttp3.MediaType JSON =
                okhttp3.MediaType.parse("application/json");

        okhttp3.RequestBody body =
                okhttp3.RequestBody.create(
                        data.toString(),
                        JSON
                );

        okhttp3.Request request =
                new okhttp3.Request.Builder()
                        .url(ML_BASE_URL + "/predict")
                        .post(body)
                        .build();

        okhttp3.Response response =
                client.newCall(request).execute();

        String responseBody =
                response.body().string();

        return gson.fromJson(
                responseBody,
                JsonObject.class
        );
    }

    // ── toggleReminder ── ApiService.toggleReminder() ─────────────────────
    public static JsonObject toggleReminder(
            Context ctx,
            int id,
            boolean enabled
    ) throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("enabled", enabled);

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "reminders?id=eq." + id
                )
                .patch(
                        RequestBody.create(
                                body.toString(),
                                JSON
                        )
                )
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        JsonObject result =
                new JsonObject();

        result.addProperty("success", true);

        return result;
    }
    // ── deleteReminder ── ApiService.deleteReminder() ─────────────────────
    public static JsonObject deleteReminder(
            Context ctx,
            int id
    ) throws IOException {

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "reminders?id=eq." + id
                )
                .delete()
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        JsonObject result =
                new JsonObject();

        result.addProperty("success", true);

        return result;
    }
    //feedback
    public static JsonObject sendFeedback(
            Context ctx,
            String message
    ) throws IOException {

        JsonObject body = new JsonObject();

        body.addProperty(
                "user_id",
                new SessionManager(ctx).getUserId()
        );

        body.addProperty(
                "message",
                message
        );

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "feedback")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        JsonObject result =
                new JsonObject();

        result.addProperty(
                "success",
                !raw.contains("code")
        );

        return result;
    }
    // ── saveVisitReminder ── ApiService.saveVisitReminder() ───────────────
    public static JsonObject saveVisitReminder(
            Context ctx,
            String date,
            String time,
            String note
    ) throws IOException {

        JsonObject body = new JsonObject();

        body.addProperty(
                "user_id",
                new SessionManager(ctx).getUserId()
        );

        body.addProperty("visit_date", date);
        body.addProperty("visit_time", time);
        body.addProperty("note", note);

        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "visits")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        android.util.Log.d("SUPABASE_VISIT_SAVE", raw);

        JsonObject result = new JsonObject();

        result.addProperty(
                "success",
                !raw.contains("code")
        );

        result.addProperty("message", raw);

        return result;
    }

    // ── getVisitReminder ── ApiService.getVisitReminder() ────────────────
    public static JsonObject getVisitReminder(
            Context ctx
    ) throws IOException {

        int userId =
                new SessionManager(ctx)
                        .getUserId();

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "visits?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc"
                )
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        android.util.Log.d(
                "SUPABASE_VISITS",
                raw
        );

        JsonArray array =
                gson.fromJson(
                        raw,
                        JsonArray.class
                );

        JsonObject result =
                new JsonObject();

        result.addProperty("success", true);
        result.add("visits", array);

        return result;
    }

    // ── getNotifications ── ApiService.getNotifications() ────────────────
    public static JsonObject getNotifications(
            Context ctx
    ) throws IOException {

        int userId =
                new SessionManager(ctx)
                        .getUserId();

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "notifications?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc"
                )
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        android.util.Log.d(
                "SUPABASE_NOTIFICATIONS",
                raw
        );

        JsonArray array =
                gson.fromJson(
                        raw,
                        JsonArray.class
                );

        JsonObject result =
                new JsonObject();

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
            String resultLabel
    ) throws IOException {

        JsonObject body = new JsonObject();

        body.addProperty(
                "user_id",
                new SessionManager(ctx).getUserId()
        );

        body.addProperty(
                "plaque_score",
                plaqueScore
        );

        body.addProperty(
                "gum_score",
                gumScore
        );

        body.addProperty(
                "cleanliness_score",
                cleanlinessScore
        );

        body.addProperty(
                "result_label",
                resultLabel
        );
        byte[] imageBytes =
                java.nio.file.Files.readAllBytes(
                        imageFile.toPath()
                );

        String imageBase64 =
                android.util.Base64.encodeToString(
                        imageBytes,
                        android.util.Base64.NO_WRAP
                );

        body.addProperty("image_base64", imageBase64);
        Request req = new Request.Builder()
                .url(SupabaseConfig.REST_URL + "tooth_scans")
                .post(RequestBody.create(body.toString(), JSON))
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        android.util.Log.d(
                "SUPABASE_SCAN_SAVE",
                raw
        );

        JsonObject result =
                new JsonObject();

        result.addProperty(
                "success",
                !raw.contains("code")
        );

        result.addProperty(
                "message",
                raw
        );

        return result;
    }
//predict tooth scan
public static JsonObject predictToothScan(
        Context ctx,
        File imageFile
) throws IOException {

    RequestBody reqBody =
            new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "image",
                            imageFile.getName(),
                            RequestBody.create(
                                    imageFile,
                                    MediaType.parse("image/*")
                            )
                    )
                    .build();

    Request req =
            new Request.Builder()
                    .url(ML_BASE_URL + "/predict-tooth")
                    .post(reqBody)
                    .build();

    Response response =
            client.newCall(req)
                    .execute();

    String raw =
            response.body()
                    .string();

    android.util.Log.d(
            "TOOTH_AI_RAW",
            raw
    );

    return gson.fromJson(
            raw,
            JsonObject.class
    );
}
    // ── getToothScans ── ApiService.getToothScans() ───────────────────────
    public static JsonObject getToothScans(
            Context ctx
    ) throws IOException {

        int userId =
                new SessionManager(ctx)
                        .getUserId();

        Request req = new Request.Builder()
                .url(
                        SupabaseConfig.REST_URL +
                                "tooth_scans?user_id=eq."
                                + userId +
                                "&select=*&order=created_at.desc"
                )
                .get()
                .headers(supabaseHeaders())
                .build();

        String raw =
                client.newCall(req)
                        .execute()
                        .body()
                        .string();

        android.util.Log.d(
                "SUPABASE_SCAN_HISTORY",
                raw
        );

        JsonArray array =
                gson.fromJson(
                        raw,
                        JsonArray.class
                );

        JsonObject result =
                new JsonObject();

        result.addProperty("success", true);
        result.add("scans", array);

        return result;

    }
    //seetings
    public static JsonObject changePassword(Context ctx, String newPassword)
            throws IOException {

        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        Request req = new Request.Builder()
                .url(SUPABASE_AUTH_URL + "/user")
                .put(RequestBody.create(body.toString(), JSON))
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + new SessionManager(ctx).getToken())
                .addHeader("Content-Type", "application/json")
                .build();

        String raw = client.newCall(req).execute().body().string();

        JsonObject result = new JsonObject();
        result.addProperty("success", raw.contains("\"id\""));
        result.addProperty("message", raw);

        return result;
    }

    // ── Sync Google User with Supabase (links existing or inserts new profile) ──
    public static JsonObject syncGoogleUserWithSupabase(Context ctx, String name, String email, String firebaseUid, String photoUrl)
            throws IOException {
        JsonObject result = new JsonObject();

        // Check if user already exists in users table by email only
        // (firebase_uid and auth_provider columns do not exist in the DB schema)
        String queryUrl = SupabaseConfig.REST_URL + "users?email=eq." + email + "&select=user_id,name,photo_url&limit=1";
        Request checkReq = new Request.Builder()
                .url(queryUrl)
                .get()
                .headers(supabaseHeaders())
                .build();

        String checkRaw = client.newCall(checkReq).execute().body().string();
        android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Check user raw: " + checkRaw);
        JsonArray users = gson.fromJson(checkRaw, JsonArray.class);

        int localUserId;
        String displayName = (name == null || name.trim().isEmpty()) ? email.split("@")[0] : name;

        if (users != null && users.size() > 0) {
            // User already exists — use their existing user_id
            JsonObject existingUser = users.get(0).getAsJsonObject();
            localUserId = existingUser.get("user_id").getAsInt();
            if (existingUser.has("name") && !existingUser.get("name").isJsonNull()) {
                displayName = existingUser.get("name").getAsString();
            }

            // Update photo_url if not set and Google provides one
            if (photoUrl != null && !photoUrl.isEmpty()) {
                String existingPhoto = existingUser.has("photo_url") && !existingUser.get("photo_url").isJsonNull() ? existingUser.get("photo_url").getAsString() : "";
                if (existingPhoto.isEmpty()) {
                    JsonObject patchBody = new JsonObject();
                    patchBody.addProperty("photo_url", photoUrl);
                    Request patchReq = new Request.Builder()
                            .url(SupabaseConfig.REST_URL + "users?user_id=eq." + localUserId)
                            .patch(RequestBody.create(patchBody.toString(), JSON))
                            .headers(supabaseHeaders())
                            .build();
                    String patchRaw = client.newCall(patchReq).execute().body().string();
                    android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Patch photo_url raw: " + patchRaw);
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

            String insertRaw = client.newCall(insertReq).execute().body().string();
            android.util.Log.d("SUPABASE_GOOGLE_SYNC", "Insert user raw: " + insertRaw);

            result.addProperty("success", !insertRaw.contains("code"));
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
                        requestBody = requestBody.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"[HIDDEN SECRET]\"");
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

            if (response.body() != null) {
                try {
                    BufferedSource source = response.body().source();
                    source.request(Long.MAX_VALUE); // Buffer the entire body.
                    Buffer buffer = source.getBuffer();
                    String body = buffer.clone().readString(Charset.forName("UTF-8"));
                    android.util.Log.d(TAG, "Response Body: " + body);
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Failed to read/log response body: " + e.getMessage());
                }
            }
            android.util.Log.d(TAG, "==========================================================");

            return response;
        }
    }
}
