package com.dentnova.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.dentnova.app.R;
import com.dentnova.app.activities.ScanDetailActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ToothScanAdapter extends RecyclerView.Adapter<ToothScanAdapter.ScanViewHolder> {

    private final Context context;
    private final JsonArray scans;

    public ToothScanAdapter(Context context, JsonArray scans) {
        this.context = context;
        this.scans = scans;
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tooth_scan_history, parent, false);
        return new ScanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        JsonObject scan = scans.get(position).getAsJsonObject();

        // Unique cache key per scan record (prevents Glide serving wrong cached image)
        String scanId = scan.has("id") ? scan.get("id").getAsString() : String.valueOf(position);

        // ── Issue 4 fix: display clean diagnosis, strip any confidence percentage ──
        String rawLabel = scan.has("result_label") && !scan.get("result_label").isJsonNull()
                ? scan.get("result_label").getAsString() : "AI Tooth Scan";
        String label = getCleanDiagnosis(rawLabel);

        // ── Issue 2 fix: parse as UTC then display in device local timezone ─────
        String rawCreatedAt = scan.has("created_at") ? scan.get("created_at").getAsString() : "";
        String formattedDateTime = formatTimestamp(rawCreatedAt);

        holder.tvDiagnosis.setText(label);
        holder.tvDateTime.setText(formattedDateTime);

        // Score fields (DB column mapping from ApiService.saveToothScan)
        double overallScore      = scan.has("plaque_score")       ? scan.get("plaque_score").getAsDouble()       : 0.0;
        double inflammationScore = scan.has("gum_score")          ? scan.get("gum_score").getAsDouble()          : 0.0;
        double cleanlinessScore  = scan.has("cleanliness_score")  ? scan.get("cleanliness_score").getAsDouble()  : 0.0;

        // ── Issue 1 fix: strip data URL prefix so both web and Android images decode correctly ──
        String rawBase64  = scan.has("image_base64") && !scan.get("image_base64").isJsonNull()
                ? scan.get("image_base64").getAsString() : "";
        String pureBase64 = stripDataUrlPrefix(rawBase64);

        if (!pureBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(pureBase64, Base64.DEFAULT);
                // Use scan ID as Glide signature — prevents cache collisions between RecyclerView items
                Glide.with(context)
                        .load(imageBytes)
                        .signature(new ObjectKey(scanId))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_tooth_outline)
                        .error(R.drawable.ic_tooth_outline)
                        .into(holder.imgThumbnail);
            } catch (Exception e) {
                holder.imgThumbnail.setImageResource(R.drawable.ic_tooth_outline);
            }
        } else {
            holder.imgThumbnail.setImageResource(R.drawable.ic_tooth_outline);
        }

        // Open details on card/thumbnail click — pass pureBase64 so ScanDetailActivity can decode it
        View.OnClickListener clickListener = v -> {
            Intent intent = new Intent(context, ScanDetailActivity.class);
            intent.putExtra("diagnosis_label",      label);
            intent.putExtra("created_at_formatted", formattedDateTime.replace("\n", " "));
            intent.putExtra("overall_score",         overallScore);
            intent.putExtra("inflammation_score",    inflammationScore);
            intent.putExtra("cleanliness_score",     cleanlinessScore);
            intent.putExtra("image_base64",          pureBase64);
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.imgThumbnail.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return scans.size();
    }

    // ── Issue 1 helper: normalise to pure base64 ─────────────────────────────
    // Web saves "data:image/jpeg;base64,/9j/..." — Android saves "/9j/..." (no prefix).
    // Strip the prefix so Base64.decode() always receives valid input.
    private String stripDataUrlPrefix(String base64) {
        if (base64 == null || base64.isEmpty()) return "";
        int comma = base64.indexOf(",");
        if (base64.startsWith("data:") && comma >= 0) {
            return base64.substring(comma + 1);
        }
        return base64;
    }

    // ── Issue 4 helper: strip confidence percentage and map short names ───────
    // Handles old records like "Gingivitis 89%" or "Healthy (97% confidence)".
    private String getCleanDiagnosis(String raw) {
        if (raw == null || raw.isEmpty()) return "AI Tooth Scan";
        // Remove trailing confidence patterns e.g. " 89%", " (89%)", " (89% confidence)"
        String stripped = raw.replaceAll("\\s*\\(?\\d+%[^)]*\\)?\\s*$", "").trim();
        String lower = stripped.toLowerCase(Locale.US);
        // Map bare class names to full descriptive labels
        if (lower.equals("healthy") || lower.equals("healthy gums")) {
            return "Healthy gums and excellent oral condition";
        }
        if (lower.equals("gingivitis")) {
            return "Possible gingival inflammation detected";
        }
        if (lower.equals("calculus")) {
            return "Poor cleanliness / calculus signs detected";
        }
        // Already a full label — return as-is
        return stripped.isEmpty() ? raw : stripped;
    }

    // ── Issue 2 helper: convert UTC Supabase timestamp to device local time ──
    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) return "";
        try {
            // Normalise: replace T separator, drop microseconds and tz offset
            String clean = rawTimestamp.replace("T", " ");
            if (clean.contains(".")) clean = clean.substring(0, clean.indexOf("."));
            if (clean.contains("+")) clean = clean.substring(0, clean.indexOf("+"));
            if (clean.contains("Z"))  clean = clean.replace("Z", "");
            clean = clean.trim();

            SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            parseFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // ← parse as UTC
            Date date = parseFormat.parse(clean);

            // displayFormat inherits the device's default local timezone
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy\nhh:mm a", Locale.US);
            return displayFormat.format(date);
        } catch (Exception e) {
            return rawTimestamp; // graceful fallback
        }
    }

    static class ScanViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumbnail;
        final TextView  tvDiagnosis;
        final TextView  tvDateTime;
        final ImageView imgAction;

        ScanViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            tvDiagnosis  = itemView.findViewById(R.id.tvDiagnosis);
            tvDateTime   = itemView.findViewById(R.id.tvDateTime);
            imgAction    = itemView.findViewById(R.id.imgAction);
        }
    }
}
