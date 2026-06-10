package com.dentnova.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.dentnova.app.R;
import com.google.android.material.button.MaterialButton;

/**
 * OnboardingActivity — exact replica of onboarding_screen.dart
 *
 * 3 pages: "Track Your Smile" | "AI Tooth Scan" | "Daily Reminders"
 * - Dots: animated width 20dp (active, cyan) / 8dp (inactive, white)
 * - Skip button → AuthActivity
 * - Next: advance page  |  Get Started (last page): → AuthActivity
 * - Saves "has_seen_onboarding" = true on exit
 */
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private LinearLayout llDots;
    private int currentPage = 0;

    private final String[] titles    = {"Track Your Smile", "AI Tooth Scan", "Daily Reminders"};
    private final String[] subtitles = {
        "Take a quick assessment and discover the health of your teeth and gums.",
        "Snap a photo and let our AI analyze plaque, gum condition, and cleanliness.",
        "Never skip brushing again with smart, gentle reminders that fit your routine."
    };
    // Icon resources: medical_services, document_scanner, notifications_active
    private final int[] icons = {
            R.drawable.onboarding_track,
            R.drawable.onboarding_scan,
            R.drawable.onboarding_reminder
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext   = findViewById(R.id.btnNext);
        llDots    = findViewById(R.id.llDots);

        viewPager.setAdapter(new OnboardingAdapter());
        viewPager.setOffscreenPageLimit(3);
        viewPager.setCurrentItem(0, false);
        buildDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                buildDots(position);
                // Update button text: "Next" or "Get Started"
                btnNext.setText(position < titles.length - 1 ? "Next" : "Get Started");
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < titles.length - 1) {
                viewPager.setCurrentItem(currentPage + 1, true);
            } else {
                goToAuth();
            }
        });

        TextView tvSkip = findViewById(R.id.tvSkip);
        tvSkip.setOnClickListener(v -> goToAuth());
    }

    /**
     * Builds animated dot indicators — replicates Flutter AnimatedContainer dots
     * Active dot: width=20dp height=8dp cyan (#00BCD4)
     * Inactive dot: width=8dp height=8dp white
     */
    private void buildDots(int active) {
        llDots.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        for (int i = 0; i < titles.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    (int) ((i == active ? 20 : 8) * dp), (int) (8 * dp));
            lp.setMargins((int)(4*dp), 0, (int)(4*dp), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == active ?
                    R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            llDots.addView(dot);
        }
    }

    private void goToAuth() {
        // Save onboarding seen flag (replaces Flutter SharedPreferences has_seen_onboarding)
        SharedPreferences prefs = getSharedPreferences("dentnova_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("has_seen_onboarding", true).apply();

        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    // ── ViewPager2 adapter (replaces Flutter PageView.builder + _OnboardingPage) ──
    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_page, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int pos) {
            holder.tvTitle.setText(titles[pos]);
            holder.tvSubtitle.setText(subtitles[pos]);
            holder.ivIllustration.setImageResource(icons[pos]);
        }

        @Override public int getItemCount() { return titles.length; }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle;
            ImageView ivIllustration;
            VH(View v) {
                super(v);
                tvTitle        = v.findViewById(R.id.tvTitle);
                tvSubtitle     = v.findViewById(R.id.tvSubtitle);
                ivIllustration = v.findViewById(R.id.ivIllustration);
            }
        }
    }
}
