package com.novaai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private static final int BG = Color.parseColor("#070B14");
    private static final int SURFACE = Color.parseColor("#0F1623");
    private static final int CARD = Color.parseColor("#151D2E");
    private static final int ACCENT = Color.parseColor("#00E5A8");
    private static final int ACCENT_DIM = Color.parseColor("#0A3D32");
    private static final int USER_BUBBLE = Color.parseColor("#1A2740");
    private static final int BOT_BUBBLE = Color.parseColor("#121A28");
    private static final int TEXT = Color.parseColor("#E8EEF7");
    private static final int MUTED = Color.parseColor("#7A8BA3");

    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 20;
    private static final int PREMIUM_LIMIT = 250;

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
        root.setBackgroundColor(BG);
        setContentView(root);

        if (userName.isEmpty()) showAccountScreen();
        else showChatScreen();
    }

    private void showAccountScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));

        TextView logo = new TextView(this);
        logo.setText("◆ NOVA");
        logo.setTextSize(36);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(ACCENT);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView sub = new TextView(this);
        sub.setText("مساعد محلي أقوى\nردود أوسع • واجهة حديثة");
        sub.setTextSize(15);
        sub.setTextColor(MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(14), 0, dp(32));
        root.addView(sub);

        EditText nameInput = new EditText(this);
        nameInput.setHint("اسمك");
        nameInput.setTextColor(TEXT);
        nameInput.setHintTextColor(MUTED);
        nameInput.setBackground(rounded(CARD, 18));
        nameInput.setPadding(dp(18), dp(16), dp(18), dp(16));
        nameInput.setSingleLine(true);
        root.addView(nameInput, matchWrap());

        Button btn = primaryBtn("ابدأ الآن");
        LinearLayout.LayoutParams bp = matchWrap();
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
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(16), dp(16), dp(12), dp(16));
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        titleCol.addView(title);

        TextView online = new TextView(this);
        online.setText("● محلي • جاهز");
        online.setTextSize(11);
        online.setTextColor(ACCENT);
        titleCol.addView(online);
        top.addView(titleCol);

        Button clearBtn = chipBtn("مسح");
        clearBtn.setOnClickListener(v -> confirmClear());
        top.addView(clearBtn);

        Button plansBtn = chipBtn(premium ? "PRO" : "ترقية");
        plansBtn.setOnClickListener(v -> showPlansScreen());
        top.addView(plansBtn);
        root.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + "   " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + limit);
        status.setTextSize(12);
        status.setTextColor(MUTED);
        status.setPadding(dp(16), dp(10), dp(16), dp(4));
        root.addView(status);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(12), dp(8), dp(12), dp(8));
        String[] quick = {"نكتة", "الوقت", "التاريخ", "نصيحة", "دعاء", "مساعدة"};
        for (String q : quick) {
            Button chip = chipBtn(q);
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
        msgBox.setPadding(dp(12), dp(6), dp(12), dp(10));
        scroll.addView(msgBox);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        refreshMessages(msgBox);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(SURFACE);
        bottom.setPadding(dp(12), dp(12), dp(12), dp(12));
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        EditText input = new EditText(this);
        input.setHint("اكتب رسالتك...");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(CARD, 16));
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        input.setSingleLine(true);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button send = primaryBtn("إرسال");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(dp(10));
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
        TextView statusView = null;
        ScrollView scroll = null;
        LinearLayout msgBox = null;
        Button sendBtn = null;

        for (int i = 0; i < root.getChildCount(); i++) {
            View c = root.getChildAt(i);
            if (c instanceof TextView) {
                String t = ((TextView) c).getText().toString();
                if (t.contains("أهلاً") || t.contains("اهلا")) statusView = (TextView) c;
            }
            if (c instanceof ScrollView) {
                scroll = (ScrollView) c;
                if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof LinearLayout)
                    msgBox = (LinearLayout) scroll.getChildAt(0);
            }
        }
        if (root.getChildCount() > 0) {
            View last = root.getChildAt(root.getChildCount() - 1);
            if (last instanceof LinearLayout) {
                LinearLayout bottom = (LinearLayout) last;
                for (int i = 0; i < bottom.getChildCount(); i++) {
                    if (bottom.getChildAt(i) instanceof Button) {
                        Button b = (Button) bottom.getChildAt(i);
                        if ("إرسال".equals(b.getText().toString()) || "...".equals(b.getText().toString()))
                            sendBtn = b;
                    }
                }
            }
        }

        if (statusView != null && msgBox != null && scroll != null && sendBtn != null)
            doSend(text, statusView, msgBox, scroll, sendBtn);
        else {
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
        status.setText("أهلاً " + userName + "   " + (premium ? "Premium" : "مجاني") + "  " + usage + "/" + lim);
        refreshMessages(msgBox);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        send.setEnabled(false);
        send.setText("...");
        executor.execute(() -> {
            try { Thread.sleep(280 + random.nextInt(450)); } catch (Exception ignored) {}
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
                    messages.add("NOVA AI: تم المسح. كيف أقدر أساعدك " + userName + "؟");
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

        Button back = chipBtn("رجوع");
        back.setOnClickListener(v -> showChatScreen());
        root.addView(back);

        TextView title = new TextView(this);
        title.setText("الخطط");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        title.setPadding(0, dp(20), 0, dp(20));
        root.addView(title);

        root.addView(planCard("مجاني", "20 رسالة • تجربة أساسية", !premium, false));
        LinearLayout.LayoutParams pp = matchWrap();
        pp.topMargin = dp(12);
        root.addView(planCard("Premium", "250 رسالة • تجربة أوسع", premium, true), pp);

        if (!premium) {
            Button activate = primaryBtn("تفعيل Premium للتجربة");
            LinearLayout.LayoutParams ap = matchWrap();
            ap.topMargin = dp(24);
            root.addView(activate, ap);
            activate.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                Toast.makeText(this, "تم تفعيل Premium", Toast.LENGTH_SHORT).show();
                showChatScreen();
            });
        }

        TextView note = new TextView(this);
        note.setText("التفعيل تجريبي — لا يوجد دفع حقيقي.");
        note.setTextSize(12);
        note.setTextColor(MUTED);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note);
    }

    private LinearLayout planCard(String name, String desc, boolean current, boolean pro) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(pro ? ACCENT_DIM : CARD, 18));
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView t = new TextView(this);
        t.setText(name);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(pro ? ACCENT : TEXT);
        card.addView(t);
        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(MUTED);
        d.setPadding(0, dp(8), 0, 0);
        card.addView(d);
        if (current) {
            TextView c = new TextView(this);
            c.setText("✓ خطتك الحالية");
            c.setTextColor(ACCENT);
            c.setPadding(0, dp(10), 0, 0);
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
            tv.setTextColor(TEXT);
            tv.setTextSize(15);
            tv.setBackground(rounded(isUser ? USER_BUBBLE : BOT_BUBBLE, 16));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = matchWrap();
            lp.bottomMargin = dp(10);
            box.addView(tv, lp);
        }
    }

    private String localReply(String message) {
        String m = message.toLowerCase().trim();

        if (has(m, "مع السلامة", "مع السلامه", "باي", "وداع", "bye", "إلى اللقاء", "الى اللقاء"))
            return pick("مع السلامة " + userName + "! يومك سعيد.",
                    "إلى اللقاء " + userName + "! أشوفك قريب.");

        if (m.equals("لا") || m.equals("لأ") || m.equals("لا شكرا") || m.equals("لا شكرًا"))
            return pick("حاضر.", "تمام، أنا هنا إذا احتجت.");

        if (m.equals("نعم") || m.equals("ايوه") || m.equals("أيوه") || m.equals("ايه") || m.equals("يب"))
            return pick("تم!", "تمام، تفضل.");

        if (has(m, "سلام", "هلا", "مرحبا", "مرحباً", "hello", "hi", "صباح", "مساء"))
            return pick("وعليكم السلام " + userName + "! كيف أقدر أساعدك؟",
                    "هلا " + userName + "! تفضل.");

        if (has(m, "كيف حالك", "كيفك", "أخبارك", "اخبارك", "شلونك"))
            return pick("بخير الحمد لله يا " + userName + "! وأنت؟", "تمام وبأفضل حال!");

        if (has(m, "اسمك", "من أنت", "من انت", "وش اسمك", "ايش اسمك"))
            return "أنا NOVA AI — مساعد محلي. أرد على الوقت، التاريخ، النكت، النصائح، الأدعية، والمساعدة.";

        if (has(m, "شكرا", "شكرًا", "شكراً", "مشكور", "thanks", "يسلمو"))
            return pick("العفو!", "ولا يهمك!", "بخدمتك.");

        if (has(m, "تساعد", "مساعدة", "ساعدني", "تقدر", "help", "وش تقدر", "ايش تقدر", "أي خدمة", "اي خدمة", "مشروع"))
            return "أكيد " + userName + "!\n• الوقت والتاريخ\n• نكت\n• نصائح\n• دعاء\n• مسح المحادثة\n• ترقية\n\nللاحتياج الأعمق (برمجة/حسابات معقدة) استخدم أدوات متخصصة أو ذكاء سحابي لاحقًا.";

        if (has(m, "وقت", "ساعه", "ساعة", "كم الساعه", "كم الساعة", "التوقيت")) {
            Calendar c = Calendar.getInstance();
            return "الوقت التقريبي: " + String.format("%02d:%02d",
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }

        if (has(m, "تاريخ", "اليوم", "كم تاريخ", "أي يوم")) {
            Calendar c = Calendar.getInstance();
            String[] days = {"الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
            int day = c.get(Calendar.DAY_OF_WEEK); // 1=Sunday
            return "اليوم " + days[day - 1] + " — التاريخ: "
                    + c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/"
                    + c.get(Calendar.YEAR);
        }

        if (has(m, "نكتة", "نكته", "نكت", "اضحكني", "joke", "ضحكني")) {
            String[] jokes = {
                    "ليش الكمبيوتر راح للدكتور؟ عشان عنده فيروس! 😄",
                    "المبرمج قال لزوجته: أحبك من 1 إلى 10... فقالت: وأنت؟ قال: من 0 إلى 1 فقط 😅",
                    "ليش الـ WiFi حزين؟ لأنه فقد الـ connection 😢",
                    "دخل بايثون على مطعم... قال له النادل: SyntaxError 😂",
                    "ليش الهاتف ما يضحك؟ لأنه على Silent mode 🤫",
                    "ليش السيرفر ما ينام؟ عنده uptime 24/7 😴",
                    "واحد يبي ينسخ ملف... قال: Ctrl... ونسي الباقي 😆"
            };
            String joke = jokes[jokeIndex % jokes.length];
            jokeIndex++;
            return joke;
        }

        if (has(m, "نصيحة", "نصيحه", "تحفيز", "تحمسني", "همة"))
            return pick(
                    "ابدأ بخطوة صغيرة اليوم — الاستمرارية أقوى من الحماس.",
                    "خذ راحة قصيرة، بعدين ركّز على هدف واحد.",
                    "لا تقارن نفسك بالناس. قارن نفسك بأمس.",
                    "النجاح تراكم أيام بسيطة، مو يوم واحد مثالي."
            );

        if (has(m, "دعاء", "ادعية", "اللهم", "ذكر"))
            return pick(
                    "اللهم إني أسألك العافية في الدنيا والآخرة.",
                    "يا مقلب القلوب ثبّت قلبي على دينك.",
                    "اللهم يسّر وأعن ولا تعسّر."
            );

        if (has(m, "😂", "🤣", "😆", "ضحك", "حلو", "حلوه", "زين", "ممتاز", "رائع"))
            return pick("يسعدني!", "تبي نكتة ثانية؟", "هههه تمام!");

        if (has(m, "حزين", "زعلان", "تعبان", "ضايق", "ممل", "مضغوط"))
            return "آسف إنك تحس كذا. جرب: نفس عميق، راحة قصيرة، أو اكتب اللي مضايقك. تبي نصيحة أو دعاء؟";

        if (has(m, "درس", "مذاكرة", "اختبار", "امتحان", "دراسة"))
            return "نصيحة مذاكرة: ركّز 25 دقيقة، ارتاح 5، وكرر. ابدأ بأصعب جزء وأنت مركز.";

        if (has(m, "رياضة", "تمرين", "جيم", "مشي"))
            return "حتى 15–20 دقيقة مشي يوميًا فرق كبير. الثبات أهم من الشدة.";

        if (has(m, "نوم", "سهر", "أرق"))
            return "حاول تثبت وقت نوم، وقلل الشاشة قبل النوم بنصف ساعة. الجوال على صامت يساعد.";

        if (has(m, "nova", "نوفا", "تطبيق", "شكل", "ألوان", "تصميم"))
            return "NOVA AI نسخة محلية قوية: واجهة داكنة، أزرار سريعة، ردود أوسع، بدون خادم حاليًا.";

        if (has(m, "premium", "بريميوم", "ترقية", "اشتراك"))
            return "المجاني 20 رسالة، Premium التجريبية 250. من زر الترقية. لا دفع حقيقي بعد.";

        if (has(m, "طقس", "جو", "حر", "برد", "مطر"))
            return "ما أقدر أعرف الطقس الحقيقي بدون خدمة خارجية. افتح تطبيق الطقس عندك.";

        if (has(m, "أخبار", "خبر"))
            return "ما عندي بث أخبار مباشر. اعتمد مصدر موثوق.";

        // عمليات حسابية بسيطة: 5+3 أو 10-2
        String calc = trySimpleMath(m);
        if (calc != null) return calc;

        return pick(
                "فهمت: «" + message + "»\nجرب: وقت، تاريخ، نكتة، نصيحة، دعاء، أو مساعدة.",
                "وصلت. النسخة المحلية أقوى من قبل لكنها محدودة. استخدم الأزرار السريعة.",
                "ما عندي رد جاهز بالضبط. صغ سؤالك بطريقة ثانية أو اضغط مساعدة."
        );
    }

    private String trySimpleMath(String m) {
        try {
            m = m.replace(" ", "").replace("؟", "").replace("?", "");
            if (m.contains("+")) {
                String[] p = m.split("\\+");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) + Double.parseDouble(p[1]));
            }
            if (m.contains("-") && !m.startsWith("-")) {
                String[] p = m.split("-");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) - Double.parseDouble(p[1]));
            }
            if (m.contains("×") || m.contains("*") || m.contains("x")) {
                String[] p = m.split("[×*x]");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) * Double.parseDouble(p[1]));
            }
            if (m.contains("÷") || m.contains("/")) {
                String[] p = m.split("[÷/]");
                if (p.length == 2) {
                    double b = Double.parseDouble(p[1]);
                    if (b == 0) return "ما ينفع القسمة على صفر.";
                    return "النتيجة: " + (Double.parseDouble(p[0]) / b);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k.toLowerCase())) return true;
        return false;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }

    private Button primaryBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.parseColor("#04120E"));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(ACCENT, 16));
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        return b;
    }

    private Button chipBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(ACCENT);
        b.setTextSize(12);
        b.setBackground(rounded(CARD, 14));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void loadMessages() {
        messages.clear();
        String raw = prefs.getString("messages", null);
        if (raw == null) {
            messages.add("NOVA AI: أهلًا" + (userName.isEmpty() ? "!" : " " + userName + "!") + "\nجاهز. جرب الأزرار أو اكتب.");
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
