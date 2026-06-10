package com.dentnova.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dentnova.app.R;

public class HowItWorksActivity extends AppCompatActivity {

    private final String[][] steps = {
            {"  Assessment", "Answer oral-health questions to understand your habits and risk score."},
            {"  Tooth Scan", "Capture or upload a tooth image after assessment for visual support."},
            {"  Smart Result", "DentNova combines assessment answers and scan signs into a clear report."},
            {"  Reminders", "Set brushing, flossing and toothbrush replacement reminders."},
            {"  Brushing & Flossing", "Build healthy daily brushing and flossing habits."},
            {"  Education", "Learn simple dental tips and improve your oral-care routine."}
    };

    private final int[] images = {
            R.drawable.img_assessment,
            R.drawable.img_tooth_scan,
            R.drawable.img_result,
            R.drawable.img_reminders,
            R.drawable.img_brushing_flossing,
            R.drawable.img_education
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_it_works);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupStep(R.id.step1, 0);
        setupStep(R.id.step2, 1);
        setupStep(R.id.step3, 2);
        setupStep(R.id.step4, 3);
        setupStep(R.id.step5, 4);
        setupStep(R.id.step6, 5);
    }

    private void setupStep(int stepId, int index) {
        View step = findViewById(stepId);

        ImageView image = step.findViewById(R.id.ivStepIcon);
        TextView title = step.findViewById(R.id.tvStepTitle);
        TextView desc = step.findViewById(R.id.tvStepDesc);
        TextView number = step.findViewById(R.id.tvStepNumber);

        image.setImageResource(images[index]);
        image.clearColorFilter();

        number.setText(String.valueOf(index + 1));
        title.setText(steps[index][0]);
        desc.setText(steps[index][1]);
    }
}