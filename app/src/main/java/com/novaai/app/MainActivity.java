package com.novaai.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.HorizontalScrollView;
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
    private static final int FREE_LIMIT = 15;
    private static final int PREMIUM_LIMIT = 200;

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
        root.setBackgroundColor(Color.parseColor("#0B0B14"));
        setContentView(root);

        if (userName.isEmpty()) showAccountScreen();
        else showChatScreen();
    }

    private void showAccountScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));

        TextView badge = new TextView(this);
        badge.setText("NOVA AI");
        badge.setTextSize(34);
        badge.setTextColor(Color.parseColor("#8B7CFF"));
        badge.setGravity(Gravity.CENTER);
        root.addView(badge);

        TextView sub = new TextView(this);
        sub.setText("مساعدك المحلي الذكي\nنسخة تجريبية قوية");
        sub.setTextSize(15);
        sub.setTextColor(Color.parseColor("#9A9AB0"));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(12), 0, dp(28));
        root.addView(sub);

        EditText nameInput = new EditText(this);
        nameInput.setHint("اكتب اسمك للبدء");
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(Color.parseColor("#666680"));
        nameInput.setBackground(rounded(Color.parseColor("#16162A"), 16));
        nameInput.setPadding(dp(16), dp(16), dp(16), dp(16));
        nameInput.setSingleLine(true);
        root.addView(nameInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btn = new Button(this);
        btn.setText("ابدأ الآن");
        btn.setTextColor(Color.WHITE);
        btn.setBackground(rounded(Color.parseColor("#7C5CFF"), 16));
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
        top.setBackgroundColor(Color.parseColor("#12121F"));
        top.setPadding(dp(14), dp(14), dp(10), dp(14));
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(title);

        Button clearBtn = smallBtn("مسح");
        clearBtn.setOnClickListener(v -> confirmClear());
        top.addView(clearBtn);

        Button plansBtn = smallBtn(premium ? "Pro" : "ترقية");
        plansBtn.setOnClickListener(v -> showPlansScreen());
        top.addView(plansBtn);
        root.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + "  •  " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + limit);
        status.setTextSize(12);
        status.setTextColor(Color.parseColor("#8A8AA0"));
        status.setPadding(dp(16), dp(8), dp(16), dp(4));
        root.addView(status);

        // Quick actions
        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(10), dp(6), dp(10), dp(6));
        String[] quick = {"نكتة", "الوقت", "التاريخ", "نصيحة", "مساعدة"};
        for (String q : quick) {
            Button chip = new Button(this);
            chip.setText(q);
            chip.setTextSize(12);
            chip.setTextColor(Color.parseColor("#D0D0FF"));
            chip.setBackground(rounded(Color.parseColor("#1C1C32"), 20));
            chip.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMarginEnd(dp(8));
            chips.addView(chip, cp);
            chip.setOnClickListener(v -> sendQuick(q));
        }
        chipsScroll.addView(chips);
        root.addView(chipsScroll);

        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(msgBox);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        refreshMessages(msgBox);

        // Input
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(Color.parseColor("#12121F"));
        bottom.setPadding(dp(10), dp(10), dp(10), dp(10));
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        EditText input = new EditText(this);
        input.setHint("اكتب أي شيء...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#666680"));
        input.setBackground(rounded(Color.parseColor("#1A1A2E"), 14));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(Color.WHITE);
        send.setBackground(rounded(Color.parseColor("#7C5CFF"), 14));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(dp(8));
        bottom.addView(send, sp);
        root.addView(bottom);

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            doSend(text, status, msgBox, scroll, send);
        });
    }

    private void sendQuick(String text) {
        // يعيد فتح الشات مع إرسال مباشر عبر حفظ مؤقت بسيط
        // نستخدم نفس مسار الإرسال من الشاشة الحالية
        View statusView = null;
        ScrollView scroll = null;
        LinearLayout msgBox = null;
        Button sendBtn = null;

        // البحث عن العناصر داخل root
        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c instanceof TextView && ((TextView) c).getText().toString().contains("أهلاً")) {
                statusView = c;
            }
            if (c instanceof ScrollView) {
                scroll = (ScrollView) c;
                if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof LinearLayout) {
                    msgBox = (LinearLayout) scroll.getChildAt(0);
                }
            }
        }
        // آخر صف = الإدخال
        if (root.getChildCount() > 0) {
            View last = root.getChildAt(root.getChildCount() - 1);
            if (last instanceof LinearLayout) {
                LinearLayout bottom = (LinearLayout) last;
                for (int i = 0; i < bottom.getChildCount(); i++) {
                    if (bottom.getChildAt(i) instanceof Button) {
                        sendBtn = (Button) bottom.getChildAt(i);
                    }
                }
            }
        }

        if (statusView instanceof TextView && msgBox != null && scroll != null && sendBtn != null) {
            doSend(text, (TextView) statusView, msgBox, scroll, sendBtn);
        } else {
            // احتياطي
            messages.add("أنت: " + text);
            messages.add("NOVA AI: " + localReply(text));
            saveMessages();
            showChatScreen();
        }
    }

    private void doSend(String text, TextView status, LinearLayout msgBox, ScrollView scroll, Button send) {
        int lim = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        if (usage >= lim) {
            Toast.makeText(this, "انتهت الرسائل. اضغط ترقية", Toast.LENGTH_SHORT).show();
            return;
        }

        messages.add("أنت: " + text);
        usage++;
        prefs.edit().putInt("usage", usage).apply();
        saveMessages();
        status.setText("أهلاً " + userName + "  •  " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + lim);
        refreshMessages(msgBox);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        send.setEnabled(false);
        send.setText("...");

        executor.execute(() -> {
            try { Thread.sleep(350 + random.nextInt(500)); } catch (Exception ignored) {}
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
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("مسح المحادثة")
                .setMessage("تبي تمسح كل الرسائل؟")
                .setPositiveButton("مسح", (d, w) -> {
                    messages.clear();
                    messages.add("NOVA AI: تم مسح المحادثة. كيف أقدر أساعدك " + userName + "؟");
                    saveMessages();
                    showChatScreen();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showPlansScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.TOP);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));

        Button back = smallBtn("رجوع");
        back.setOnClickListener(v -> showChatScreen());
        root.addView(back);

        TextView title = new TextView(this);
        title.setText("الخطط");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, dp(16), 0, dp(16));
        root.addView(title);

        root.addView(planCard("مجاني", "15 رسالة تجريبية", !premium, false));
        LinearLayout pro = planCard("Premium", "200 رسالة تجريبية + مزايا أكثر", premium, true);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dp(12);
        root.addView(pro, pp);

        if (!premium) {
            Button activate = new Button(this);
            activate.setText("تفعيل Premium للتجربة");
            activate.setTextColor(Color.WHITE);
            activate.setBackground(rounded(Color.parseColor("#7C5CFF"), 14));
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ap.topMargin = dp(20);
            root.addView(activate, ap);
            activate.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                Toast.makeText(this, "تم تفعيل Premium", Toast.LENGTH_SHORT).show();
                showChatScreen();
            });
        }

        TextView note = new TextView(this);
        note.setText("ملاحظة: التفعيل تجريبي ولا يوجد دفع حقيقي.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#777790"));
        note.setPadding(0, dp(16), 0, 0);
        root.addView(note);
    }

    private LinearLayout planCard(String name, String desc, boolean current, boolean pro) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(pro ? Color.parseColor("#1A1530") : Color.parseColor("#16162A"), 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(18);
        t.setTextColor(pro ? Color.parseColor("#A78BFF") : Color.WHITE);
        card.addView(t);
        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#9A9AB0"));
        d.setPadding(0, dp(6), 0, 0);
        card.addView(d);
        if (current) {
            TextView c = new TextView(this);
            c.setText("✓ خطتك الحالية");
            c.setTextColor(Color.parseColor("#7C5CFF"));
            c.setPadding(0, dp(8), 0, 0);
            card.addView(c);
        }
        return card;
    }

    private void refreshMessages(LinearLayout box) {
        box.removeAllViews();
        for (String m : messages) {
            boolean isUser = m.startsWith("أنت:");
            TextView tv = new TextView(this);
            tv.setText(m);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setBackground(rounded(isUser ? Color.parseColor("#2A2650") : Color.parseColor("#16162A"), 14));
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

        if (has(m, "مع السلامة", "مع السلامه", "باي", "وداع", "bye", "إلى اللقاء", "الى اللقاء"))
            return pick("مع السلامة " + userName + "! يومك سعيد.",
                    "إلى اللقاء " + userName + "! أشوفك قريب.",
                    "الله معك " + userName + "!");

        if (m.equals("لا") || m.equals("لأ") || m.equals("لا شكرا") || m.equals("لا شكرًا"))
            return pick("حاضر.", "تمام، إذا احتجت شيء أنا هنا.", "أوك.");

        if (m.equals("نعم") || m.equals("ايوه") || m.equals("أيوه") || m.equals("ايه") || m.equals("يب") || m.equals("يس"))
            return pick("تم!", "تمام، كمل...", "أوك، تفضل.");

        if (has(m, "سلام", "هلا", "مرحبا", "مرحباً", "hello", "hi", "صباح", "مساء"))
            return pick("وعليكم السلام " + userName + "! كيف أقدر أساعدك؟",
                    "هلا " + userName + "! تفضل.",
                    "أهلًا " + userName + "! موجود.");

        if (has(m, "كيف حالك", "كيفك", "أخبارك", "اخبارك", "شلونك"))
            return pick("بخير الحمد لله يا " + userName + "! وأنت؟",
                    "تمام وبأفضل حال!",
                    "الحمد لله، سعيد بسؤالك.");

        if (has(m, "اسمك", "من أنت", "من انت", "وش اسمك"))
            return "أنا NOVA AI — مساعد محلي تجريبي. أرد على الوقت، التاريخ، النكت، النصائح، والمساعدة داخل التطبيق.";

        if (has(m, "شكرا", "شكرًا", "شكراً", "مشكور", "thanks", "يسلمو"))
            return pick("العفو!", "ولا يهمك!", "بخدمتك.");

        if (has(m, "تساعد", "مساعدة", "ساعدني", "تقدر", "help", "وش تقدر", "ايش تقدر", "أي خدمة", "اي خدمة"))
            return "أكيد " + userName + "!\n• الوقت والتاريخ\n• نكت متنوعة\n• نصائح سريعة\n• مسح المحادثة\n• ترقية Premium\n\nأو استخدم الأزرار السريعة فوق.";

        if (has(m, "وقت", "ساعه", "ساعة", "كم الساعه", "كم الساعة", "التوقيت")) {
            Calendar c = Calendar.getInstance();
            return "الوقت التقريبي: " + String.format("%02d:%02d",
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }

        if (has(m, "تاريخ", "اليوم", "كم تاريخ", "أي يوم")) {
            Calendar c = Calendar.getInstance();
            return "تاريخ اليوم: " + c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        }

        if (has(m, "نكتة", "نكته", "نكت", "اضحكني", "joke", "ضحكني")) {
            String[] jokes = {
                    "ليش الكمبيوتر راح للدكتور؟ عشان عنده فيروس! 😄",
                    "المبرمج قال لزوجته: أحبك من 1 إلى 10... فقالت: وأنت؟ قال: من 0 إلى 1 فقط 😅",
                    "ليش الـ WiFi حزين؟ لأنه فقد الـ connection 😢",
                    "دخل بايثون على مطعم... قال له النادل: SyntaxError 😂",
                    "ليش الهاتف ما يضحك؟ لأنه على Silent mode 🤫",
                    "ليش السيرفر ما ينام؟ عنده uptime 24/7 😴",
                    "واحد سأل: كيف أنسخ ملف؟ قال له: Ctrl... بعدين نسي الباقي 😆"
            };
            String joke = jokes[jokeIndex % jokes.length];
            jokeIndex++;
            return joke;
        }

        if (has(m, "نصيحة", "نصيحه", "تحفيز", "تحمسني", "همة"))
            return pick(
                    "ابدأ بخطوة صغيرة اليوم — الاستمرارية أقوى من الحماس.",
                    "خذ راحة 10 دقائق، بعدين ارجع لهدف واحد بس.",
                    "لا تقارن نفسك بالناس. قارن نفسك بأمس."
            );

        if (has(m, "😂", "🤣", "😆", "ضحك", "حلو", "حلوه", "زين", "ممتاز", "رائع"))
            return pick("يسعدني!", "تبي نكتة ثانية؟", "هههه تمام!");

        if (has(m, "حزين", "زعلان", "تعبان", "ضايق", "ممل", "مكركب"))
            return "آسف إنك تحس كذا. تبي نكتة خفيفة ولا نصيحة سريعة؟";

        if (has(m, "nova", "نوفا", "تطبيق", "من صنعك", "مطور"))
            return "NOVA AI نسخة تجريبية محلية قوية. بدون خادم خارجي حالياً، مع واجهة سريعة وأزرار مساعدة.";

        if (has(m, "premium", "بريميوم", "ترقية", "اشتراك"))
            return "المجاني 15 رسالة، Premium التجريبية 200. من زر الترقية. لا يوجد دفع حقيقي بعد.";

        if (has(m, "مسح", "نظف", "حذف المحادثة"))
            return "تقدر تضغط زر «مسح» فوق عشان تفضي المحادثة.";

        return pick(
                "فهمت: «" + message + "»\nمو ضمن قاعدة الردود بعد. جرب الأزرار السريعة فوق، أو اسأل عن وقت/نكتة/نصيحة.",
                "وصلت. هذي نسخة محلية قوية لكن محدودة. جرب: مساعدة، نكتة، نصيحة.",
                "ما عندي رد جاهز بالضبط. أعد صياغة السؤال أو استخدم الأزرار السريعة."
        );
    }

    private boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k.toLowerCase())) return true;
        return false;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private Button smallBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.parseColor("#C4B5FD"));
        b.setBackground(rounded(Color.parseColor("#1C1C32"), 12));
        b.setTextSize(12);
        b.setPadding(dp(8), dp(4), dp(8), dp(4));
        return b;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private void loadMessages() {
        messages.clear();
        String raw = prefs.getString("messages", null);
        if (raw == null) {
            messages.add("NOVA AI: مرحبًا " + (userName.isEmpty() ? "" : userName) + "!\nأنا NOVA AI — استخدم الأزرار السريعة أو اكتب أي شيء.");
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
