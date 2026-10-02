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
import android.os.Vibrator;
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
    private static final int FREE_LIMIT = 30;
    private static final int PREMIUM_LIMIT = 400;

    private SharedPreferences prefs;
    private String userName = "";
    private boolean premium = false;
    private int usage = 0;
    private int totalSent = 0;
    private int jokeIndex = 0;
    private int tab = 0; // 0 chat 1 tools 2 about
    private ArrayList<String> messages = new ArrayList<>();

    private LinearLayout root;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    // calculator state
    private String calcExpr = "";
    private TextView calcDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        userName = prefs.getString("user_name", "");
        premium = prefs.getBoolean("premium", false);
        usage = prefs.getInt("usage", 0);
        totalSent = prefs.getInt("total_sent", 0);
        loadMessages();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        if (userName.isEmpty()) showAccountScreen();
        else showMain();
    }

    private void showAccountScreen() {
        root.removeAllViews();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));

        TextView logo = new TextView(this);
        logo.setText("◆ NOVA");
        logo.setTextSize(40);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(ACCENT);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView sub = new TextView(this);
        sub.setText("محادثة + أدوات + إحصائيات\nأقوى نسخة محلية");
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

        Button btn = primaryBtn("ابدأ");
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
            showMain();
        });
    }

    private void showMain() {
        root.removeAllViews();
        root.setGravity(Gravity.TOP);
        root.setPadding(0, 0, 0, 0);

        // content
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(content);

        if (tab == 0) buildChat(content);
        else if (tab == 1) buildTools(content);
        else buildAbout(content);

        // bottom nav
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(SURFACE);
        nav.setPadding(dp(8), dp(10), dp(8), dp(10));
        nav.setGravity(Gravity.CENTER);

        nav.addView(navBtn("محادثة", 0), navWeight());
        nav.addView(navBtn("أدوات", 1), navWeight());
        nav.addView(navBtn("حولي", 2), navWeight());
        root.addView(nav);
    }

    private LinearLayout.LayoutParams navWeight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private Button navBtn(String label, int id) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(tab == id ? Color.parseColor("#04120E") : MUTED);
        b.setBackground(rounded(tab == id ? ACCENT : CARD, 14));
        b.setOnClickListener(v -> {
            tab = id;
            showMain();
        });
        return b;
    }

    // ================= CHAT =================
    private void buildChat(LinearLayout content) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(14), dp(14), dp(10), dp(14));
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
        content.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + "  |  " + (premium ? "Pro" : "مجاني") + " " + usage + "/" + limit);
        status.setTextSize(12);
        status.setTextColor(MUTED);
        status.setPadding(dp(16), dp(8), dp(16), dp(4));
        content.addView(status);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(12), dp(6), dp(12), dp(6));
        String[] quick = {"نكتة", "الوقت", "التاريخ", "نصيحة", "دعاء", "مساعدة"};
        for (String q : quick) {
            Button chip = chipBtn(q);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMarginEnd(dp(8));
            chips.addView(chip, cp);
            chip.setOnClickListener(v -> sendQuickFromChat(q, content));
        }
        chipsScroll.addView(chips);
        content.addView(chipsScroll);

        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(12), dp(6), dp(12), dp(8));
        scroll.addView(msgBox);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        refreshMessages(msgBox);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(SURFACE);
        bottom.setPadding(dp(12), dp(10), dp(12), dp(10));
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        EditText input = new EditText(this);
        input.setHint("اكتب...");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(CARD, 16));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button send = primaryBtn("إرسال");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(dp(8));
        bottom.addView(send, sp);
        content.addView(bottom);

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            doSend(text, status, msgBox, scroll, send);
        });
    }

    private void sendQuickFromChat(String text, LinearLayout content) {
        // rebuild is heavy; find views
        TextView status = null;
        ScrollView scroll = null;
        LinearLayout msgBox = null;
        Button sendBtn = null;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (c instanceof TextView) {
                String t = ((TextView) c).getText().toString();
                if (t.contains("أهلاً") || t.contains("اهلا")) status = (TextView) c;
            }
            if (c instanceof ScrollView) {
                scroll = (ScrollView) c;
                if (scroll.getChildCount() > 0 && scroll.getChildAt(0) instanceof LinearLayout)
                    msgBox = (LinearLayout) scroll.getChildAt(0);
            }
            if (c instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) c;
                for (int j = 0; j < row.getChildCount(); j++) {
                    if (row.getChildAt(j) instanceof Button) {
                        Button b = (Button) row.getChildAt(j);
                        if ("إرسال".equals(b.getText().toString()) || "...".equals(b.getText().toString()))
                            sendBtn = b;
                    }
                }
            }
        }
        if (status != null && msgBox != null && scroll != null && sendBtn != null)
            doSend(text, status, msgBox, scroll, sendBtn);
        else {
            messages.add("أنت: " + text);
            messages.add("NOVA AI: " + localReply(text));
            saveMessages();
            showMain();
        }
    }

    private void doSend(String text, TextView status, LinearLayout msgBox, ScrollView scroll, Button send) {
        int lim = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        if (usage >= lim) {
            Toast.makeText(this, "انتهت الرسائل", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(25);
        } catch (Exception ignored) {}

        messages.add("أنت: " + text);
        usage++;
        totalSent++;
        prefs.edit().putInt("usage", usage).putInt("total_sent", totalSent).apply();
        saveMessages();
        status.setText("أهلاً " + userName + "  |  " + (premium ? "Pro" : "مجاني") + " " + usage + "/" + lim);
        refreshMessages(msgBox);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        send.setEnabled(false);
        send.setText("...");
        executor.execute(() -> {
            try { Thread.sleep(220 + random.nextInt(350)); } catch (Exception ignored) {}
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

    // ================= TOOLS =================
    private void buildTools(LinearLayout content) {
        TextView h = new TextView(this);
        h.setText("الأدوات");
        h.setTextSize(24);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(TEXT);
        h.setPadding(dp(16), dp(18), dp(16), dp(12));
        content.addView(h);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(16));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Calculator
        TextView calcTitle = section("آلة حاسبة");
        box.addView(calcTitle);

        calcDisplay = new TextView(this);
        calcDisplay.setText(calcExpr.isEmpty() ? "0" : calcExpr);
        calcDisplay.setTextSize(28);
        calcDisplay.setTextColor(TEXT);
        calcDisplay.setBackground(rounded(CARD, 14));
        calcDisplay.setPadding(dp(16), dp(16), dp(16), dp(16));
        calcDisplay.setGravity(Gravity.END);
        LinearLayout.LayoutParams dp0 = matchWrap();
        dp0.bottomMargin = dp(10);
        box.addView(calcDisplay, dp0);

        String[][] rows = {
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "-"},
                {"0", ".", "C", "+"},
                {"="}
        };
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setPadding(0, 0, 0, dp(8));
            for (String key : row) {
                Button b = chipBtn(key);
                b.setTextColor(key.equals("=") || key.equals("C") ? ACCENT : TEXT);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                lp.setMarginEnd(dp(6));
                r.addView(b, lp);
                b.setOnClickListener(v -> onCalcKey(key));
            }
            box.addView(r);
        }

        // Coin + Dice
        box.addView(section("عشوائي"));
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button coin = primaryBtn("عملة");
        Button dice = primaryBtn("نرد");
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        hp.setMarginEnd(dp(8));
        row2.addView(coin, hp);
        row2.addView(dice, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(row2);

        TextView result = new TextView(this);
        result.setText("اضغط عملة أو نرد");
        result.setTextColor(MUTED);
        result.setTextSize(16);
        result.setPadding(0, dp(14), 0, 0);
        result.setGravity(Gravity.CENTER);
        box.addView(result);

        coin.setOnClickListener(v -> result.setText(random.nextBoolean() ? "ملك" : "كتابة"));
        dice.setOnClickListener(v -> result.setText("النرد: " + (random.nextInt(6) + 1)));
    }

    private void onCalcKey(String key) {
        if (key.equals("C")) {
            calcExpr = "";
        } else if (key.equals("=")) {
            String res = trySimpleMath(calcExpr);
            calcExpr = res != null ? res.replace("النتيجة: ", "") : "خطأ";
        } else {
            if (calcExpr.equals("خطأ") || calcExpr.equals("0")) calcExpr = "";
            calcExpr += key;
        }
        if (calcDisplay != null) calcDisplay.setText(calcExpr.isEmpty() ? "0" : calcExpr);
    }

    // ================= ABOUT =================
    private void buildAbout(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        TextView h = new TextView(this);
        h.setText("حولي");
        h.setTextSize(26);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(TEXT);
        box.addView(h);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setBackground(rounded(CARD, 16));
        stats.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(16);
        TextView st = new TextView(this);
        st.setText("إحصائيات");
        st.setTextColor(ACCENT);
        st.setTypeface(Typeface.DEFAULT_BOLD);
        stats.addView(st);
        TextView sd = new TextView(this);
        sd.setText("الاسم: " + userName + "\nالرسائل هذه الجلسة: " + usage + "\nالإجمالي: " + totalSent + "\nالخطة: " + (premium ? "Premium" : "مجاني"));
        sd.setTextColor(MUTED);
        sd.setPadding(0, dp(8), 0, 0);
        stats.addView(sd);
        box.addView(stats, sp);

        LinearLayout about = new LinearLayout(this);
        about.setOrientation(LinearLayout.VERTICAL);
        about.setBackground(rounded(CARD, 16));
        about.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams ap = matchWrap();
        ap.topMargin = dp(12);
        TextView at = new TextView(this);
        at.setText("عن NOVA AI");
        at.setTextColor(ACCENT);
        at.setTypeface(Typeface.DEFAULT_BOLD);
        about.addView(at);
        TextView ad = new TextView(this);
        ad.setText("نسخة محلية قوية من الجوال:\n• محادثة بردود محلية\n• آلة حاسبة وعملة ونرد\n• إحصائيات وخطط\n\nالذكاء السحابي الكامل يحتاج API لاحقًا.");
        ad.setTextColor(MUTED);
        ad.setPadding(0, dp(8), 0, 0);
        about.addView(ad);
        box.addView(about, ap);

        Button nameBtn = primaryBtn("تغيير الاسم");
        LinearLayout.LayoutParams np = matchWrap();
        np.topMargin = dp(16);
        box.addView(nameBtn, np);
        nameBtn.setOnClickListener(v -> changeName());

        if (!premium) {
            Button pro = primaryBtn("تفعيل Premium تجريبي");
            LinearLayout.LayoutParams pp = matchWrap();
            pp.topMargin = dp(10);
            box.addView(pro, pp);
            pro.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                Toast.makeText(this, "تم تفعيل Premium", Toast.LENGTH_SHORT).show();
                showMain();
            });
        }

        TextView ver = new TextView(this);
        ver.setText("NOVA AI v1.1 محلي");
        ver.setTextColor(MUTED);
        ver.setTextSize(12);
        ver.setPadding(0, dp(20), 0, 0);
        ver.setGravity(Gravity.CENTER);
        box.addView(ver);
    }

    private void changeName() {
        final EditText input = new EditText(this);
        input.setText(userName);
        input.setTextColor(TEXT);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        new AlertDialog.Builder(this)
                .setTitle("تغيير الاسم")
                .setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) {
                        userName = n;
                        prefs.edit().putString("user_name", userName).apply();
                        showMain();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("مسح")
                .setMessage("مسح المحادثة؟")
                .setPositiveButton("مسح", (d, w) -> {
                    messages.clear();
                    messages.add("NOVA AI: تم المسح. " + tipOfDay());
                    saveMessages();
                    showMain();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private String tipOfDay() {
        String[] tips = {
                "ركّز 25 دقيقة على مهمة واحدة.",
                "اشرب ماء وخذ استراحة قصيرة.",
                "خطوة صغيرة كل يوم تصنع فرق."
        };
        return tips[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % tips.length];
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
            return "مع السلامة " + userName + "!";
        if (m.equals("لا") || m.equals("لأ")) return "حاضر.";
        if (m.equals("نعم") || m.equals("ايوه") || m.equals("أيوه") || m.equals("يب")) return "تمام.";
        if (has(m, "سلام", "هلا", "مرحبا", "hello", "hi", "صباح", "مساء"))
            return "وعليكم السلام " + userName + "!";
        if (has(m, "كيف حالك", "كيفك", "شلونك")) return "بخير الحمد لله! وأنت؟";
        if (has(m, "اسمك", "من أنت", "من انت", "وش اسمك", "ايش اسمك"))
            return "أنا NOVA AI — محادثة + أدوات + إحصائيات.";
        if (has(m, "شكرا", "شكرًا", "مشكور", "thanks")) return "العفو!";
        if (has(m, "تساعد", "مساعدة", "ساعدني", "help", "مشروع"))
            return "أكيد!\n• تبويب محادثة\n• تبويب أدوات (حاسبة/عملة/نرد)\n• تبويب حولي\nاكتب أو استخدم الأزرار.";
        if (has(m, "وقت", "ساعه", "ساعة", "كم الساعه", "كم الساعة")) {
            Calendar c = Calendar.getInstance();
            return "الوقت: " + String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
        if (has(m, "تاريخ", "اليوم", "كم تاريخ")) {
            Calendar c = Calendar.getInstance();
            String[] days = {"الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};
            return days[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + "/"
                    + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        }
        if (has(m, "نكتة", "نكته", "اضحكني", "joke")) {
            String[] jokes = {
                    "ليش الكمبيوتر راح للدكتور؟ عنده فيروس! 😄",
                    "بايثون دخل مطعم... SyntaxError 😂",
                    "الواي فاي حزين؟ فقد الاتصال 😢"
            };
            String j = jokes[jokeIndex % jokes.length];
            jokeIndex++;
            return j;
        }
        if (has(m, "نصيحة", "نصيحه", "تحفيز")) return tipOfDay();
        if (has(m, "دعاء", "اللهم")) return pick("اللهم يسّر وأعن.", "اللهم إني أسألك العافية.");
        if (has(m, "😂", "ضحك", "ممتاز", "حلو")) return "يسعدني!";
        if (has(m, "أدوات", "حاسبة", "نرد", "عملة")) return "من الأسفل افتح تبويب أدوات.";

        String calc = trySimpleMath(m);
        if (calc != null) return calc;

        return "فهمت: «" + message + "»\nجرب الأزرار أو تبويب الأدوات.";
    }

    private String trySimpleMath(String m) {
        try {
            m = m.replace(" ", "").replace("؟", "").replace("?", "");
            m = m.replace("÷", "/").replace("×", "*");
            if (m.contains("+")) {
                String[] p = m.split("\\+");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) + Double.parseDouble(p[1]));
            }
            if (m.contains("-") && m.indexOf('-') > 0) {
                String[] p = m.split("-");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) - Double.parseDouble(p[1]));
            }
            if (m.contains("*")) {
                String[] p = m.split("\\*");
                if (p.length == 2) return "النتيجة: " + (Double.parseDouble(p[0]) * Double.parseDouble(p[1]));
            }
            if (m.contains("/")) {
                String[] p = m.split("/");
                if (p.length == 2) {
                    double b = Double.parseDouble(p[1]);
                    if (b == 0) return "لا قسمة على صفر";
                    return "النتيجة: " + (Double.parseDouble(p[0]) / b);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private TextView section(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(ACCENT);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(16);
        v.setPadding(0, dp(12), 0, dp(8));
        return v;
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
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        return b;
    }

    private Button chipBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(ACCENT);
        b.setTextSize(12);
        b.setBackground(rounded(CARD, 14));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
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
            messages.add("NOVA AI: أهلًا " + userName + "!\n3 تبويبات: محادثة • أدوات • حولي");
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
        } catch (Exception ignored) {}
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
