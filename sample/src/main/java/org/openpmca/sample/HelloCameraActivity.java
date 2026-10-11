package org.openpmca.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import org.openpmca.framework.PmcaRuntime;

public final class HelloCameraActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText(DiagnosticsFormatter.format(PmcaRuntime.inspect()));
        textView.setTextSize(14.0f);
        textView.setPadding(16, 16, 16, 16);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);

        setContentView(scrollView);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            finish();
            startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
