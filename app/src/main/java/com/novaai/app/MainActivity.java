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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 10;
    private static final int PREMIUM_LIMIT = 100;

    private SharedPreferences prefs;
    private String userName = "";
    private boolean premium = false;
    private int usage = 0;
    private int jokeIndex = 0;
    private ArrayList<String> messages = new ArrayList<>();

    private LinearLayout root;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

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

        if (userName.isEmpty()) showAccountScreen();
        else showChatScreen();
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
        root.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + " | " + (premium ? "Premium" : "مجاني") + " " + usage + "/" + limit);
        status.setTextSize(13);
        status.setTextColor(Color.parseColor("#AAAAAA"));
        status.setPadding(dp(16), dp(8), dp(16), dp(8));
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

        EditText input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#666666"));
        input.setBackgroundColor(Color.parseColor("#0F0F1A"));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
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
                Toast.makeText(this, "انتهت الاستخدامات. اضغط ترقية", Toast.LENGTH_SHORT).show();
                return;
            }

            input.setText("");
            messages.add("أنت: " + text);
            usage++;
            prefs.edit().putInt("usage", usage).apply();
            saveMessages();
            status.setText("أهلاً " + userName + " | " + (premium ? "Premium" : "مجاني") + " " + usage + "/" + lim);
            refreshMessages(msgBox);
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

            send.setEnabled(false);
            send.setText("...");

            executor.execute(() -> {
                // تأخير بسيط عشان يحس إنه يفكر
                try { Thread.sleep(400 + random.nextInt(600)); } catch (Exception ignored) {}
                String reply = localReply(text);
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
        freeTitle.setText("مجاني - 10 استخدامات");
        freeTitle.setTextColor(Color.WHITE);
        freeTitle.setTextSize(18);
        freeCard.addView(freeTitle);
        if (!premium) {
            TextView cur = new TextView(this);
            cur.setText("الخطة الحالية");
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
        premTitle.setText("Premium - 100 استخدام");
        premTitle.setTextColor(Color.parseColor("#7C5CFF"));
        premTitle.setTextSize(18);
        premCard.addView(premTitle);
        if (premium) {
            TextView on = new TextView(this);
            on.setText("مفعلة");
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
                showChatScreen();
            });
            premCard.addView(activate);
        }
        root.addView(premCard, pp);
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

    private String localReply(String message) {
        String m = message.toLowerCase().trim();

        // وداع أولاً
        if (has(m, "مع السلامة", "مع السلامه", "باي", "وداع", "bye", "إلى اللقاء", "الى اللقاء"))
            return pick(
                    "مع السلامة " + userName + "! يومك سعيد.",
                    "إلى اللقاء " + userName + "! أشوفك قريب.",
                    "الله معك " + userName + "!"
            );

        // تحية
        if (has(m, "سلام", "هلا", "مرحبا", "مرحباً", "hello", "hi", "صباح", "مساء"))
            return pick(
                    "وعليكم السلام " + userName + "! كيف أقدر أساعدك؟",
                    "هلا " + userName + "! تفضل وش تحتاج؟",
                    "أهلًا " + userName + "! موجود لك."
            );

        if (has(m, "كيف حالك", "كيفك", "أخبارك", "اخبارك", "شلونك", "وش أخبارك"))
            return pick(
                    "بخير الحمد لله يا " + userName + "! وأنت كيف حالك؟",
                    "تمام وبأفضل حال! أنت كيفك؟",
                    "الحمد لله بخير، سعيد إنك سألت."
            );

        if (has(m, "اسمك", "من أنت", "من انت", "وش اسمك", "شنو اسمك"))
            return "أنا NOVA AI، مساعدك التجريبي المحلي. أقدر أرد على تحية، وقت، تاريخ، نكت، ونصائح بسيطة.";

        if (has(m, "شكرا", "شكرًا", "شكراً", "مشكور", "thanks", "يسلمو"))
            return pick("العفو!", "ولا يهمك!", "أنا هنا لأي شيء.");

        if (has(m, "تساعد", "مساعدة", "ساعدني", "تقدر", "help", "وش تقدر", "ايش تقدر", "أي خدمة", "اي خدمة"))
            return "أكيد " + userName + "! أقدر أساعدك في:\n• الوقت والتاريخ\n• نكت\n• تحية ووداع\n• نصائح بسيطة\n• أسئلة عن التطبيق\n\nاكتب سؤالك.";

        // وقت وتاريخ
        if (has(m, "وقت", "ساعه", "ساعة", "كم الساعه", "كم الساعة", "التوقيت")) {
            Calendar c = Calendar.getInstance();
            return "الوقت التقريبي على جهازك: " + String.format("%02d:%02d",
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
        if (has(m, "تاريخ", "اليوم", "كم تاريخ", "أي يوم")) {
            Calendar c = Calendar.getInstance();
            return "تاريخ اليوم: " + c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        }

        // نكت
        if (has(m, "نكتة", "نكته", "نكت", "اضحكني", "joke", "ضحكني")) {
            String[] jokes = {
                    "ليش الكمبيوتر راح للدكتور؟ عشان عنده فيروس! 😄",
                    "المبرمج قال لزوجته: أحبك من 1 إلى 10... فقالت: وأنت؟ قال: من 0 إلى 1 فقط 😅",
                    "ليش الـ WiFi حزين؟ لأنه فقد الـ connection 😢",
                    "دخل بايثون على مطعم... قال له النادل: SyntaxError 😂",
                    "ليش الهاتف ما يضحك؟ لأنه على Silent mode 🤫",
                    "واحد غبي اشترى كمبيوتر محمول... طاح منه، قال: سموه محمول عشان كذا! 😆",
                    "ليش السيرفر ما ينام؟ لأنه عنده 24/7 uptime 😴"
            };
            String joke = jokes[jokeIndex % jokes.length];
            jokeIndex++;
            return joke;
        }

        // إيموجي ومشاعر
        if (has(m, "😂", "🤣", "😆", "ضحك", "حلو", "حلوه", "زين", "ممتاز", "رائع", "خروف"))
            return pick("يسعدني!", "تبي نكتة ثانية؟", "هههه تمام!");

        if (has(m, "❤️", "💕", "😍", "حب", "أحبك", "احبك"))
            return "هههه شكرًا! أنا مساعد، وسعادتي إنك تستفيد مني.";

        if (has(m, "👍", "👏", "🔥", "قوي", "ياش"))
            return "تمام! أي سؤال ثاني؟";

        if (has(m, "حزين", "زعلان", "تعبان", "ضايق", "ممل"))
            return "آسف تحس كذا. تبي نكتة خفيفة ولا نغيير الموضوع؟";

        // عن التطبيق
        if (has(m, "nova", "نوفا", "تطبيق", "هالتطبيق", "من صنعك", "مطور", "برمجك"))
            return "NOVA AI نسخة تجريبية محلية. الردود محفوظة على الجهاز، وبدون إنترنت للذكاء. هدفها تتطور لاحقًا.";

        if (has(m, "premium", "بريميوم", "ترقية", "اشتراك", "مجاني"))
            return "المجاني 10 رسائل، وPremium التجريبية 100. من زر الترقية فوق. ما فيه دفع حقيقي بعد.";

        // نصائح بسيطة
        if (has(m, "نصيحة", "نصيحه", "تحفيز", "تحمسني"))
            return pick(
                    "ابدأ بخطوة صغيرة اليوم، والباقي يجي تدريجي.",
                    "النجاح غالباً استمرارية مو سرعة.",
                    "خذ راحة قصيرة، بعدين كمّل بتركيز."
            );

        if (has(m, "طقس", "جو", "حر", "برد", "مطر"))
            return "ما أقدر أعرف الطقس الحقيقي بدون إنترنت وخدمة خارجية. شيك تطبيق الطقس عندك.";

        if (has(m, "أخبار", "خبر", "حدث"))
            return "ما عندي أخبار مباشرة. افتح مصدر أخبار موثوق للجديد.";

        // رد افتراضي أذكى شوي
        return pick(
                "فهمت: «" + message + "»\nمو ضمن الردود المحفوظة بعد. جرب: وقت، تاريخ، نكتة، نصيحة، أو مرحبا.",
                "وصلت رسالتك. هذي نسخة محلية محدودة. اسألني عن الوقت أو نكتة أو مساعدة.",
                "ما عندي رد جاهز لهالسؤال بالضبط. جرب تصيغه بطريقة ثانية، أو اسأل عن الوقت/نكتة/مساعدة."
        );
    }

    private boolean has(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private void loadMessages() {
        messages.clear();
        String raw = prefs.getString("messages", null);
        if (raw == null) {
            messages.add("NOVA AI: مرحبًا! أنا NOVA AI\nكيف أقدر أساعدك؟");
            return;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) messages.add(arr.optString(i));
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
