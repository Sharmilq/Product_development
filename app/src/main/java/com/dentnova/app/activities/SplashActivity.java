package com.dentnova.app.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.dentnova.app.R;
import com.dentnova.app.utils.SessionManager;

/**
 * SplashActivity — exact Java equivalent of splash_screen.dart
 *
 * Animations:
 *  - Logo pulse scale 1.0→1.08 (2s repeat)        ← Flutter _pulseCtrl
 *  - Float: 5 icons bob up/down staggered          ← Flutter _floatCtrl
 *  - Fade+Slide: title+tagline appear after 800ms  ← Flutter _fadeCtrl + SlideTransition
 *  - Sparkle: title icon rotates continuously      ← Flutter _sparkleCtrl
 *  - Dots: 3 dots bounce with staggered delay      ← Flutter _dotCtrl
 *
 * Navigation after 3000ms  ← Flutter _checkAppState()
 */
public class SplashActivity extends AppCompatActivity {
    private boolean hasNavigated = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startAnimations();
        scheduleNavigation();
    }

    private void startAnimations() {
        View flaLogo = findViewById(R.id.flaLogo);
        View dot1    = findViewById(R.id.dot1);
        View dot2    = findViewById(R.id.dot2);
        View dot3    = findViewById(R.id.dot3);
        View tvTitle = findViewById(R.id.llTitleRow);
        View tvTag   = findViewById(R.id.tvTagline);
        View spark   = findViewById(R.id.ivTitleSparkle);

        // ── Logo pulse (replaces _pulseCtrl scale 1.0→1.08) ──────────
        animatePulse(flaLogo);

        // ── Float decorative icons ────────────────────────────────────
        floatIcon(R.id.ivToothDecor,   0,    -6f, 6f,   4000);
        floatIcon(R.id.ivBrushDecor,   500,   4f, -5f,  4000);
        floatIcon(R.id.ivLeafDecor,    1000, -5f, 5f,   4000);
        floatIcon(R.id.ivSparkleDecor, 1500,  3f, -4f,  4000);
        floatIcon(R.id.ivCursorDecor,  800,   2f, -3.5f,4000);

        // ── Sparkle continuous rotation ───────────────────────────────
        ObjectAnimator rot = ObjectAnimator.ofFloat(spark, "rotation", 0f, 360f);
        rot.setDuration(3000);
        rot.setRepeatCount(ValueAnimator.INFINITE);
        rot.setInterpolator(new LinearInterpolator());
        rot.start();

        // ── Bouncing loading dots ─────────────────────────────────────
        bounceDot(dot1, 0);
        bounceDot(dot2, 333);
        bounceDot(dot3, 666);

        // ── Fade + slide title after 800ms (replaces _fadeCtrl) ──────
        float dp80 = 80f * getResources().getDisplayMetrics().density;
        tvTitle.setAlpha(0f); tvTitle.setTranslationY(dp80);
        tvTag.setAlpha(0f);   tvTag.setTranslationY(dp80);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            tvTitle.animate().alpha(1f).translationY(0f)
                .setDuration(1200).setInterpolator(new AccelerateDecelerateInterpolator()).start();
            tvTag.animate().alpha(1f).translationY(0f)
                .setDuration(1200).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        }, 800);
    }

    private void animatePulse(View v) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 1.08f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 1.08f);
        for (ObjectAnimator a : new ObjectAnimator[]{sx, sy}) {
            a.setDuration(2000);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.start();
        }
    }

    private void floatIcon(int resId, long delay, float from, float to, long dur) {
        View v = findViewById(resId);
        if (v == null) return;
        float dp = getResources().getDisplayMetrics().density;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", from * dp, to * dp);
        a.setDuration(dur);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
    }

    private void bounceDot(View v, long delay) {
        float dp = getResources().getDisplayMetrics().density;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", 0f, -4f * dp);
        a.setDuration(600);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
    }

    /**
     * Replicates _checkAppState() from splash_screen.dart:
     * isLoggedIn → HomeActivity
     * !hasSeenOnboarding → OnboardingActivity
     * else → AuthActivity
     */
    private void scheduleNavigation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            SharedPreferences prefs = getSharedPreferences("dentnova_prefs", MODE_PRIVATE);
            boolean seenOnboarding = prefs.getBoolean("has_seen_onboarding", false);

            Class<?> dest;
            if (session.isLoggedIn()) {
                dest = HomeActivity.class;
            } else if (!seenOnboarding) {
                dest = OnboardingActivity.class;
            } else {
                dest = AuthActivity.class;
            }
            if (hasNavigated) return;
            hasNavigated = true;

            Intent intent = new Intent(this, dest);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 3000);
    }
}
