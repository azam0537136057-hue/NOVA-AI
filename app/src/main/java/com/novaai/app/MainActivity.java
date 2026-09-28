package com.novaai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#0F0F1A"));
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextSize(28);
        title.setTextColor(Color.parseColor("#7C5CFF"));
        title.setGravity(Gravity.CENTER);

        TextView msg = new TextView(this);
        msg.setText("التطبيق يعمل بنجاح");
        msg.setTextSize(18);
        msg.setTextColor(Color.WHITE);
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 40, 0, 0);

        layout.addView(title);
        layout.addView(msg);
        setContentView(layout);
    }
}
