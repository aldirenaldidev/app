package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Pengaturan: satu-satunya kontrol manual yang tersisa —
 * toggle "Auto-refresh background" (default ON).
 */
public class SettingsActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        Switch sw = findViewById(R.id.swAutoRefresh);
        TextView tvDesc = findViewById(R.id.tvSettingDesc);
        tvDesc.setText("Saat aktif, BursaKu otomatis memperbarui data indeks, "
                + "watchlist, dan alert harga tiap 15 menit selama jam bursa "
                + "(Senin–Jumat). Di luar jam bursa tidak ada polling.");

        findViewById(R.id.btnAboutOpen).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            }
        });

        sw.setChecked(Prefs.autoRefreshOn(this));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean on) {
                Prefs.setAutoRefresh(SettingsActivity.this, on);
                if (on) AlertScheduler.schedule(SettingsActivity.this);
                else AlertScheduler.cancel(SettingsActivity.this);
            }
        });
    }
}
