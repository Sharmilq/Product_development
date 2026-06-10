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

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Google Sign-In result launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
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

            ProgressDialog progress =
                    new ProgressDialog(this);

            progress.setMessage("Sending verification code (OTP)...");
            progress.setCancelable(false);
            progress.show();

            executor.execute(() -> {
                try {
                    JsonObject result = ApiService.forgotPassword(email);
                    runOnUiThread(() -> {
                        progress.dismiss();
                        // Supabase returns {} on success — treat any non-error as OTP sent
                        boolean sent = !result.has("error") && !result.has("error_code");
                        if (sent) {
                            Toast.makeText(
                                    this,
                                    R.string.otp_sent_success,
                                    Toast.LENGTH_LONG
                            ).show();

                            // Navigate to OTP verification screen
                            Intent intent = new Intent(this, OtpVerificationActivity.class);
                            intent.putExtra("email", email);
                            startActivity(intent);
                        } else {
                            String msg = result.has("message") ? result.get("message").getAsString() : "Email not registered in Supabase.";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("AuthActivity", "Error requesting password reset OTP", e);
                    runOnUiThread(() -> {
                        progress.dismiss();
                        Toast.makeText(
                                this,
                                "Failed to send reset link. Please check connection.",
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
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
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

        // Password length
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
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
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Authenticating with Firebase...");
        progress.setCancelable(false);
        progress.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            syncGoogleUserWithSupabase(user, progress);
                        } else {
                            progress.dismiss();
                            Toast.makeText(this, "Authentication failed. User is null.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progress.dismiss();
                        Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void syncGoogleUserWithSupabase(FirebaseUser user, ProgressDialog progress) {
        progress.setMessage("Syncing profile with Supabase...");

        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        executor.execute(() -> {
            try {
                JsonObject result = ApiService.syncGoogleUserWithSupabase(
                        this,
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getUid(),
                        photoUrl
                );
                runOnUiThread(() -> {
                    progress.dismiss();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        int localUserId = result.get("user_id").getAsInt();
                        String displayName = result.get("name").getAsString();

                        // Save session — use Firebase UID as token for Google sign-in users
                        new com.dentnova.app.utils.SessionManager(this).saveSession(
                                localUserId,
                                user.getUid(),
                                displayName,
                                user.getEmail()
                        );

                        Toast.makeText(this, "Signed in with Google! 👋", Toast.LENGTH_SHORT).show();

                        // Navigate to Home
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        String msg = result.has("message") ? result.get("message").getAsString() : "Failed to sync profile with Supabase.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("AuthActivity", "Error syncing Google user with Supabase", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to connect to Supabase. Check your network.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
