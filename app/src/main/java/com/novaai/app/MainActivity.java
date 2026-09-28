package com.novaai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // غيّر هذا لعنوان الـ Backend عندك
    private static final String BACKEND_URL = "http://10.0.2.2:3000";

    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 10;
    private static final int PREMIUM_LIMIT = 100;

    private SharedPreferences prefs;
    private String userName = "";
    private boolean premium = false;
    private int usage = 0;
    private ArrayList<String> messages = new ArrayList<>();

    private LinearLayout root;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        userName = prefs.getString("user_name", "");
        premium = prefs.getBoolean("premium", false);
        usage = prefs.getInt("usage", 0);
        loadMessages();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F0F1A"));
        setContentView(root);

        if (userName.isEmpty()) {
            showAccountScreen();
        } else {
            showChatScreen();
        }
    }

    private void showAccountScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextSize(32);
        title.setTextColor(Color.parseColor("#7C5CFF"));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("إنشاء حساب محلي");
        sub.setTextSize(16);
        sub.setTextColor(Color.parseColor("#AAAAAA"));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(12), 0, dp(32));
        root.addView(sub);

        EditText nameInput = new EditText(this);
        nameInput.setHint("اسمك");
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(Color.parseColor("#666666"));
        nameInput.setBackgroundColor(Color.parseColor("#1A1A2E"));
        nameInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        nameInput.setSingleLine(true);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(nameInput, ep);

        Button btn = new Button(this);
        btn.setText("متابعة");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#7C5CFF"));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(16);
        root.addView(btn, bp);

        btn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "اكتب اسمك", Toast.LENGTH_SHORT).show();
                return;
            }
            userName = name;
            prefs.edit().putString("user_name", userName).apply();
            showChatScreen();
        });
    }

    private void showChatScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.TOP);
        root.setPadding(0, 0, 0, 0);

        // Top bar
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(Color.parseColor("#1A1A2E"));
        top.setPadding(dp(16), dp(16), dp(16), dp(16));
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(title);

        Button plansBtn = new Button(this);
        plansBtn.setText(premium ? "Premium" : "ترقية");
        plansBtn.setTextColor(Color.parseColor("#7C5CFF"));
        plansBtn.setBackgroundColor(Color.TRANSPARENT);
        plansBtn.setOnClickListener(v -> showPlansScreen());
        top.addView(plansBtn);

        root.addView(top, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Status
        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + "  |  " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + limit);
        status.setTextSize(13);
        status.setTextColor(Color.parseColor("#AAAAAA"));
        status.setPadding(dp(16), dp(8), dp(16), dp(8));
        root.addView(status);

        // Messages
        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(msgBox);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        root.addView(scroll, scrollParams);

        refreshMessages(msgBox);

        // Input area
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(Color.parseColor("#1A1A2E"));
        bottom.setPadding(dp(8), dp(8), dp(8), dp(8));
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        EditText input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#666666"));
        input.setBackgroundColor(Color.parseColor("#0F0F1A"));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        bottom.addView(input, ip);

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(Color.WHITE);
        send.setBackgroundColor(Color.parseColor("#7C5CFF"));
        bottom.addView(send);

        root.addView(bottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;

            int lim = premium ? PREMIUM_LIMIT : FREE_LIMIT;
            if (usage >= lim) {
                Toast.makeText(this, "انتهت الاستخدامات. فعّل Premium", Toast.LENGTH_SHORT).show();
                return;
            }

            input.setText("");
            messages.add("أنت: " + text);
            usage++;
            prefs.edit().putInt("usage", usage).apply();
            saveMessages();
            status.setText("أهلاً " + userName + "  |  " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + lim);
            refreshMessages(msgBox);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

            send.setEnabled(false);
            send.setText("...");

            executor.execute(() -> {
                String reply;
                try {
                    reply = sendToBackend(text);
                } catch (Exception e) {
                    reply = "تعذر الاتصال بالخادم. تأكد أن Backend يعمل.";
                }
                String finalReply = reply;
                mainHandler.post(() -> {
                    messages.add("NOVA AI: " + finalReply);
                    saveMessages();
                    refreshMessages(msgBox);
                    scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                    send.setEnabled(true);
                    send.setText("إرسال");
                });
            });
        });
    }

    private void showPlansScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.TOP);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));

        Button back = new Button(this);
        back.setText("← رجوع");
        back.setTextColor(Color.parseColor("#7C5CFF"));
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setGravity(Gravity.START);
        back.setOnClickListener(v -> showChatScreen());
        root.addView(back);

        TextView title = new TextView(this);
        title.setText("خطط NOVA AI");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, dp(16), 0, dp(16));
        root.addView(title);

        // Free card
        LinearLayout freeCard = card();
        TextView freeTitle = new TextView(this);
        freeTitle.setText("مجاني");
        freeTitle.setTextSize(18);
        freeTitle.setTextColor(Color.WHITE);
        freeCard.addView(freeTitle);
        TextView freeDesc = new TextView(this);
        freeDesc.setText("10 استخدامات AI");
        freeDesc.setTextColor(Color.parseColor("#AAAAAA"));
        freeCard.addView(freeDesc);
        if (!premium) {
            TextView cur = new TextView(this);
            cur.setText("✓ الخطة الحالية");
            cur.setTextColor(Color.parseColor("#7C5CFF"));
            freeCard.addView(cur);
        }
        root.addView(freeCard);

        // Premium card
        LinearLayout premCard = card();
        premCard.setBackgroundColor(Color.parseColor("#221A3A"));
        TextView premTitle = new TextView(this);
        premTitle.setText("Premium");
        premTitle.setTextSize(18);
        premTitle.setTextColor(Color.parseColor("#7C5CFF"));
        premCard.addView(premTitle);
        TextView premDesc = new TextView(this);
        premDesc.setText("100 استخدام تجريبي");
        premDesc.setTextColor(Color.parseColor("#AAAAAA"));
        premCard.addView(premDesc);
        if (premium) {
            TextView on = new TextView(this);
            on.setText("✓ مفعّلة");
            on.setTextColor(Color.parseColor("#7C5CFF"));
            premCard.addView(on);
        } else {
            Button activate = new Button(this);
            activate.setText("تفعيل للتجربة");
            activate.setTextColor(Color.WHITE);
            activate.setBackgroundColor(Color.parseColor("#7C5CFF"));
            activate.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                Toast.makeText(this, "تم تفعيل Premium", Toast.LENGTH_SHORT).show();
                showChatScreen();
            });
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ap.topMargin = dp(8);
            premCard.addView(activate, ap);
        }
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(12);
        root.addView(premCard, pp);

        TextView note = new TextView(this);
        note.setText("ملاحظة: التفعيل تجريبي ولا يوجد دفع حقيقي بعد.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#888888"));
        note.setPadding(0, dp(20), 0, 0);
        root.addView(note);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(Color.parseColor("#1A1A2E"));
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        return c;
    }

    private void refreshMessages(LinearLayout box) {
        box.removeAllViews();
        for (String m : messages) {
            TextView tv = new TextView(this);
            tv.setText(m);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setBackgroundColor(Color.parseColor("#1A1A2E"));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            box.addView(tv, lp);
        }
    }

    private String sendToBackend(String message) throws Exception {
        URL url = new URL(BACKEND_URL + "/api/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String body = new JSONObject().put("message", message).toString();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new Exception("خطأ " + code);
        }
        return new JSONObject(sb.toString()).optString("reply", "لم يصل رد");
    }

    private void loadMessages() {
        String raw = prefs.getString("messages", null);
        messages.clear();
        if (raw == null) {
            messages.add("NOVA AI: مرحبًا! أنا NOVA AI 👋\nكيف أقدر أساعدك؟");
            return;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                messages.add(arr.optString(i));
            }
        } catch (Exception e) {
            messages.add("NOVA AI: مرحبًا! أنا NOVA AI 👋");
        }
    }

    private void saveMessages() {
        try {
            JSONArray arr = new JSONArray();
            for (String m : messages) arr.put(m);
            prefs.edit().putString("messages", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
