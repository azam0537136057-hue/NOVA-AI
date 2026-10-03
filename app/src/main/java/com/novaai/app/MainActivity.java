package com.novaai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private int BG, SURFACE, CARD, ACCENT, USER_BUBBLE, BOT_BUBBLE, TEXT, MUTED;
    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 50;
    private static final int PREMIUM_LIMIT = 600;

    private SharedPreferences prefs;
    private String userName = "";
    private boolean premium = false;
    private int usage = 0, totalSent = 0, jokeIndex = 0, tab = 0, themeId = 0;
    private int tasbih = 0, water = 0, azkarIdx = 0;

    // محادثات متعددة
    private static class Chat {
        String id, title;
        ArrayList<String> messages = new ArrayList<>();
        Chat(String id, String title) { this.id = id; this.title = title; }
    }
    private ArrayList<Chat> chats = new ArrayList<>();
    private String currentChatId = "";

    private LinearLayout root;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private String calcExpr = "";
    private TextView calcDisplay, tasbihText, waterText, azkarText, swText;
    private long swStart = 0;
    private boolean swRunning = false;
    private final Runnable swTick = new Runnable() {
        public void run() {
            if (swRunning && swText != null) {
                long ms = System.currentTimeMillis() - swStart;
                int s = (int) (ms / 1000), m = s / 60; s %= 60;
                int cs = (int) ((ms % 1000) / 10);
                swText.setText(String.format("%02d:%02d.%02d", m, s, cs));
                mainHandler.postDelayed(this, 50);
            }
        }
    };

    private static final String[] AZKAR_MORNING = {
            "أصبحنا وأصبح الملك لله، والحمد لله",
            "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور",
            "سبحان الله وبحمده",
            "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير"
    };
    private static final String[] AZKAR_EVENING = {
            "أمسينا وأمسى الملك لله، والحمد لله",
            "اللهم بك أمسينا وبك أصبحنا وبك نحيا وبك نموت وإليك المصير",
            "أعوذ بكلمات الله التامات من شر ما خلق",
            "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء"
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        userName = prefs.getString("user_name", "");
        premium = prefs.getBoolean("premium", false);
        usage = prefs.getInt("usage", 0);
        totalSent = prefs.getInt("total_sent", 0);
        themeId = prefs.getInt("theme_id", 0);
        tasbih = prefs.getInt("tasbih", 0);
        water = prefs.getInt("water", 0);
        applyTheme();
        loadChats();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        if (userName.isEmpty()) showAccount();
        else showMain();
    }

    private void applyTheme() {
        TEXT = Color.parseColor("#E8EEF7");
        MUTED = Color.parseColor("#7A8BA3");
        if (themeId == 1) {
            BG = Color.parseColor("#070B16"); SURFACE = Color.parseColor("#0E1524"); CARD = Color.parseColor("#152033");
            ACCENT = Color.parseColor("#4DA3FF"); USER_BUBBLE = Color.parseColor("#1A2A45"); BOT_BUBBLE = Color.parseColor("#121C2C");
        } else if (themeId == 2) {
            BG = Color.parseColor("#0B0714"); SURFACE = Color.parseColor("#140F1F"); CARD = Color.parseColor("#1C152E");
            ACCENT = Color.parseColor("#B794F6"); USER_BUBBLE = Color.parseColor("#2A2040"); BOT_BUBBLE = Color.parseColor("#18122A");
        } else {
            BG = Color.parseColor("#070B14"); SURFACE = Color.parseColor("#0F1623"); CARD = Color.parseColor("#151D2E");
            ACCENT = Color.parseColor("#00E5A8"); USER_BUBBLE = Color.parseColor("#1A2740"); BOT_BUBBLE = Color.parseColor("#121A28");
        }
    }

    // ——— محادثات متعددة ———
    private Chat currentChat() {
        for (Chat c : chats) if (c.id.equals(currentChatId)) return c;
        if (!chats.isEmpty()) {
            currentChatId = chats.get(0).id;
            return chats.get(0);
        }
        return null;
    }

    private void ensureChat() {
        if (chats.isEmpty()) {
            Chat c = new Chat(UUID.randomUUID().toString(), "محادثة 1");
            c.messages.add("NOVA AI: أهلًا " + userName + ".\nهذه محادثة جديدة.");
            chats.add(c);
            currentChatId = c.id;
            saveChats();
        } else if (currentChat() == null) {
            currentChatId = chats.get(0).id;
        }
    }

    private void newChat() {
        int n = chats.size() + 1;
        Chat c = new Chat(UUID.randomUUID().toString(), "محادثة " + n);
        c.messages.add("NOVA AI: محادثة جديدة #" + n);
        chats.add(0, c);
        currentChatId = c.id;
        saveChats();
        showMain();
    }

    private void showChatList() {
        ensureChat();
        CharSequence[] items = new CharSequence[chats.size()];
        for (int i = 0; i < chats.size(); i++) {
            Chat c = chats.get(i);
            String mark = c.id.equals(currentChatId) ? "● " : "○ ";
            items[i] = mark + c.title + " (" + c.messages.size() + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("المحادثات")
                .setItems(items, (d, which) -> {
                    currentChatId = chats.get(which).id;
                    saveChats();
                    showMain();
                })
                .setPositiveButton("محادثة جديدة", (d, w) -> newChat())
                .setNeutralButton("حذف الحالية", (d, w) -> deleteCurrentChat())
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void deleteCurrentChat() {
        if (chats.size() <= 1) {
            Toast.makeText(this, "لازم تبقى محادثة واحدة على الأقل", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < chats.size(); i++) {
            if (chats.get(i).id.equals(currentChatId)) {
                chats.remove(i);
                break;
            }
        }
        currentChatId = chats.get(0).id;
        saveChats();
        showMain();
    }

    private void loadChats() {
        chats.clear();
        String raw = prefs.getString("chats_v2", null);
        currentChatId = prefs.getString("current_chat_id", "");
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    Chat c = new Chat(o.getString("id"), o.optString("title", "محادثة"));
                    JSONArray msgs = o.optJSONArray("messages");
                    if (msgs != null) for (int j = 0; j < msgs.length(); j++) c.messages.add(msgs.optString(j));
                    chats.add(c);
                }
            } catch (Exception ignored) {
            }
        }
        // ترحيل من النسخة القديمة إن وجدت
        if (chats.isEmpty()) {
            String old = prefs.getString("messages", null);
            Chat c = new Chat(UUID.randomUUID().toString(), "محادثة 1");
            if (old != null) {
                try {
                    JSONArray a = new JSONArray(old);
                    for (int i = 0; i < a.length(); i++) c.messages.add(a.optString(i));
                } catch (Exception e) {
                    c.messages.add("NOVA AI: أهلًا!");
                }
            } else {
                c.messages.add("NOVA AI: أهلًا " + userName + ".\nمحادثات متعددة جاهزة.");
            }
            chats.add(c);
            currentChatId = c.id;
            saveChats();
        }
        ensureChat();
    }

    private void saveChats() {
        try {
            JSONArray arr = new JSONArray();
            for (Chat c : chats) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("title", c.title);
                JSONArray msgs = new JSONArray();
                for (String m : c.messages) msgs.put(m);
                o.put("messages", msgs);
                arr.put(o);
            }
            prefs.edit()
                    .putString("chats_v2", arr.toString())
                    .putString("current_chat_id", currentChatId)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void showAccount() {
        root.removeAllViews();
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(32), dp(32), dp(32), dp(32));
        TextView logo = new TextView(this);
        logo.setText("◆ NOVA");
        logo.setTextSize(40);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(ACCENT);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);
        TextView sub = new TextView(this);
        sub.setText("محادثات متعددة");
        sub.setTextColor(MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(10), 0, dp(28));
        root.addView(sub);
        EditText nameInput = new EditText(this);
        nameInput.setHint("اسمك");
        nameInput.setTextColor(TEXT);
        nameInput.setHintTextColor(MUTED);
        nameInput.setBackground(rounded(CARD, 16));
        nameInput.setPadding(dp(18), dp(16), dp(18), dp(16));
        nameInput.setSingleLine(true);
        root.addView(nameInput, matchWrap());
        Button btn = primaryBtn("ابدأ");
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(16);
        root.addView(btn, bp);
        btn.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) {
                Toast.makeText(this, "اكتب اسمك", Toast.LENGTH_SHORT).show();
                return;
            }
            userName = n;
            prefs.edit().putString("user_name", userName).apply();
            showMain();
        });
    }

    private void showMain() {
        root.removeAllViews();
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.TOP);
        root.setPadding(0, 0, 0, 0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(content);
        if (tab == 0) buildChat(content);
        else if (tab == 1) buildTools(content);
        else if (tab == 2) buildNotes(content);
        else buildAbout(content);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(SURFACE);
        nav.setPadding(dp(10), dp(10), dp(10), dp(10));
        nav.addView(navBtn("محادثة", 0), navWeight());
        nav.addView(navBtn("أدوات", 1), navWeight());
        nav.addView(navBtn("ملاحظات", 2), navWeight());
        nav.addView(navBtn("حولي", 3), navWeight());
        root.addView(nav);
    }

    private LinearLayout.LayoutParams navWeight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private Button navBtn(String label, int id) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(tab == id ? Color.parseColor("#04120E") : MUTED);
        b.setBackground(rounded(tab == id ? ACCENT : Color.TRANSPARENT, 12));
        b.setOnClickListener(v -> {
            tab = id;
            showMain();
        });
        return b;
    }

    private void buildChat(LinearLayout content) {
        ensureChat();
        Chat chat = currentChat();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(12), dp(12), dp(10), dp(12));
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button chatsBtn = chipBtn("📋");
        chatsBtn.setOnClickListener(v -> showChatList());
        top.addView(chatsBtn);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        col.setPadding(dp(10), 0, dp(6), 0);
        TextView title = new TextView(this);
        title.setText(chat != null ? chat.title : "محادثة");
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        col.addView(title);
        TextView online = new TextView(this);
        online.setText("● محلي · " + chats.size() + " محادثة");
        online.setTextSize(11);
        online.setTextColor(ACCENT);
        col.addView(online);
        top.addView(col);

        Button newBtn = chipBtn("+");
        newBtn.setOnClickListener(v -> newChat());
        top.addView(newBtn);
        content.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText(userName + "  ·  " + (premium ? "Pro" : "مجاني") + "  " + usage + "/" + limit);
        status.setTextSize(12);
        status.setTextColor(MUTED);
        status.setPadding(dp(16), dp(8), dp(16), dp(4));
        content.addView(status);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(10), dp(4), dp(10), dp(8));
        String[] quick = {"نكتة", "أذكار", "نصيحة", "مساعدة"};
        for (String q : quick) {
            Button chip = chipBtn(q);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            cp.setMargins(dp(3), 0, dp(3), 0);
            chips.addView(chip, cp);
            chip.setOnClickListener(v -> sendQuick(q, content));
        }
        content.addView(chips);

        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(14), dp(6), dp(14), dp(8));
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
        input.setHint("اكتب رسالة...");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(CARD, 14));
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
            String t = input.getText().toString().trim();
            if (t.isEmpty()) return;
            input.setText("");
            doSend(t, status, msgBox, scroll, send);
        });
    }

    private void sendQuick(String text, LinearLayout content) {
        TextView status = null;
        ScrollView scroll = null;
        LinearLayout msgBox = null;
        Button sendBtn = null;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (c instanceof TextView) {
                String t = ((TextView) c).getText().toString();
                if (t.contains("Pro") || t.contains("مجاني")) status = (TextView) c;
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
            Chat chat = currentChat();
            if (chat != null) {
                chat.messages.add("أنت: " + text);
                chat.messages.add("NOVA AI: " + localReply(text));
                saveChats();
            }
            showMain();
        }
    }

    private void doSend(String text, TextView status, LinearLayout msgBox, ScrollView scroll, Button send) {
        int lim = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        if (usage >= lim) {
            Toast.makeText(this, "انتهت الرسائل", Toast.LENGTH_SHORT).show();
            return;
        }
        Chat chat = currentChat();
        if (chat == null) return;
        try {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(12);
        } catch (Exception ignored) {
        }
        chat.messages.add("أنت: " + text);
        // تحديث عنوان المحادثة من أول رسالة مستخدم
        if (chat.messages.size() <= 3 && chat.title.startsWith("محادثة")) {
            String t = text.length() > 22 ? text.substring(0, 22) + "…" : text;
            chat.title = t;
        }
        usage++;
        totalSent++;
        prefs.edit().putInt("usage", usage).putInt("total_sent", totalSent).apply();
        saveChats();
        status.setText(userName + "  ·  " + (premium ? "Pro" : "مجاني") + "  " + usage + "/" + lim);
        refreshMessages(msgBox);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        send.setEnabled(false);
        send.setText("...");
        executor.execute(() -> {
            try {
                Thread.sleep(150 + random.nextInt(200));
            } catch (Exception ignored) {
            }
            String reply = localReply(text);
            mainHandler.post(() -> {
                Chat ch = currentChat();
                if (ch != null) {
                    ch.messages.add("NOVA AI: " + reply);
                    saveChats();
                }
                refreshMessages(msgBox);
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                send.setEnabled(true);
                send.setText("إرسال");
            });
        });
    }

    private void refreshMessages(LinearLayout box) {
        box.removeAllViews();
        Chat chat = currentChat();
        if (chat == null) return;
        for (String m : chat.messages) {
            boolean isUser = m.startsWith("أنت:");
            TextView tv = new TextView(this);
            tv.setText(m);
            tv.setTextColor(TEXT);
            tv.setTextSize(15);
            tv.setBackground(rounded(isUser ? USER_BUBBLE : BOT_BUBBLE, 14));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = matchWrap();
            lp.bottomMargin = dp(8);
            box.addView(tv, lp);
        }
    }

    private void buildTools(LinearLayout content) {
        content.addView(headerBar("الأدوات"));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(20));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        half.setMarginEnd(dp(8));

        box.addView(section("أذكار"));
        azkarText = new TextView(this);
        azkarText.setText(AZKAR_MORNING[0]);
        azkarText.setTextColor(TEXT);
        azkarText.setTextSize(15);
        azkarText.setBackground(rounded(CARD, 14));
        azkarText.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.addView(azkarText, matchWrap());
        LinearLayout aRow = new LinearLayout(this);
        aRow.setOrientation(LinearLayout.HORIZONTAL);
        aRow.setPadding(0, dp(8), 0, 0);
        Button aM = primaryBtn("صباح");
        Button aE = primaryBtn("مساء");
        Button aN = chipBtn("التالي");
        aRow.addView(aM, half);
        aRow.addView(aE, half);
        aRow.addView(aN, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(aRow);
        final boolean[] evening = {false};
        aM.setOnClickListener(v -> { evening[0] = false; azkarIdx = 0; azkarText.setText(AZKAR_MORNING[0]); });
        aE.setOnClickListener(v -> { evening[0] = true; azkarIdx = 0; azkarText.setText(AZKAR_EVENING[0]); });
        aN.setOnClickListener(v -> {
            String[] arr = evening[0] ? AZKAR_EVENING : AZKAR_MORNING;
            azkarIdx = (azkarIdx + 1) % arr.length;
            azkarText.setText(arr[azkarIdx]);
        });

        box.addView(section("مسبحة وماء"));
        LinearLayout counters = new LinearLayout(this);
        counters.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout tasbihBox = cardBox();
        tasbihText = bigNum(String.valueOf(tasbih), ACCENT);
        tasbihBox.addView(tasbihText);
        tasbihBox.addView(smallLabel("تسبيح"));
        Button tPlus = primaryBtn("+");
        tasbihBox.addView(tPlus);
        tPlus.setOnClickListener(v -> {
            tasbih++;
            prefs.edit().putInt("tasbih", tasbih).apply();
            tasbihText.setText(String.valueOf(tasbih));
            vibrate();
        });
        LinearLayout waterBox = cardBox();
        waterText = bigNum(water + "/8", TEXT);
        waterBox.addView(waterText);
        waterBox.addView(smallLabel("ماء"));
        Button wPlus = primaryBtn("+");
        waterBox.addView(wPlus);
        wPlus.setOnClickListener(v -> {
            water++;
            prefs.edit().putInt("water", water).apply();
            waterText.setText(water + "/8");
            if (water >= 8) Toast.makeText(this, "أحسنت!", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        c1.setMarginEnd(dp(8));
        counters.addView(tasbihBox, c1);
        counters.addView(waterBox, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(counters);

        box.addView(section("حاسبة"));
        calcDisplay = new TextView(this);
        calcDisplay.setText(calcExpr.isEmpty() ? "0" : calcExpr);
        calcDisplay.setTextSize(26);
        calcDisplay.setTextColor(TEXT);
        calcDisplay.setBackground(rounded(CARD, 12));
        calcDisplay.setPadding(dp(16), dp(14), dp(16), dp(14));
        calcDisplay.setGravity(Gravity.END);
        box.addView(calcDisplay, matchWrap());
        String[][] rows = {{"7", "8", "9", "÷"}, {"4", "5", "6", "×"}, {"1", "2", "3", "-"}, {"0", ".", "C", "+"}, {"="}};
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setPadding(0, dp(5), 0, 0);
            for (String key : row) {
                Button b = chipBtn(key);
                b.setTextColor(TEXT);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                lp.setMarginEnd(dp(5));
                r.addView(b, lp);
                b.setOnClickListener(v -> onCalcKey(key));
            }
            box.addView(r);
        }

        box.addView(section("BMI وحرارة"));
        EditText weight = field("الوزن كجم");
        EditText height = field("الطول سم");
        box.addView(weight);
        box.addView(height);
        TextView bmiOut = new TextView(this);
        bmiOut.setTextColor(TEXT);
        bmiOut.setPadding(0, dp(6), 0, 0);
        box.addView(bmiOut);
        Button bmiBtn = primaryBtn("احسب BMI");
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(8);
        box.addView(bmiBtn, bp);
        bmiBtn.setOnClickListener(v -> {
            try {
                double w = Double.parseDouble(weight.getText().toString().trim());
                double h = Double.parseDouble(height.getText().toString().trim()) / 100.0;
                double bmi = w / (h * h);
                String cat = bmi < 18.5 ? "نحافة" : bmi < 25 ? "طبيعي" : bmi < 30 ? "زيادة وزن" : "سمنة";
                bmiOut.setText(String.format("BMI = %.1f — %s", bmi, cat));
            } catch (Exception e) {
                bmiOut.setText("أرقام غير صحيحة");
            }
        });
        EditText tempIn = field("حرارة");
        box.addView(tempIn);
        TextView tempOut = new TextView(this);
        tempOut.setTextColor(TEXT);
        tempOut.setPadding(0, dp(6), 0, 0);
        box.addView(tempOut);
        LinearLayout trow = new LinearLayout(this);
        trow.setOrientation(LinearLayout.HORIZONTAL);
        trow.setPadding(0, dp(6), 0, 0);
        Button toF = primaryBtn("→ F");
        Button toC = primaryBtn("→ C");
        trow.addView(toF, half);
        trow.addView(toC, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(trow);
        toF.setOnClickListener(v -> {
            try {
                double c = Double.parseDouble(tempIn.getText().toString().trim());
                tempOut.setText(String.format("%.1f °F", c * 9 / 5 + 32));
            } catch (Exception e) {
                tempOut.setText("خطأ");
            }
        });
        toC.setOnClickListener(v -> {
            try {
                double f = Double.parseDouble(tempIn.getText().toString().trim());
                tempOut.setText(String.format("%.1f °C", (f - 32) * 5 / 9));
            } catch (Exception e) {
                tempOut.setText("خطأ");
            }
        });

        box.addView(section("ساعة إيقاف"));
        swText = new TextView(this);
        swText.setText("00:00.00");
        swText.setTextSize(28);
        swText.setTextColor(TEXT);
        swText.setGravity(Gravity.CENTER);
        swText.setBackground(rounded(CARD, 12));
        swText.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.addView(swText, matchWrap());
        LinearLayout swRow = new LinearLayout(this);
        swRow.setOrientation(LinearLayout.HORIZONTAL);
        swRow.setPadding(0, dp(8), 0, 0);
        Button swStartBtn = primaryBtn(swRunning ? "إيقاف" : "بدء");
        Button swReset = chipBtn("تصفير");
        swRow.addView(swStartBtn, half);
        swRow.addView(swReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(swRow);
        swStartBtn.setOnClickListener(v -> {
            if (!swRunning) {
                swStart = System.currentTimeMillis();
                swRunning = true;
                swStartBtn.setText("إيقاف");
                mainHandler.post(swTick);
            } else {
                swRunning = false;
                swStartBtn.setText("بدء");
            }
        });
        swReset.setOnClickListener(v -> {
            swRunning = false;
            swStartBtn.setText("بدء");
            swText.setText("00:00.00");
        });

        box.addView(section("أخرى"));
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button coin = primaryBtn("عملة");
        Button dice = primaryBtn("نرد");
        row2.addView(coin, half);
        row2.addView(dice, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(row2);
        TextView rnd = new TextView(this);
        rnd.setTextColor(MUTED);
        rnd.setGravity(Gravity.CENTER);
        rnd.setPadding(0, dp(8), 0, 0);
        box.addView(rnd);
        coin.setOnClickListener(v -> rnd.setText(random.nextBoolean() ? "ملك" : "كتابة"));
        dice.setOnClickListener(v -> rnd.setText("النرد: " + (random.nextInt(6) + 1)));
        TextView passView = new TextView(this);
        passView.setText("—");
        passView.setTextColor(TEXT);
        passView.setBackground(rounded(CARD, 12));
        passView.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams pp = matchWrap();
        pp.topMargin = dp(10);
        box.addView(passView, pp);
        LinearLayout passRow = new LinearLayout(this);
        passRow.setOrientation(LinearLayout.HORIZONTAL);
        passRow.setPadding(0, dp(6), 0, 0);
        Button gen = primaryBtn("كلمة مرور");
        Button copy = chipBtn("نسخ");
        passRow.addView(gen, half);
        passRow.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(passRow);
        gen.setOnClickListener(v -> passView.setText(genPass()));
        copy.setOnClickListener(v -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("p", passView.getText().toString()));
            Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show();
        });
    }

    private LinearLayout headerBar(String t) {
        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView title = new TextView(this);
        title.setText(t);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        top.addView(title);
        return top;
    }

    private LinearLayout cardBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(CARD, 14));
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setGravity(Gravity.CENTER);
        return box;
    }

    private TextView bigNum(String t, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(28);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private TextView smallLabel(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(MUTED);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(12);
        return v;
    }

    private void vibrate() {
        try {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(10);
        } catch (Exception ignored) {
        }
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackground(rounded(CARD, 12));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setSingleLine(true);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(6);
        e.setLayoutParams(lp);
        return e;
    }

    private String genPass() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    private void onCalcKey(String key) {
        if (key.equals("C")) calcExpr = "";
        else if (key.equals("=")) {
            String res = tryMath(calcExpr.replace("÷", "/").replace("×", "*"));
            calcExpr = res != null ? res.replace("النتيجة: ", "") : "خطأ";
        } else {
            if (calcExpr.equals("خطأ")) calcExpr = "";
            calcExpr += key;
        }
        if (calcDisplay != null) calcDisplay.setText(calcExpr.isEmpty() ? "0" : calcExpr);
    }

    private void buildNotes(LinearLayout content) {
        content.addView(headerBar("الملاحظات"));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        EditText noteInput = new EditText(this);
        noteInput.setHint("ملاحظة جديدة...");
        noteInput.setTextColor(TEXT);
        noteInput.setHintTextColor(MUTED);
        noteInput.setBackground(rounded(CARD, 14));
        noteInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.addView(noteInput, matchWrap());
        Button add = primaryBtn("إضافة");
        LinearLayout.LayoutParams ap = matchWrap();
        ap.topMargin = dp(10);
        ap.bottomMargin = dp(12);
        box.addView(add, ap);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        box.addView(list);
        renderNotes(list);
        add.setOnClickListener(v -> {
            String t = noteInput.getText().toString().trim();
            if (t.isEmpty()) return;
            notes.add(0, t);
            saveNotes();
            noteInput.setText("");
            renderNotes(list);
        });
    }

    private ArrayList<String> notes = new ArrayList<>();

    private void renderNotes(LinearLayout list) {
        list.removeAllViews();
        if (notes.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("لا توجد ملاحظات");
            e.setTextColor(MUTED);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(24), 0, 0);
            list.addView(e);
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            final int idx = i;
            final String note = notes.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(rounded(CARD, 12));
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams cp = matchWrap();
            cp.bottomMargin = dp(8);
            TextView tv = new TextView(this);
            tv.setText(note);
            tv.setTextColor(TEXT);
            card.addView(tv);
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(8), 0, 0);
            Button share = chipBtn("مشاركة");
            Button del = chipBtn("حذف");
            actions.addView(share);
            actions.addView(del);
            card.addView(actions);
            share.setOnClickListener(v -> {
                Intent in = new Intent(Intent.ACTION_SEND);
                in.setType("text/plain");
                in.putExtra(Intent.EXTRA_TEXT, note);
                startActivity(Intent.createChooser(in, "مشاركة"));
            });
            del.setOnClickListener(v -> {
                notes.remove(idx);
                saveNotes();
                renderNotes(list);
            });
            list.addView(card, cp);
        }
    }

    private void loadNotes() {
        notes.clear();
        String raw = prefs.getString("notes", null);
        if (raw == null) return;
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) notes.add(a.optString(i));
        } catch (Exception ignored) {
        }
    }

    private void saveNotes() {
        try {
            JSONArray a = new JSONArray();
            for (String n : notes) a.put(n);
            prefs.edit().putString("notes", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void buildAbout(LinearLayout content) {
        loadNotes();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        TextView h = new TextView(this);
        h.setText("حولي");
        h.setTextSize(24);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(TEXT);
        box.addView(h);
        box.addView(infoCard("إحصائيات",
                userName + "\nمحادثات: " + chats.size() + " | رسائل: " + totalSent +
                        "\nتسبيح: " + tasbih + " | ماء: " + water + "/8"));
        box.addView(section("الثيم"));
        LinearLayout themes = new LinearLayout(this);
        themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.addView(themeBtn("أخضر", 0), navWeight());
        themes.addView(themeBtn("أزرق", 1), navWeight());
        themes.addView(themeBtn("بنفسجي", 2), navWeight());
        box.addView(themes);
        Button nameBtn = primaryBtn("تغيير الاسم");
        LinearLayout.LayoutParams np = matchWrap();
        np.topMargin = dp(16);
        box.addView(nameBtn, np);
        nameBtn.setOnClickListener(v -> changeName());
        if (!premium) {
            Button pro = primaryBtn("تفعيل Pro");
            LinearLayout.LayoutParams pp = matchWrap();
            pp.topMargin = dp(10);
            box.addView(pro, pp);
            pro.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                showMain();
            });
        }
        TextView ver = new TextView(this);
        ver.setText("NOVA AI v1.9 — محادثات متعددة");
        ver.setTextColor(MUTED);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(24), 0, 0);
        box.addView(ver);
    }

    private Button themeBtn(String label, int id) {
        Button b = chipBtn(label);
        if (themeId == id) {
            b.setBackground(rounded(ACCENT, 12));
            b.setTextColor(Color.parseColor("#04120E"));
        }
        b.setOnClickListener(v -> {
            themeId = id;
            prefs.edit().putInt("theme_id", themeId).apply();
            applyTheme();
            showMain();
        });
        return b;
    }

    private LinearLayout infoCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(CARD, 14));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(14);
        card.setLayoutParams(lp);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(ACCENT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);
        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(MUTED);
        b.setPadding(0, dp(8), 0, 0);
        card.addView(b);
        return card;
    }

    private void changeName() {
        final EditText input = new EditText(this);
        input.setText(userName);
        input.setTextColor(TEXT);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        new AlertDialog.Builder(this).setTitle("تغيير الاسم").setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) {
                        userName = n;
                        prefs.edit().putString("user_name", userName).apply();
                        showMain();
                    }
                }).setNegativeButton("إلغاء", null).show();
    }

    private String localReply(String message) {
        String m = message.toLowerCase().trim();
        if (has(m, "مع السلامة", "باي", "وداع", "bye"))
            return "مع السلامة " + userName + "!";
        if (has(m, "مباراة", "ماتش", "نتيجة", "نتائج", "دوري", "يلعب", "بيلعب"))
            return "ما عندي نتائج مباريات مباشرة.";
        if (has(m, "أذكار المساء") || m.equals("مساء") || has(m, "مساء الخير"))
            return "🌙 " + AZKAR_EVENING[0] + "\n" + AZKAR_EVENING[1];
        if (has(m, "أذكار", "اذكار", "صباح") || m.equals("صباح"))
            return "🌅 " + AZKAR_MORNING[0] + "\n" + AZKAR_MORNING[1];
        if (has(m, "محادثة", "شات", "جديد"))
            return "اضغط + لمحادثة جديدة، أو 📋 لعرض كل المحادثات.";
        if (has(m, "سلام", "هلا", "مرحبا", "hello", "hi"))
            return "وعليكم السلام " + userName + "!";
        if (has(m, "كيف حالك", "كيفك", "شلونك"))
            return "بخير! وأنت؟";
        if (has(m, "اسمك", "من أنت", "من انت"))
            return "أنا NOVA AI — محادثات متعددة محليًا.";
        if (has(m, "شكرا", "شكرًا", "مشكور"))
            return "العفو!";
        if (has(m, "مساعدة", "ساعدني", "help"))
            return "📋 قائمة المحادثات · + محادثة جديدة\nأدوات · ملاحظات · حولي";
        if (has(m, "مسبحة", "تسبيح"))
            return "التسبيح: " + tasbih;
        if (has(m, "ماء"))
            return "الماء: " + water + "/8";
        if (has(m, "وقت", "ساعة", "ساعه")) {
            Calendar c = Calendar.getInstance();
            return String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
        if (has(m, "تاريخ") || m.equals("التاريخ")) {
            Calendar c = Calendar.getInstance();
            return c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
        }
        if (has(m, "نكتة", "نكته", "اضحكني")) {
            String[] j = {"ليش الكمبيوتر راح للدكتور؟ عنده فيروس! 😄", "بايثون في مطعم: SyntaxError 😂"};
            return j[jokeIndex++ % j.length];
        }
        if (has(m, "نصيحة", "تحفيز"))
            return "خطوة صغيرة كل يوم أفضل من حماسة يوم واحد.";
        if (has(m, "دعاء"))
            return "اللهم يسّر وأعن.";
        String calc = tryMath(m.replace("÷", "/").replace("×", "*"));
        if (calc != null) return calc;
        return "فهمت. جرب الأزرار أو + لمحادثة جديدة.";
    }

    private String tryMath(String m) {
        try {
            m = m.replace(" ", "");
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
                if (p.length == 2 && Double.parseDouble(p[1]) != 0)
                    return "النتيجة: " + (Double.parseDouble(p[0]) / Double.parseDouble(p[1]));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private TextView section(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(ACCENT);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(14);
        v.setPadding(0, dp(18), 0, dp(8));
        return v;
    }

    private boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k.toLowerCase())) return true;
        return false;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.parseColor("#04120E"));
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(13);
        b.setBackground(rounded(ACCENT, 12));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        return b;
    }

    private Button chipBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(ACCENT);
        b.setTextSize(12);
        b.setBackground(rounded(CARD, 12));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        return b;
    }

    private GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
