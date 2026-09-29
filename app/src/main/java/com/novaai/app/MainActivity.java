package com.novaai.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

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
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
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
        nameInput.setGravity(Gravity.RIGHT);
        root.addView(nameInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(Color.parseColor("#1A1A2E"));
        top.setPadding(dp(12), dp(14), dp(12), dp(14));
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

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
        root.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + "  |  " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + limit);
        status.setTextSize(13);
        status.setTextColor(Color.parseColor("#AAAAAA"));
        status.setPadding(dp(16), dp(8), dp(16), dp(8));
        status.setGravity(Gravity.RIGHT);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(msgBox);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        refreshMessages(msgBox);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(Color.parseColor("#1A1A2E"));
        bottom.setPadding(dp(8), dp(8), dp(8), dp(8));
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#666666"));
        input.setBackgroundColor(Color.parseColor("#0F0F1A"));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
        input.setGravity(Gravity.RIGHT);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(Color.WHITE);
        send.setBackgroundColor(Color.parseColor("#7C5CFF"));
        bottom.addView(send);
        root.addView(bottom);

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
                String reply = getReply(text);
                mainHandler.post(() -> {
                    messages.add("NOVA AI: " + reply);
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
        back.setText("رجوع");
        back.setTextColor(Color.parseColor("#7C5CFF"));
        back.setBackgroundColor(Color.TRANSPARENT);
        back.setOnClickListener(v -> showChatScreen());
        root.addView(back);

        TextView title = new TextView(this);
        title.setText("خطط NOVA AI");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, dp(16), 0, dp(16));
        root.addView(title);

        LinearLayout freeCard = new LinearLayout(this);
        freeCard.setOrientation(LinearLayout.VERTICAL);
        freeCard.setBackgroundColor(Color.parseColor("#1A1A2E"));
        freeCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView freeTitle = new TextView(this);
        freeTitle.setText("مجاني — 10 استخدامات");
        freeTitle.setTextColor(Color.WHITE);
        freeTitle.setTextSize(18);
        freeCard.addView(freeTitle);
        if (!premium) {
            TextView cur = new TextView(this);
            cur.setText("✓ الخطة الحالية");
            cur.setTextColor(Color.parseColor("#7C5CFF"));
            freeCard.addView(cur);
        }
        root.addView(freeCard);

        LinearLayout premCard = new LinearLayout(this);
        premCard.setOrientation(LinearLayout.VERTICAL);
        premCard.setBackgroundColor(Color.parseColor("#221A3A"));
        premCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(12);

        TextView premTitle = new TextView(this);
        premTitle.setText("Premium — 100 استخدام");
        premTitle.setTextColor(Color.parseColor("#7C5CFF"));
        premTitle.setTextSize(18);
        premCard.addView(premTitle);

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
            premCard.addView(activate);
        }
        root.addView(premCard, pp);

        TextView note = new TextView(this);
        note.setText("التفعيل تجريبي ولا يوجد دفع حقيقي بعد.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#888888"));
        note.setPadding(0, dp(20), 0, 0);
        root.addView(note);
    }

    private void refreshMessages(LinearLayout box) {
        box.removeAllViews();
        for (String m : messages) {
            boolean isUser = m.startsWith("أنت:");
            TextView tv = new TextView(this);
            tv.setText(m);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setBackgroundColor(isUser ? Color.parseColor("#2D2B55") : Color.parseColor("#1A1A2E"));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            tv.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            box.addView(tv, lp);
        }
    }

    private String getReply(String message) {
        try {
            URL url = new URL(BACKEND_URL + "/api/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            String body = new JSONObject().put("message", message).toString();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();
                String reply = new JSONObject(sb.toString()).optString("reply", "");
                if (!reply.isEmpty()) return reply;
            }
            conn.disconnect();
        } catch (Exception ignored) {
        }
        return localReply(message);
    }

    private String localReply(String message) {
        String m = message.toLowerCase().trim();

        if (containsAny(m, "سلام", "هلا", "مرحبا", "مرحباً", "hello", "hi", "السلام")) {
            return "وعليكم السلام " + userName + "! كيف أقدر أساعدك؟";
        }
        if (containsAny(m, "كيف حالك", "كيفك", "أخبارك", "شلونك", "كيف الحال")) {
            return "بخير الحمد لله يا " + userName + "! وأنت كيف حالك؟";
        }
        if (containsAny(m, "اسمك", "من أنت", "من انت", "وش اسمك", "شنو اسمك")) {
            return "أنا NOVA AI، مساعدك الذكي التجريبي. سعيد بمعرفتك!";
        }
        if (containsAny(m, "شكرا", "شكرًا", "شكراً", "مشكور", "thanks", "thank")) {
            return "العفو! أي وقت تحتاجني أنا هنا.";
        }
        if (containsAny(m, "باي", "مع السلامة", "وداعا", "bye", "أشوفك")) {
            return "مع السلامة " + userName + "! يومك سعيد.";
        }
        if (containsAny(m, "مساعدة", "تساعد", "help", "ساعدني")) {
            return "أكيد! اسألني عن:\n• تحية وتعارف\n• الوقت التقريبي\n• نكت بسيطة\n• أو أي سؤال عام";
        }
        if (containsAny(m, "وقت", "ساعة", "كم الساعة", "التوقيت")) {
            Calendar c = Calendar.getInstance();
            int h = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);
            return "الوقت التقريبي على جهازك: " + String.format("%02d:%02d", h, min);
        }
        if (containsAny(m, "تاريخ", "اليوم", "أي يوم")) {
            Calendar c = Calendar.getInstance();
            int d = c.get(Calendar.DAY_OF_MONTH);
            int mo = c.get(Calendar.MONTH) + 1;
            int y = c.get(Calendar.YEAR);
            return "تاريخ اليوم على جهازك: " + d + "/" + mo + "/" + y;
        }
        if (containsAny(m, "نكتة", "نكت", "اضحكني", "joke")) {
            return "ليش الكمبيوتر راح للدكتور؟\nعشان عنده فيروس! 😄";
        }
        if (containsAny(m, "تطبيق", "هذا البرنامج", "nova", "نوفا")) {
            return "NOVA AI تطبيق محادثة تجريبي. الحين يردود محلية، ولاحقًا يتصل بذكاء اصطناعي حقيقي.";
        }
        if (containsAny(m, "premium", "بريميوم", "ترقية", "اشتراك")) {
            return "الخطة المجانية 10 رسائل، وPremium التجريبية 100 رسالة. من زر الترقية فوق.";
        }
        if (containsAny(m, "أحبك", "احبك", "love")) {
            return "هههه شكرًا! أنا مساعد ذكي، وسعادتي إنك تستفيد مني.";
        }
        if (containsAny(m, "طقس", "جو", "حر", "برد")) {
            return "ما أقدر أعرف الطقس الحقيقي بدون إنترنت وخادم، بس تقدر تشيك تطبيق الطقس.";
        }
        if (containsAny(m, "من صنعك", "من برمجك", "مطور", "صانع")) {
            return "تم بنائي كنسخة تجريبية لتطبيق NOVA AI.";
        }

        return "فهمت رسالتك: «" + message + "»\n\n"
                + "هذي نسخة تجريبية بردود محلية. "
                + "جرب تسأل عن: الوقت، التاريخ، نكتة، أو قل مرحبا.";
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private void loadMessages() {
        messages.clear();
        String raw = prefs.getString("messages", null);
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
            messages.add("NOVA AI: مرحبًا!");
        }
    }

    private void saveMessages() {
        try {
            JSONArray arr = new JSONArray();
            for (String m : messages) arr.put(m);
            prefs.edit().putString("messages", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
