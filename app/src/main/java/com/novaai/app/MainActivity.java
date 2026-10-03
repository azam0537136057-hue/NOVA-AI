package com.novaai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private int BG, SURFACE, CARD, ACCENT, USER_BUBBLE, BOT_BUBBLE, TEXT, MUTED;
    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 80;
    private static final int PREMIUM_LIMIT = 800;

    private SharedPreferences prefs;
    private String userName = "", userEmail = "";
    private boolean premium = false, loggedIn = false;
    private int usage = 0, totalSent = 0, jokeIndex = 0, tab = 0, themeId = 0, lang = 0;
    private int tasbih = 0, water = 0, azkarIdx = 0;
    private boolean showChatList = false;

    private static class Chat {
        String id, title;
        ArrayList<String> messages = new ArrayList<>();
        Chat(String id, String title) { this.id = id; this.title = title; }
    }
    private ArrayList<Chat> chats = new ArrayList<>();
    private String currentChatId = "";
    private ArrayList<String> notes = new ArrayList<>();

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
                swText.setText(String.format(Locale.US, "%02d:%02d.%02d", m, s, cs));
                mainHandler.postDelayed(this, 50);
            }
        }
    };
    private HashMap<String, String[]> L = new HashMap<>();

    private static final String[] AZKAR_MORNING = {
            "أصبحنا وأصبح الملك لله، والحمد لله",
            "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور",
            "سبحان الله وبحمده"
    };
    private static final String[] AZKAR_EVENING = {
            "أمسينا وأمسى الملك لله، والحمد لله",
            "اللهم بك أمسينا وبك أصبحنا وبك نحيا وبك نموت وإليك المصير",
            "أعوذ بكلمات الله التامات من شر ما خلق"
    };

    private void initLang() {
        L.put("chat", new String[]{"محادثة", "Chat", "Chat", "بات چیت", "Sohbet"});
        L.put("tools", new String[]{"أدوات", "Tools", "Outils", "آلات", "Araçlar"});
        L.put("notes", new String[]{"ملاحظات", "Notes", "Notes", "نوٹس", "Notlar"});
        L.put("about", new String[]{"حولي", "About", "À propos", "کے بارے", "Hakkında"});
        L.put("chats", new String[]{"المحادثات", "Chats", "Discussions", "بات چیتیں", "Sohbetler"});
        L.put("new_chat", new String[]{"محادثة جديدة", "New chat", "Nouveau chat", "نئی بات", "Yeni sohbet"});
        L.put("send", new String[]{"إرسال", "Send", "Envoyer", "بھیجیں", "Gönder"});
        L.put("type", new String[]{"اكتب رسالة...", "Message...", "Écrire...", "پیغام...", "Mesaj..."});
        L.put("login", new String[]{"دخول", "Log in", "Connexion", "لاگ ان", "Giriş"});
        L.put("register", new String[]{"إنشاء حساب", "Sign up", "S'inscrire", "رجسٹر", "Kayıt"});
        L.put("email", new String[]{"البريد", "Email", "E-mail", "ای میل", "E-posta"});
        L.put("password", new String[]{"كلمة المرور", "Password", "Mot de passe", "پاس ورڈ", "Şifre"});
        L.put("name", new String[]{"الاسم", "Name", "Nom", "نام", "Ad"});
        L.put("help", new String[]{"مساعدة", "Help", "Aide", "مدد", "Yardım"});
        L.put("joke", new String[]{"نكتة", "Joke", "Blague", "جوک", "Şaka"});
        L.put("advice", new String[]{"نصيحة", "Advice", "Conseil", "مشورہ", "Tavsiye"});
        L.put("azkar", new String[]{"أذكار", "Azkar", "Invocations", "اذکار", "Zikir"});
        L.put("local", new String[]{"محلي", "Local", "Local", "مقامی", "Yerel"});
        L.put("delete", new String[]{"حذف", "Delete", "Supprimer", "حذف", "Sil"});
        L.put("open", new String[]{"فتح", "Open", "Ouvrir", "کھولیں", "Aç"});
        L.put("lang", new String[]{"اللغة", "Language", "Langue", "زبان", "Dil"});
    }

    private String t(String key) {
        String[] arr = L.get(key);
        if (arr == null) return key;
        return arr[Math.max(0, Math.min(lang, arr.length - 1))];
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        initLang();
        userName = prefs.getString("user_name", "");
        userEmail = prefs.getString("user_email", "");
        loggedIn = prefs.getBoolean("logged_in", false);
        premium = prefs.getBoolean("premium", false);
        usage = prefs.getInt("usage", 0);
        totalSent = prefs.getInt("total_sent", 0);
        themeId = prefs.getInt("theme_id", 0);
        lang = prefs.getInt("lang", 0);
        tasbih = prefs.getInt("tasbih", 0);
        water = prefs.getInt("water", 0);
        applyTheme();
        loadChats();
        loadNotes();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        if (!loggedIn || userEmail.isEmpty()) showAuth();
        else showMain();
    }

    private void applyTheme() {
        if (themeId == 0) {
            BG = Color.parseColor("#FFFFFF");
            SURFACE = Color.parseColor("#F7F7F8");
            CARD = Color.parseColor("#EFEFEF");
            ACCENT = Color.parseColor("#1A1A1A");
            TEXT = Color.parseColor("#0D0D0D");
            MUTED = Color.parseColor("#6B6B6B");
            USER_BUBBLE = Color.parseColor("#F0F0F0");
            BOT_BUBBLE = Color.parseColor("#F7F7F8");
        } else if (themeId == 1) {
            BG = Color.parseColor("#0A0A0A");
            SURFACE = Color.parseColor("#141414");
            CARD = Color.parseColor("#1F1F1F");
            ACCENT = Color.parseColor("#FFFFFF");
            TEXT = Color.parseColor("#F5F5F5");
            MUTED = Color.parseColor("#888888");
            USER_BUBBLE = Color.parseColor("#2A2A2A");
            BOT_BUBBLE = Color.parseColor("#1A1A1A");
        } else {
            BG = Color.parseColor("#070B14");
            SURFACE = Color.parseColor("#0F1623");
            CARD = Color.parseColor("#151D2E");
            ACCENT = Color.parseColor("#00E5A8");
            TEXT = Color.parseColor("#E8EEF7");
            MUTED = Color.parseColor("#7A8BA3");
            USER_BUBBLE = Color.parseColor("#1A2740");
            BOT_BUBBLE = Color.parseColor("#121A28");
        }
    }

    private int onAccent() {
        return themeId == 0 ? Color.WHITE : (themeId == 1 ? Color.BLACK : Color.parseColor("#04120E"));
    }

    private void showAuth() {
        root.removeAllViews();
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        TextView logo = new TextView(this);
        logo.setText("NOVA");
        logo.setTextSize(36);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(TEXT);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);
        TextView sub = new TextView(this);
        sub.setText(t("login") + " / " + t("register"));
        sub.setTextColor(MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(8), 0, dp(20));
        root.addView(sub);
        EditText nameIn = field(t("name"));
        EditText emailIn = field(t("email"));
        EditText passIn = field(t("password"));
        passIn.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        if (!userName.isEmpty()) nameIn.setText(userName);
        if (!userEmail.isEmpty()) emailIn.setText(userEmail);
        root.addView(nameIn);
        root.addView(emailIn);
        root.addView(passIn);
        Button reg = primaryBtn(t("register"));
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(14);
        root.addView(reg, bp);
        reg.setOnClickListener(v -> {
            String n = nameIn.getText().toString().trim();
            String e = emailIn.getText().toString().trim().toLowerCase(Locale.US);
            String p = passIn.getText().toString();
            if (n.isEmpty() || e.isEmpty() || p.length() < 4) {
                Toast.makeText(this, "اسم + إيميل + مرور 4+", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!e.contains("@")) {
                Toast.makeText(this, "إيميل غير صالح", Toast.LENGTH_SHORT).show();
                return;
            }
            userName = n;
            userEmail = e;
            prefs.edit().putString("user_name", n).putString("user_email", e)
                    .putString("user_pass", p).putBoolean("logged_in", true).apply();
            loggedIn = true;
            showMain();
        });
        Button login = chipBtn(t("login"));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(10);
        root.addView(login, lp);
        login.setOnClickListener(v -> {
            String e = emailIn.getText().toString().trim().toLowerCase(Locale.US);
            String p = passIn.getText().toString();
            if (e.equals(prefs.getString("user_email", "")) && p.equals(prefs.getString("user_pass", "")) && !e.isEmpty()) {
                userName = prefs.getString("user_name", "User");
                userEmail = e;
                loggedIn = true;
                prefs.edit().putBoolean("logged_in", true).apply();
                showMain();
            } else Toast.makeText(this, "بيانات خطأ", Toast.LENGTH_SHORT).show();
        });
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setBackground(rounded(CARD, 14));
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        e.setSingleLine(true);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(8);
        e.setLayoutParams(lp);
        return e;
    }

    private Chat currentChat() {
        for (Chat c : chats) if (c.id.equals(currentChatId)) return c;
        if (!chats.isEmpty()) { currentChatId = chats.get(0).id; return chats.get(0); }
        return null;
    }

    private void ensureChat() {
        if (chats.isEmpty()) {
            Chat c = new Chat(UUID.randomUUID().toString(), t("new_chat"));
            c.messages.add("NOVA AI: مرحبًا " + userName + " 👋");
            chats.add(c);
            currentChatId = c.id;
            saveChats();
        } else if (currentChat() == null) currentChatId = chats.get(0).id;
    }

    private void newChat() {
        Chat c = new Chat(UUID.randomUUID().toString(), t("new_chat"));
        c.messages.add("NOVA AI: " + t("new_chat"));
        chats.add(0, c);
        currentChatId = c.id;
        showChatList = false;
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
                    Chat c = new Chat(o.getString("id"), o.optString("title", "Chat"));
                    JSONArray msgs = o.optJSONArray("messages");
                    if (msgs != null) for (int j = 0; j < msgs.length(); j++) c.messages.add(msgs.optString(j));
                    chats.add(c);
                }
            } catch (Exception ignored) {}
        }
        if (chats.isEmpty()) {
            Chat c = new Chat(UUID.randomUUID().toString(), t("new_chat"));
            c.messages.add("NOVA AI: مرحبًا " + userName);
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
            prefs.edit().putString("chats_v2", arr.toString()).putString("current_chat_id", currentChatId).apply();
        } catch (Exception ignored) {}
    }

    private void showMain() {
        root.removeAllViews();
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.TOP);
        root.setPadding(0, 0, 0, 0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(content);
        if (tab == 0) {
            if (showChatList) buildChatList(content);
            else buildChat(content);
        } else if (tab == 1) buildTools(content);
        else if (tab == 2) buildNotes(content);
        else buildAbout(content);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(SURFACE);
        nav.setPadding(dp(8), dp(10), dp(8), dp(10));
        nav.addView(navBtn(t("chat"), 0), navWeight());
        nav.addView(navBtn(t("tools"), 1), navWeight());
        nav.addView(navBtn(t("notes"), 2), navWeight());
        nav.addView(navBtn(t("about"), 3), navWeight());
        root.addView(nav);
    }

    private LinearLayout.LayoutParams navWeight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private Button navBtn(String label, int id) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        boolean on = tab == id;
        b.setTextColor(on ? onAccent() : MUTED);
        b.setBackground(rounded(on ? ACCENT : Color.TRANSPARENT, 12));
        b.setOnClickListener(v -> {
            tab = id;
            if (id != 0) showChatList = false;
            showMain();
        });
        return b;
    }

    private LinearLayout bar(String title) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(14), dp(14), dp(12), dp(14));
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        top.addView(t);
        return top;
    }

    private void buildChatList(LinearLayout content) {
        LinearLayout top = bar(t("chats"));
        Button add = primaryBtn("+");
        add.setOnClickListener(v -> newChat());
        top.addView(add);
        content.addView(top);
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(12), dp(12), dp(12));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        for (int i = 0; i < chats.size(); i++) {
            final Chat c = chats.get(i);
            final int idx = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(rounded(c.id.equals(currentChatId) ? CARD : SURFACE, 14));
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            LinearLayout.LayoutParams cp = matchWrap();
            cp.bottomMargin = dp(10);
            TextView t1 = new TextView(this);
            t1.setText((c.id.equals(currentChatId) ? "● " : "") + c.title);
            t1.setTextColor(TEXT);
            t1.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(t1);
            String preview = c.messages.isEmpty() ? "" : c.messages.get(c.messages.size() - 1);
            if (preview.length() > 48) preview = preview.substring(0, 48) + "…";
            TextView t2 = new TextView(this);
            t2.setText(preview);
            t2.setTextColor(MUTED);
            t2.setTextSize(12);
            t2.setPadding(0, dp(4), 0, 0);
            card.addView(t2);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(10), 0, 0);
            Button open = primaryBtn(t("open"));
            Button del = chipBtn(t("delete"));
            LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            half.setMarginEnd(dp(8));
            row.addView(open, half);
            row.addView(del, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            card.addView(row);
            open.setOnClickListener(v -> {
                currentChatId = c.id;
                showChatList = false;
                saveChats();
                showMain();
            });
            del.setOnClickListener(v -> {
                if (chats.size() <= 1) {
                    Toast.makeText(this, "1+", Toast.LENGTH_SHORT).show();
                    return;
                }
                chats.remove(idx);
                if (c.id.equals(currentChatId)) currentChatId = chats.get(0).id;
                saveChats();
                showMain();
            });
            list.addView(card, cp);
        }
    }

    private void buildChat(LinearLayout content) {
        ensureChat();
        Chat chat = currentChat();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(SURFACE);
        top.setPadding(dp(10), dp(12), dp(10), dp(12));
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button listBtn = chipBtn("☰");
        listBtn.setOnClickListener(v -> { showChatList = true; showMain(); });
        top.addView(listBtn);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        col.setPadding(dp(10), 0, dp(6), 0);
        TextView title = new TextView(this);
        title.setText(chat != null ? chat.title : t("chat"));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT);
        col.addView(title);
        TextView online = new TextView(this);
        online.setText(t("local") + " · " + chats.size());
        online.setTextSize(11);
        online.setTextColor(MUTED);
        col.addView(online);
        top.addView(col);
        Button newBtn = chipBtn("+");
        newBtn.setOnClickListener(v -> newChat());
        top.addView(newBtn);
        content.addView(top);

        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText(userName + " · " + usage + "/" + limit);
        status.setTextSize(12);
        status.setTextColor(MUTED);
        status.setPadding(dp(16), dp(6), dp(16), dp(4));
        content.addView(status);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(dp(8), dp(4), dp(8), dp(8));
        for (String s : new String[]{t("joke"), t("azkar"), t("advice"), t("help")}) {
            Button chip = chipBtn(s);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            cp.setMargins(dp(3), 0, dp(3), 0);
            chips.addView(chip, cp);
            chip.setOnClickListener(v -> sendQuick(s, content));
        }
        content.addView(chips);

        ScrollView scroll = new ScrollView(this);
        LinearLayout msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(dp(14), dp(6), dp(14), dp(8));
        scroll.addView(msgBox);
        content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        refreshMessages(msgBox);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(SURFACE);
        bottom.setPadding(dp(12), dp(10), dp(12), dp(10));
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new EditText(this);
        input.setHint(t("type"));
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackground(rounded(CARD, 14));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSingleLine(true);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button send = primaryBtn(t("send"));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(dp(8));
        bottom.addView(send, sp);
        content.addView(bottom);
        send.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) return;
            input.setText("");
            doSend(txt, status, msgBox, scroll, send);
        });
    }

    private void sendQuick(String text, LinearLayout content) {
        TextView status = null;
        ScrollView scroll = null;
        LinearLayout msgBox = null;
        Button sendBtn = null;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (c instanceof TextView && ((TextView) c).getText().toString().contains("/"))
                status = (TextView) c;
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
                        if (b.getText().toString().equals(t("send")) || "...".equals(b.getText().toString()))
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
            Toast.makeText(this, "Limit", Toast.LENGTH_SHORT).show();
            return;
        }
        Chat chat = currentChat();
        if (chat == null) return;
        try {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(10);
        } catch (Exception ignored) {}
        chat.messages.add("أنت: " + text);
        if (chat.messages.size() <= 3)
            chat.title = text.length() > 22 ? text.substring(0, 22) + "…" : text;
        usage++;
        totalSent++;
        prefs.edit().putInt("usage", usage).putInt("total_sent", totalSent).apply();
        saveChats();
        status.setText(userName + " · " + usage + "/" + lim);
        refreshMessages(msgBox);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        send.setEnabled(false);
        send.setText("...");
        executor.execute(() -> {
            try { Thread.sleep(120 + random.nextInt(160)); } catch (Exception ignored) {}
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
                send.setText(t("send"));
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
            tv.setBackground(rounded(isUser ? USER_BUBBLE : BOT_BUBBLE, 16));
            tv.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams lp = matchWrap();
            lp.bottomMargin = dp(8);
            box.addView(tv, lp);
        }
    }

    private void buildTools(LinearLayout content) {
        content.addView(bar(t("tools")));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(24));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        half.setMarginEnd(dp(8));

        // أذكار
        box.addView(section(t("azkar")));
        azkarText = new TextView(this);
        azkarText.setText(AZKAR_MORNING[0]);
        azkarText.setTextColor(TEXT);
        azkarText.setBackground(rounded(CARD, 14));
        azkarText.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.addView(azkarText, matchWrap());
        LinearLayout aRow = new LinearLayout(this);
        aRow.setOrientation(LinearLayout.HORIZONTAL);
        aRow.setPadding(0, dp(8), 0, 0);
        Button aM = primaryBtn("صباح");
        Button aE = primaryBtn("مساء");
        Button aN = chipBtn("→");
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

        // مسبحة وماء
        box.addView(section("تسبيح / ماء"));
        LinearLayout counters = new LinearLayout(this);
        counters.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout tb = cardBox();
        tasbihText = num(String.valueOf(tasbih));
        tb.addView(tasbihText);
        Button tPlus = primaryBtn("+");
        tb.addView(tPlus);
        tPlus.setOnClickListener(v -> {
            tasbih++;
            prefs.edit().putInt("tasbih", tasbih).apply();
            tasbihText.setText(String.valueOf(tasbih));
        });
        LinearLayout wb = cardBox();
        waterText = num(water + "/8");
        wb.addView(waterText);
        Button wPlus = primaryBtn("+");
        wb.addView(wPlus);
        wPlus.setOnClickListener(v -> {
            water++;
            prefs.edit().putInt("water", water).apply();
            waterText.setText(water + "/8");
        });
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        c1.setMarginEnd(dp(8));
        counters.addView(tb, c1);
        counters.addView(wb, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        box.addView(counters);

        // حاسبة
        box.addView(section("حاسبة"));
        calcDisplay = new TextView(this);
        calcDisplay.setText("0");
        calcDisplay.setTextSize(24);
        calcDisplay.setTextColor(TEXT);
        calcDisplay.setBackground(rounded(CARD, 12));
        calcDisplay.setPadding(dp(14), dp(12), dp(14), dp(12));
        calcDisplay.setGravity(Gravity.END);
        box.addView(calcDisplay, matchWrap());
        String[][] rows = {{"7", "8", "9", "÷"}, {"4", "5", "6", "×"}, {"1", "2", "3", "-"}, {"0", ".", "C", "+"}, {"="}};
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setPadding(0, dp(4), 0, 0);
            for (String key : row) {
                Button b = chipBtn(key);
                b.setTextColor(TEXT);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                lp.setMarginEnd(dp(4));
                r.addView(b, lp);
                b.setOnClickListener(v -> onCalcKey(key));
            }
            box.addView(r);
        }

        // BMI
        box.addView(section("BMI"));
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
                bmiOut.setText(String.format(Locale.US, "BMI = %.1f — %s", bmi, cat));
            } catch (Exception e) {
                bmiOut.setText("أدخل أرقام صحيحة");
            }
        });

        // حرارة
        box.addView(section("تحويل حرارة"));
        EditText tempIn = field("درجة الحرارة");
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
                tempOut.setText(String.format(Locale.US, "%.1f °F", c * 9 / 5 + 32));
            } catch (Exception e) {
                tempOut.setText("رقم غير صالح");
            }
        });
        toC.setOnClickListener(v -> {
            try {
                double f = Double.parseDouble(tempIn.getText().toString().trim());
                tempOut.setText(String.format(Locale.US, "%.1f °C", (f - 32) * 5 / 9));
            } catch (Exception e) {
                tempOut.setText("رقم غير صالح");
            }
        });

        // ساعة إيقاف
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

        // عشوائي + مرور
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

    private LinearLayout cardBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(CARD, 14));
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setGravity(Gravity.CENTER);
        return box;
    }

    private TextView num(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(26);
        v.setTextColor(TEXT);
        v.setGravity(Gravity.CENTER);
        return v;
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
            calcExpr = res != null ? res : "Err";
        } else {
            if ("Err".equals(calcExpr)) calcExpr = "";
            calcExpr += key;
        }
        if (calcDisplay != null) calcDisplay.setText(calcExpr.isEmpty() ? "0" : calcExpr);
    }

    private void buildNotes(LinearLayout content) {
        content.addView(bar(t("notes")));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        EditText noteInput = field("...");
        box.addView(noteInput);
        Button add = primaryBtn("+");
        LinearLayout.LayoutParams ap = matchWrap();
        ap.topMargin = dp(8);
        ap.bottomMargin = dp(12);
        box.addView(add, ap);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        box.addView(list);
        renderNotes(list);
        add.setOnClickListener(v -> {
            String tx = noteInput.getText().toString().trim();
            if (tx.isEmpty()) return;
            notes.add(0, tx);
            saveNotes();
            noteInput.setText("");
            renderNotes(list);
        });
    }

    private void renderNotes(LinearLayout list) {
        list.removeAllViews();
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
            Button del = chipBtn(t("delete"));
            card.addView(del);
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
        } catch (Exception ignored) {}
    }

    private void saveNotes() {
        try {
            JSONArray a = new JSONArray();
            for (String n : notes) a.put(n);
            prefs.edit().putString("notes", a.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void buildAbout(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroll.addView(box);
        content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        TextView h = new TextView(this);
        h.setText(t("about"));
        h.setTextSize(24);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextColor(TEXT);
        box.addView(h);
        box.addView(infoCard(userName, userEmail + "\n" + chats.size() + " محادثة · " + totalSent));

        box.addView(section("المظهر"));
        String[] themes = {"أبيض", "أسود", "أخضر داكن"};
        for (int i = 0; i < 3; i++) {
            final int id = i;
            Button b = chipBtn((themeId == i ? "● " : "○ ") + themes[i]);
            if (themeId == i) {
                b.setBackground(rounded(ACCENT, 12));
                b.setTextColor(onAccent());
            }
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = dp(6);
            box.addView(b, lp);
            b.setOnClickListener(v -> {
                themeId = id;
                prefs.edit().putInt("theme_id", themeId).apply();
                applyTheme();
                showMain();
            });
        }

        box.addView(section(t("lang")));
        String[] langs = {"العربية", "English", "Français", "اردو", "Türkçe"};
        for (int i = 0; i < langs.length; i++) {
            final int id = i;
            Button b = chipBtn((lang == i ? "● " : "○ ") + langs[i]);
            if (lang == i) {
                b.setBackground(rounded(ACCENT, 12));
                b.setTextColor(onAccent());
            }
            LinearLayout.LayoutParams lp = matchWrap();
            lp.topMargin = dp(6);
            box.addView(b, lp);
            b.setOnClickListener(v -> {
                lang = id;
                prefs.edit().putInt("lang", lang).apply();
                showMain();
            });
        }

        if (!premium) {
            Button pro = primaryBtn("Pro");
            LinearLayout.LayoutParams pp = matchWrap();
            pp.topMargin = dp(16);
            box.addView(pro, pp);
            pro.setOnClickListener(v -> {
                premium = true;
                prefs.edit().putBoolean("premium", true).apply();
                showMain();
            });
        }
        Button logout = chipBtn("خروج");
        LinearLayout.LayoutParams lo = matchWrap();
        lo.topMargin = dp(12);
        box.addView(logout, lo);
        logout.setOnClickListener(v -> {
            prefs.edit().putBoolean("logged_in", false).apply();
            loggedIn = false;
            showAuth();
        });
        TextView ver = new TextView(this);
        ver.setText("NOVA AI v2.2 · ثيم أبيض");
        ver.setTextColor(MUTED);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(24), 0, 0);
        box.addView(ver);
    }

    private LinearLayout infoCard(String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(CARD, 14));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(14);
        card.setLayoutParams(lp);
        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextColor(TEXT);
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t1);
        TextView t2 = new TextView(this);
        t2.setText(body);
        t2.setTextColor(MUTED);
        t2.setPadding(0, dp(6), 0, 0);
        card.addView(t2);
        return card;
    }

    private String localReply(String message) {
        String m = message.toLowerCase(Locale.US).trim();
        if ((has(m, "سلام", "hello", "hi") && has(m, "كيف", "حالك", "شلونك")) || has(m, "السلام عليكم كيف"))
            return "وعليكم السلام " + userName + "!\nأنا بخير، شكرًا. كيف أقدر أساعدك؟";
        if (has(m, "السلام عليكم", "سلام عليكم"))
            return "وعليكم السلام ورحمة الله " + userName;
        if (has(m, "كيف حالك", "كيفك", "شلونك"))
            return "بخير الحمد لله! وأنت يا " + userName + "؟";
        if (has(m, "مباراة", "مباريات", "ماتش", "نتيجة", "نتائج", "دوري", "يلعب", "بيلعب"))
            return "ما عندي نتائج مباريات مباشرة (وضع محلي).";
        if (has(m, "نكتة", "joke", "اضحكني"))
            return "ليش الكمبيوتر راح للدكتور؟ عنده فيروس! 😄";
        if (has(m, "نصيحة", "advice", "تحفيز"))
            return "خطوة صغيرة كل يوم أفضل من حماسة يوم واحد.";
        if (has(m, "مساعدة", "help", "تقدر تساعدني", "ساعدني"))
            return "☰ المحادثات · + جديدة · أدوات (حرارة/BMI/حاسبة) · ملاحظات · حولي";
        if (has(m, "أذكار", "azkar", "صباح", "مساء"))
            return "🌅 " + AZKAR_MORNING[0];
        if (has(m, "شكرا", "شكرًا", "مشكور"))
            return "العفو!";
        if (has(m, "مع السلامة", "باي", "bye"))
            return "مع السلامة " + userName + "!";
        if (has(m, "اسمك", "من أنت", "من انت", "ايش اسمك"))
            return "أنا NOVA AI — نسخة محلية بثيم أبيض.";
        if (has(m, "وقت", "ساعة", "ساعه")) {
            Calendar c = Calendar.getInstance();
            return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
        String calc = tryMath(m.replace("÷", "/").replace("×", "*"));
        if (calc != null) return "النتيجة: " + calc;
        return "وصلتني رسالتك. جرب: مساعدة، أذكار، نصيحة، أو ☰ للمحادثات.";
    }

    private String tryMath(String m) {
        try {
            m = m.replace(" ", "");
            if (m.contains("+")) {
                String[] p = m.split("\\+");
                if (p.length == 2) return "" + (Double.parseDouble(p[0]) + Double.parseDouble(p[1]));
            }
            if (m.contains("-") && m.indexOf('-') > 0) {
                String[] p = m.split("-");
                if (p.length == 2) return "" + (Double.parseDouble(p[0]) - Double.parseDouble(p[1]));
            }
            if (m.contains("*")) {
                String[] p = m.split("\\*");
                if (p.length == 2) return "" + (Double.parseDouble(p[0]) * Double.parseDouble(p[1]));
            }
            if (m.contains("/")) {
                String[] p = m.split("/");
                if (p.length == 2 && Double.parseDouble(p[1]) != 0)
                    return "" + (Double.parseDouble(p[0]) / Double.parseDouble(p[1]));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private TextView section(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(TEXT);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(16), 0, dp(8));
        return v;
    }

    private boolean has(String text, String... keys) {
        for (String k : keys) if (text.contains(k.toLowerCase(Locale.US))) return true;
        return false;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(onAccent());
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(13);
        b.setBackground(rounded(ACCENT, 12));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        return b;
    }

    private Button chipBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
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
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
