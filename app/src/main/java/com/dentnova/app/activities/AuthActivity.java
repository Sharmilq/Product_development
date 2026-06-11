package com.dentnova.app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.dentnova.app.R;
import com.dentnova.app.services.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Firebase & Google Sign-In imports
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * AuthActivity — exact Java replica of auth_screen.dart
 *
 * Modes: LOGIN (isLogin=true) / REGISTER (isLogin=false)
 * Toggle between modes by tapping tvToggleAction (same as Flutter setState(() => _isLogin = !_isLogin))
 *
 * Validation mirrors _handleAuth() in auth_screen.dart:
 *   - Empty name (register)
 *   - Invalid email regex
 *   - Password < 6 chars
 *   - Passwords don't match (register)
 *
 * On success:
 *   - Login  → HomeActivity (replaces MaterialPageRoute → HomeScreen)
 *   - Register → ProfileSetupActivity (replaces MaterialPageRoute → ProfileSetupScreen)
 */
public class AuthActivity extends AppCompatActivity {

    private boolean isLogin = true;

    // Views
    private View layoutName, layoutConfirmPassword, layoutSocialLogin;
    private TextView tvTitle, tvSubtitle, tvForgotPassword;
    private TextView tvTogglePrompt, tvToggleAction;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnAuth;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In — requestProfile() forces fresh account chooser
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Google Sign-In result launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Re-enable the Google button regardless of outcome
                    View btnG = findViewById(R.id.btnGoogle);
                    if (btnG != null) btnG.setEnabled(true);

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                String idToken = account.getIdToken();
                                if (idToken == null || idToken.isEmpty()) {
                                    android.util.Log.e("GOOGLE_AUTH_FAILED", "Google ID Token is null or empty");
                                    Toast.makeText(this, "Google ID Token is missing. Authentication aborted.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                android.util.Log.d("GOOGLE_AUTH_SUCCESS", "GOOGLE_AUTH_SUCCESS");
                                android.util.Log.d("GOOGLE_AUTH_SUCCESS",
                                        "Account selected: " + account.getEmail());
                                firebaseAuthWithGoogle(idToken);
                            } else {
                                android.util.Log.e("GOOGLE_AUTH_FAILED",
                                        "Account chooser returned null account.");
                                Toast.makeText(this,
                                        "Google Sign-In failed: no account returned.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (ApiException e) {
                            android.util.Log.e("GOOGLE_AUTH_FAILED",
                                    "ApiException code=" + e.getStatusCode() + " msg=" + e.getMessage(), e);
                            Toast.makeText(this,
                                    "Google Sign-In failed (code " + e.getStatusCode() + ").",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // User cancelled the chooser — stay on AuthActivity
                        android.util.Log.w("GOOGLE_AUTH_FAILED",
                                "Google chooser cancelled or closed. ResultCode=" + result.getResultCode());
                        Toast.makeText(this,
                                "Google Sign-In cancelled.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Check if launched in register mode
        if (getIntent().getBooleanExtra("isRegister", false)) {
            isLogin = false;
        }

        initViews();
        applyMode();
        setupListeners();
    }

    private void initViews() {
        layoutName             = findViewById(R.id.layoutName);
        layoutConfirmPassword  = findViewById(R.id.layoutConfirmPassword);
        layoutSocialLogin      = findViewById(R.id.layoutSocialLogin);
        tvTitle                = findViewById(R.id.tvTitle);
        tvSubtitle             = findViewById(R.id.tvSubtitle);
        tvForgotPassword       = findViewById(R.id.tvForgotPassword);
        tvTogglePrompt         = findViewById(R.id.tvTogglePrompt);
        tvToggleAction         = findViewById(R.id.tvToggleAction);
        etName                 = findViewById(R.id.etName);
        etEmail                = findViewById(R.id.etEmail);
        etPassword             = findViewById(R.id.etPassword);
        etConfirmPassword      = findViewById(R.id.etConfirmPassword);
        btnAuth                = findViewById(R.id.btnAuth);
    }

    /** Mirrors Flutter setState(() => _isLogin = ...) — shows/hides the correct fields */
    private void applyMode() {
        if (isLogin) {
            tvTitle.setText("Welcome back");
            tvSubtitle.setText("Sign in to continue your dental journey");
            btnAuth.setText("Sign In");
            tvTogglePrompt.setText("New here? ");
            tvToggleAction.setText("Create account");
            layoutName.setVisibility(View.GONE);
            layoutConfirmPassword.setVisibility(View.GONE);
            layoutSocialLogin.setVisibility(View.VISIBLE);
            tvForgotPassword.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Create account");
            tvSubtitle.setText("Join DentNova and start caring for your smile.");
            btnAuth.setText("Create account");
            tvTogglePrompt.setText("Already have an account? ");
            tvToggleAction.setText("Sign in");
            layoutName.setVisibility(View.VISIBLE);
            layoutConfirmPassword.setVisibility(View.VISIBLE);
            layoutSocialLogin.setVisibility(View.GONE);
            tvForgotPassword.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnAuth.setOnClickListener(v -> handleAuth());

        // Toggle mode (replicates GestureDetector on tvToggleAction)
        tvToggleAction.setOnClickListener(v -> {
            isLogin = !isLogin;
            applyMode();
        });
        tvForgotPassword.setOnClickListener(v -> {
            android.util.Log.d("AuthActivity", "FORGOT_PASSWORD_CLICKED");

            String email =
                    etEmail.getText() != null
                            ? etEmail.getText().toString().trim()
                            : "";

            if (email.isEmpty()) {
                Toast.makeText(
                        this,
                        "Enter your email first",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                        this,
                        "Please enter a valid email address",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("Sending verification code (OTP)...");
            progress.setCancelable(false);
            progress.show();

            executor.execute(() -> {
                try {
                    JsonObject result = ApiService.forgotPassword(email);
                    runOnUiThread(() -> {
                        progress.dismiss();
                        boolean success = result.has("success") && result.get("success").getAsBoolean();
                        if (success) {
                            Toast.makeText(
                                    this,
                                    "OTP sent successfully.",
                                    Toast.LENGTH_LONG
                            ).show();
                            
                            // Navigate to OTP verification screen
                            Intent intent = new Intent(this, OtpVerificationActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        } else {
                            String msg = result.has("message") ? result.get("message").getAsString() : "Email is not registered.";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("AuthActivity", "Error requesting password reset", e);
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(
                                this,
                                "Failed to connect: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            });
        });
        // Google sign-in integration
        View btnGoogle = findViewById(R.id.btnGoogle);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                android.util.Log.d("GOOGLE_BUTTON_CLICKED",
                        "Google Sign-In button tapped. Starting full session clear...");

                // Step 1 — disable button to prevent double-tap
                btnGoogle.setEnabled(false);

                // Step 2 — sign out of Firebase (synchronous)
                android.util.Log.d("GOOGLE_SIGN_OUT_STARTED", "Signing out of Firebase Auth...");
                mAuth.signOut();

                // Step 3 — sign out of Google, then revoke access to force chooser
                mGoogleSignInClient.signOut().addOnCompleteListener(this, signOutTask -> {
                    android.util.Log.d("GOOGLE_SIGN_OUT_STARTED",
                            "Google signOut complete. Revoking access to force account chooser...");

                    mGoogleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                        android.util.Log.d("GOOGLE_REVOKE_COMPLETED",
                                "Google revokeAccess complete. Launching account chooser now.");

                        // Step 4 — launch account chooser ONLY after revoke completes
                        android.util.Log.d("GOOGLE_CHOOSER_LAUNCHED",
                                "getSignInIntent() called — account chooser should appear.");
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        googleSignInLauncher.launch(signInIntent);
                    });
                });
            });
        }
    }

    /**
     * Exact replica of _handleAuth() from auth_screen.dart
     * Validates → shows ProgressDialog → calls ApiService → navigates
     */
    private void handleAuth() {
        String name     = etName    != null && etName.getText()            != null ? etName.getText().toString().trim()    : "";
        String email    = etEmail   != null && etEmail.getText()           != null ? etEmail.getText().toString().trim()   : "";
        String password = etPassword!= null && etPassword.getText()        != null ? etPassword.getText().toString()       : "";
        String confirm  = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        // Name validation (register only)
        if (!isLogin && name.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Email validation
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isLogin && !isStrongPassword(password)) {
            etPassword.setError("Weak password");
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Weak Password")
                    .setMessage("Your password must contain:\n\n• At least 8 characters\n• One uppercase letter\n• One lowercase letter\n• One number\n• One special character")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Confirm password (register only)
        if (!isLogin && !password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading (replaces Flutter showDialog CircularProgressIndicator)
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Please wait…");
        progress.setCancelable(false);
        progress.show();

        final String finalName = name, finalEmail = email, finalPassword = password;
        final boolean wasLogin = isLogin;

        executor.execute(() -> {
            try {
                JsonObject result;
                if (wasLogin) {
                    result = ApiService.login(this, finalEmail, finalPassword);
                } else {
                    result = ApiService.register(this, finalName, finalEmail, finalPassword);
                }

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        // Navigate: login → Home, register → ProfileSetup
                        if (wasLogin) {
                            startActivity(new Intent(this, HomeActivity.class));
                        } else {
                            startActivity(new Intent(this, ProfileSetupActivity.class));
                        }
                        finish();
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Something went wrong";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("AuthActivity", "Error during login/register API call", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(
                            this,
                            "Login failed. Please check your email, password, or email verification.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        android.util.Log.d("GOOGLE_AUTH_SUCCESS",
                "Starting Firebase Auth with Google credential...");
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Authenticating with Google...");
        progress.setCancelable(false);
        progress.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            android.util.Log.d("FIREBASE_USER_NULL_CHECK_PASSED", "FIREBASE_USER_NULL_CHECK_PASSED");
                            syncGoogleUserWithSupabase(user, progress);
                        } else {
                            progress.dismiss();
                            android.util.Log.e("GOOGLE_AUTH_FAILED",
                                    "Firebase signInWithCredential succeeded but getCurrentUser() is null.");
                            Toast.makeText(this,
                                    "Authentication error: user is null. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progress.dismiss();
                        Exception e = task.getException();
                        android.util.Log.e("GOOGLE_AUTH_FAILED",
                                "Firebase Auth failed: " + (e != null ? e.getMessage() : "unknown"), e);
                        Toast.makeText(this,
                                "Google authentication failed. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void syncGoogleUserWithSupabase(FirebaseUser user, ProgressDialog progress) {
        progress.setMessage("Syncing profile...");

        // Null safety checks before using firebaseUser values
        String email = user.getEmail();
        String uid = user.getUid();
        String displayName = user.getDisplayName();
        android.net.Uri photoUri = user.getPhotoUrl();
        String photoUrl = photoUri != null ? photoUri.toString() : "";

        if (uid == null || uid.isEmpty()) {
            progress.dismiss();
            android.util.Log.e("GOOGLE_AUTH_FAILED", "Firebase UID is null or empty.");
            Toast.makeText(this, "Firebase UID is missing. Google Sign-In aborted.", Toast.LENGTH_LONG).show();
            return;
        }
        if (email == null || email.isEmpty()) {
            progress.dismiss();
            android.util.Log.e("GOOGLE_AUTH_FAILED", "Firebase email is null or empty.");
            Toast.makeText(this, "Firebase email is missing. Google Sign-In aborted.", Toast.LENGTH_LONG).show();
            return;
        }

        // If optional fields are null, handle gracefully
        if (displayName == null) {
            displayName = "";
        }

        android.util.Log.d("SUPABASE_SYNC_STARTED", "SUPABASE_SYNC_STARTED");

        final String finalEmail = email;
        final String finalUid = uid;
        final String finalDisplayName = displayName;
        final String finalPhotoUrl = photoUrl;

        executor.execute(() -> {
            try {
                JsonObject result = ApiService.syncGoogleUserWithSupabase(
                        this,
                        finalDisplayName,
                        finalEmail,
                        finalUid,
                        finalPhotoUrl
                );

                android.util.Log.d("SUPABASE_SYNC_RESPONSE", "SUPABASE_SYNC_RESPONSE");

                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
                        int localUserId = result.has("user_id") && !result.get("user_id").isJsonNull() ? result.get("user_id").getAsInt() : -1;
                        String nameStr = result.has("name") && !result.get("name").isJsonNull() ? result.get("name").getAsString() : finalDisplayName;

                        android.util.Log.d("SESSION_SAVE_STARTED", "SESSION_SAVE_STARTED");

                        new com.dentnova.app.utils.SessionManager(this).saveSession(
                                localUserId,
                                finalUid,
                                nameStr,
                                finalEmail
                        );

                        android.util.Log.d("SESSION_SAVE_SUCCESS", "SESSION_SAVE_SUCCESS");

                        Toast.makeText(this, "Signed in with Google! 👋", Toast.LENGTH_SHORT).show();

                        android.util.Log.d("HOME_NAVIGATION_STARTED", "HOME_NAVIGATION_STARTED");

                        Intent home = new Intent(AuthActivity.this, HomeActivity.class);
                        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(home);

                        android.util.Log.d("HOME_NAVIGATION_SUCCESS", "HOME_NAVIGATION_SUCCESS");
                        finish();
                    } else {
                        String msg = (result != null && result.has("message"))
                                ? result.get("message").getAsString()
                                : "Failed to sync profile with Supabase.";
                        android.util.Log.e("SUPABASE_SYNC_FAILED", "Supabase sync FAILED: " + msg);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("SUPABASE_SYNC_FAILED", "Exception during Supabase sync", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this,
                            "Network error. Could not connect to Supabase.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
