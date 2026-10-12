package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

/** Layar Tentang: hak cipta & kredit developer. */
public class AboutActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_about);

        openUrl(R.id.btnIg, "https://instagram.com/aldire__");
    }

    private void openUrl(int btnId, final String url) {
        findViewById(btnId).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }
}
