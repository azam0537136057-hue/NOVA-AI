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

    private int BG, SURFACE, CARD, ACCENT, ACCENT_DIM, USER_BUBBLE, BOT_BUBBLE, TEXT, MUTED;
    private static final String PREFS = "nova_ai";
    private static final int FREE_LIMIT = 50;
    private static final int PREMIUM_LIMIT = 600;

    private SharedPreferences prefs;
    private String userName = "";
    private boolean premium = false;
    private int usage = 0, totalSent = 0, jokeIndex = 0, tab = 0, themeId = 0;
    private int tasbih = 0, water = 0;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<String> notes = new ArrayList<>();
    private LinearLayout root;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private String calcExpr = "";
    private TextView calcDisplay, tasbihText, waterText;
    private long swStart = 0;
    private boolean swRunning = false;
    private TextView swText;
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
        applyTheme(); loadMessages(); loadNotes();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        if (userName.isEmpty()) showAccount(); else showMain();
    }

    private void applyTheme() {
        TEXT = Color.parseColor("#E8EEF7"); MUTED = Color.parseColor("#7A8BA3");
        if (themeId == 1) {
            BG = Color.parseColor("#070B16"); SURFACE = Color.parseColor("#0E1524"); CARD = Color.parseColor("#152033");
            ACCENT = Color.parseColor("#4DA3FF"); ACCENT_DIM = Color.parseColor("#0D2A4A");
            USER_BUBBLE = Color.parseColor("#1A2A45"); BOT_BUBBLE = Color.parseColor("#121C2C");
        } else if (themeId == 2) {
            BG = Color.parseColor("#0B0714"); SURFACE = Color.parseColor("#140F1F"); CARD = Color.parseColor("#1C152E");
            ACCENT = Color.parseColor("#B794F6"); ACCENT_DIM = Color.parseColor("#2A1B4A");
            USER_BUBBLE = Color.parseColor("#2A2040"); BOT_BUBBLE = Color.parseColor("#18122A");
        } else {
            BG = Color.parseColor("#070B14"); SURFACE = Color.parseColor("#0F1623"); CARD = Color.parseColor("#151D2E");
            ACCENT = Color.parseColor("#00E5A8"); ACCENT_DIM = Color.parseColor("#0A3D32");
            USER_BUBBLE = Color.parseColor("#1A2740"); BOT_BUBBLE = Color.parseColor("#121A28");
        }
    }

    private void showAccount() {
        root.removeAllViews(); root.setBackgroundColor(BG); root.setGravity(Gravity.CENTER); root.setPadding(dp(28),dp(28),dp(28),dp(28));
        TextView logo = new TextView(this); logo.setText("◆ NOVA"); logo.setTextSize(40); logo.setTypeface(Typeface.DEFAULT_BOLD); logo.setTextColor(ACCENT); logo.setGravity(Gravity.CENTER); root.addView(logo);
        TextView sub = new TextView(this); sub.setText("v1.5 — مسبحة + عداد ماء"); sub.setTextColor(MUTED); sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(12),0,dp(28)); root.addView(sub);
        EditText nameInput = new EditText(this); nameInput.setHint("اسمك"); nameInput.setTextColor(TEXT); nameInput.setHintTextColor(MUTED); nameInput.setBackground(rounded(CARD,18)); nameInput.setPadding(dp(18),dp(16),dp(18),dp(16)); nameInput.setSingleLine(true); root.addView(nameInput, matchWrap());
        Button btn = primaryBtn("ابدأ"); LinearLayout.LayoutParams bp = matchWrap(); bp.topMargin = dp(16); root.addView(btn, bp);
        btn.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this,"اكتب اسمك",Toast.LENGTH_SHORT).show(); return; }
            userName = n; prefs.edit().putString("user_name", userName).apply(); showMain();
        });
    }

    private void showMain() {
        root.removeAllViews(); root.setBackgroundColor(BG); root.setGravity(Gravity.TOP); root.setPadding(0,0,0,0);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        root.addView(content);
        if (tab==0) buildChat(content); else if (tab==1) buildTools(content); else if (tab==2) buildNotes(content); else buildAbout(content);
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setBackgroundColor(SURFACE); nav.setPadding(dp(6),dp(8),dp(6),dp(8));
        nav.addView(navBtn("محادثة",0), navWeight()); nav.addView(navBtn("أدوات",1), navWeight());
        nav.addView(navBtn("ملاحظات",2), navWeight()); nav.addView(navBtn("حولي",3), navWeight());
        root.addView(nav);
    }

    private LinearLayout.LayoutParams navWeight() { return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); }
    private Button navBtn(String label, int id) {
        Button b = new Button(this); b.setText(label); b.setTextSize(11);
        b.setTextColor(tab==id ? Color.parseColor("#04120E") : MUTED);
        b.setBackground(rounded(tab==id ? ACCENT : CARD, 12));
        b.setOnClickListener(v -> { tab = id; showMain(); });
        return b;
    }

    private LinearLayout rowTop(String titleText) {
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setBackgroundColor(SURFACE);
        top.setPadding(dp(14),dp(14),dp(10),dp(14)); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView title = new TextView(this); title.setText(titleText); title.setTextSize(20); title.setTypeface(Typeface.DEFAULT_BOLD); title.setTextColor(TEXT); col.addView(title);
        TextView online = new TextView(this); online.setText("● محلي • جاهز"); online.setTextSize(11); online.setTextColor(ACCENT); col.addView(online);
        top.addView(col); return top;
    }

    private void buildChat(LinearLayout content) {
        LinearLayout top = rowTop("NOVA AI");
        Button clearBtn = chipBtn("مسح"); clearBtn.setOnClickListener(v -> confirmClear()); top.addView(clearBtn); content.addView(top);
        int limit = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        TextView status = new TextView(this);
        status.setText("أهلاً " + userName + " | " + (premium?"Pro":"مجاني") + " " + usage + "/" + limit);
        status.setTextSize(12); status.setTextColor(MUTED); status.setPadding(dp(16),dp(8),dp(16),dp(4)); content.addView(status);
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); chips.setPadding(dp(12),dp(6),dp(12),dp(6));
        for (String q : new String[]{"نكتة","الوقت","نصيحة","دعاء","مساعدة"}) {
            Button chip = chipBtn(q); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); cp.setMarginEnd(dp(8));
            chips.addView(chip, cp); chip.setOnClickListener(v -> sendQuick(q, content));
        }
        hs.addView(chips); content.addView(hs);
        ScrollView scroll = new ScrollView(this); LinearLayout msgBox = new LinearLayout(this); msgBox.setOrientation(LinearLayout.VERTICAL); msgBox.setPadding(dp(12),dp(6),dp(12),dp(8));
        scroll.addView(msgBox); content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1)); refreshMessages(msgBox);
        LinearLayout bottom = new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setBackgroundColor(SURFACE); bottom.setPadding(dp(12),dp(10),dp(12),dp(10)); bottom.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new EditText(this); input.setHint("اكتب..."); input.setTextColor(TEXT); input.setHintTextColor(MUTED); input.setBackground(rounded(CARD,16)); input.setPadding(dp(14),dp(12),dp(14),dp(12)); input.setSingleLine(true);
        bottom.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button send = primaryBtn("إرسال"); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); sp.setMarginStart(dp(8)); bottom.addView(send, sp); content.addView(bottom);
        send.setOnClickListener(v -> { String t = input.getText().toString().trim(); if (t.isEmpty()) return; input.setText(""); doSend(t, status, msgBox, scroll, send); });
    }

    private void sendQuick(String text, LinearLayout content) {
        TextView status=null; ScrollView scroll=null; LinearLayout msgBox=null; Button sendBtn=null;
        for (int i=0;i<content.getChildCount();i++) {
            View c = content.getChildAt(i);
            if (c instanceof TextView) { String t=((TextView)c).getText().toString(); if (t.contains("أهلاً")||t.contains("اهلا")) status=(TextView)c; }
            if (c instanceof ScrollView) { scroll=(ScrollView)c; if (scroll.getChildCount()>0 && scroll.getChildAt(0) instanceof LinearLayout) msgBox=(LinearLayout)scroll.getChildAt(0); }
            if (c instanceof LinearLayout) { LinearLayout row=(LinearLayout)c; for (int j=0;j<row.getChildCount();j++) if (row.getChildAt(j) instanceof Button) { Button b=(Button)row.getChildAt(j); if ("إرسال".equals(b.getText().toString())||"...".equals(b.getText().toString())) sendBtn=b; } }
        }
        if (status!=null && msgBox!=null && scroll!=null && sendBtn!=null) doSend(text, status, msgBox, scroll, sendBtn);
        else { messages.add("أنت: "+text); messages.add("NOVA AI: "+localReply(text)); saveMessages(); showMain(); }
    }

    private void doSend(String text, TextView status, LinearLayout msgBox, ScrollView scroll, Button send) {
        int lim = premium ? PREMIUM_LIMIT : FREE_LIMIT;
        if (usage >= lim) { Toast.makeText(this,"انتهت الرسائل",Toast.LENGTH_SHORT).show(); return; }
        try { Vibrator vib=(Vibrator)getSystemService(VIBRATOR_SERVICE); if (vib!=null) vib.vibrate(15); } catch (Exception ignored) {}
        messages.add("أنت: "+text); usage++; totalSent++;
        prefs.edit().putInt("usage",usage).putInt("total_sent",totalSent).apply(); saveMessages();
        status.setText("أهلاً "+userName+" | "+(premium?"Pro":"مجاني")+" "+usage+"/"+lim);
        refreshMessages(msgBox); scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        send.setEnabled(false); send.setText("...");
        executor.execute(() -> {
            try { Thread.sleep(160+random.nextInt(250)); } catch (Exception ignored) {}
            String reply = localReply(text);
            mainHandler.post(() -> {
                messages.add("NOVA AI: "+reply); saveMessages(); refreshMessages(msgBox);
                scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN)); send.setEnabled(true); send.setText("إرسال");
            });
        });
    }

    private void buildTools(LinearLayout content) {
        content.addView(header("الأدوات"));
        ScrollView scroll = new ScrollView(this); LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(8),dp(16),dp(16));
        scroll.addView(box); content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));

        // مسبحة
        box.addView(section("مسبحة"));
        tasbihText = new TextView(this); tasbihText.setText(String.valueOf(tasbih)); tasbihText.setTextSize(42); tasbihText.setTextColor(ACCENT);
        tasbihText.setGravity(Gravity.CENTER); tasbihText.setBackground(rounded(CARD,16)); tasbihText.setPadding(dp(16),dp(20),dp(16),dp(20));
        box.addView(tasbihText, matchWrap());
        LinearLayout tRow = new LinearLayout(this); tRow.setOrientation(LinearLayout.HORIZONTAL); tRow.setPadding(0,dp(8),0,0);
        Button tPlus = primaryBtn("+ تسبيح"); Button tReset = chipBtn("تصفير");
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); hp.setMarginEnd(dp(8));
        tRow.addView(tPlus, hp); tRow.addView(tReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(tRow);
        tPlus.setOnClickListener(v -> { tasbih++; prefs.edit().putInt("tasbih", tasbih).apply(); tasbihText.setText(String.valueOf(tasbih)); vibrate(); });
        tReset.setOnClickListener(v -> { tasbih=0; prefs.edit().putInt("tasbih",0).apply(); tasbihText.setText("0"); });

        // ماء
        box.addView(section("عداد الماء (أكواب)"));
        waterText = new TextView(this); waterText.setText(water + " / 8"); waterText.setTextSize(28); waterText.setTextColor(TEXT);
        waterText.setGravity(Gravity.CENTER); waterText.setBackground(rounded(CARD,16)); waterText.setPadding(dp(16),dp(16),dp(16),dp(16));
        box.addView(waterText, matchWrap());
        LinearLayout wRow = new LinearLayout(this); wRow.setOrientation(LinearLayout.HORIZONTAL); wRow.setPadding(0,dp(8),0,0);
        Button wPlus = primaryBtn("+ كوب"); Button wReset = chipBtn("تصفير");
        wRow.addView(wPlus, hp); wRow.addView(wReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(wRow);
        wPlus.setOnClickListener(v -> { water++; prefs.edit().putInt("water", water).apply(); waterText.setText(water+" / 8"); if(water>=8) Toast.makeText(this,"أحسنت! وصلت هدف اليوم",Toast.LENGTH_SHORT).show(); });
        wReset.setOnClickListener(v -> { water=0; prefs.edit().putInt("water",0).apply(); waterText.setText("0 / 8"); });

        // حاسبة
        box.addView(section("آلة حاسبة"));
        calcDisplay = new TextView(this); calcDisplay.setText(calcExpr.isEmpty()?"0":calcExpr); calcDisplay.setTextSize(24); calcDisplay.setTextColor(TEXT);
        calcDisplay.setBackground(rounded(CARD,14)); calcDisplay.setPadding(dp(16),dp(14),dp(16),dp(14)); calcDisplay.setGravity(Gravity.END); box.addView(calcDisplay, matchWrap());
        String[][] rows = {{"7","8","9","÷"},{"4","5","6","×"},{"1","2","3","-"},{"0",".","C","+"},{"="}};
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setPadding(0,dp(6),0,0);
            for (String key : row) {
                Button b = chipBtn(key); b.setTextColor(TEXT);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); lp.setMarginEnd(dp(6));
                r.addView(b, lp); b.setOnClickListener(v -> onCalcKey(key));
            }
            box.addView(r);
        }

        // BMI
        box.addView(section("BMI"));
        EditText weight = field("الوزن كجم"); EditText height = field("الطول سم"); box.addView(weight); box.addView(height);
        TextView bmiOut = new TextView(this); bmiOut.setTextColor(TEXT); bmiOut.setPadding(0,dp(8),0,0); box.addView(bmiOut);
        Button bmiBtn = primaryBtn("احسب BMI"); LinearLayout.LayoutParams bp = matchWrap(); bp.topMargin=dp(8); box.addView(bmiBtn, bp);
        bmiBtn.setOnClickListener(v -> {
            try {
                double w = Double.parseDouble(weight.getText().toString().trim());
                double h = Double.parseDouble(height.getText().toString().trim())/100.0;
                double bmi = w/(h*h);
                String cat = bmi<18.5?"نحافة": bmi<25?"طبيعي": bmi<30?"زيادة وزن":"سمنة";
                bmiOut.setText(String.format("BMI = %.1f — %s", bmi, cat));
            } catch (Exception e) { bmiOut.setText("أدخل أرقام صحيحة"); }
        });

        // حرارة
        box.addView(section("تحويل حرارة"));
        EditText tempIn = field("درجة الحرارة"); box.addView(tempIn);
        TextView tempOut = new TextView(this); tempOut.setTextColor(TEXT); tempOut.setPadding(0,dp(8),0,0); box.addView(tempOut);
        LinearLayout trow = new LinearLayout(this); trow.setOrientation(LinearLayout.HORIZONTAL); trow.setPadding(0,dp(8),0,0);
        Button toF = primaryBtn("إلى F"); Button toC = primaryBtn("إلى C");
        trow.addView(toF, hp); trow.addView(toC, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(trow);
        toF.setOnClickListener(v -> { try { double c=Double.parseDouble(tempIn.getText().toString().trim()); tempOut.setText(String.format("%.1f °F", c*9/5+32)); } catch(Exception e){ tempOut.setText("رقم غير صالح"); }});
        toC.setOnClickListener(v -> { try { double f=Double.parseDouble(tempIn.getText().toString().trim()); tempOut.setText(String.format("%.1f °C", (f-32)*5/9)); } catch(Exception e){ tempOut.setText("رقم غير صالح"); }});

        // ساعة إيقاف
        box.addView(section("ساعة إيقاف"));
        swText = new TextView(this); swText.setText("00:00.00"); swText.setTextSize(30); swText.setTextColor(TEXT); swText.setGravity(Gravity.CENTER);
        swText.setBackground(rounded(CARD,14)); swText.setPadding(dp(16),dp(16),dp(16),dp(16)); box.addView(swText, matchWrap());
        LinearLayout swRow = new LinearLayout(this); swRow.setOrientation(LinearLayout.HORIZONTAL); swRow.setPadding(0,dp(8),0,0);
        Button swStartBtn = primaryBtn(swRunning?"إيقاف":"بدء"); Button swReset = chipBtn("تصفير");
        swRow.addView(swStartBtn, hp); swRow.addView(swReset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(swRow);
        swStartBtn.setOnClickListener(v -> {
            if (!swRunning) { swStart = System.currentTimeMillis(); swRunning = true; swStartBtn.setText("إيقاف"); mainHandler.post(swTick); }
            else { swRunning = false; swStartBtn.setText("بدء"); }
        });
        swReset.setOnClickListener(v -> { swRunning=false; swStartBtn.setText("بدء"); swText.setText("00:00.00"); });

        // عشوائي + مرور
        box.addView(section("عشوائي + مرور"));
        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        Button coin = primaryBtn("عملة"); Button dice = primaryBtn("نرد");
        row2.addView(coin, hp); row2.addView(dice, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(row2);
        TextView rnd = new TextView(this); rnd.setTextColor(MUTED); rnd.setGravity(Gravity.CENTER); rnd.setPadding(0,dp(10),0,0); box.addView(rnd);
        coin.setOnClickListener(v -> rnd.setText(random.nextBoolean()?"ملك":"كتابة"));
        dice.setOnClickListener(v -> rnd.setText("النرد: "+(random.nextInt(6)+1)));
        TextView passView = new TextView(this); passView.setText("كلمة المرور"); passView.setTextColor(TEXT); passView.setBackground(rounded(CARD,14)); passView.setPadding(dp(14),dp(14),dp(14),dp(14));
        LinearLayout.LayoutParams pp = matchWrap(); pp.topMargin=dp(12); box.addView(passView, pp);
        LinearLayout passRow = new LinearLayout(this); passRow.setOrientation(LinearLayout.HORIZONTAL); passRow.setPadding(0,dp(8),0,0);
        Button gen = primaryBtn("توليد"); Button copy = chipBtn("نسخ");
        passRow.addView(gen, hp); passRow.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); box.addView(passRow);
        gen.setOnClickListener(v -> passView.setText(genPass()));
        copy.setOnClickListener(v -> { ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("p", passView.getText().toString())); Toast.makeText(this,"تم النسخ",Toast.LENGTH_SHORT).show(); });
    }

    private void vibrate() {
        try { Vibrator vib=(Vibrator)getSystemService(VIBRATOR_SERVICE); if (vib!=null) vib.vibrate(12); } catch (Exception ignored) {}
    }

    private EditText field(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextColor(TEXT); e.setHintTextColor(MUTED);
        e.setBackground(rounded(CARD,12)); e.setPadding(dp(12),dp(12),dp(12),dp(12)); e.setSingleLine(true);
        LinearLayout.LayoutParams lp = matchWrap(); lp.topMargin = dp(6); e.setLayoutParams(lp); return e;
    }

    private String genPass() {
        String chars="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
        StringBuilder sb=new StringBuilder(); for(int i=0;i<14;i++) sb.append(chars.charAt(random.nextInt(chars.length()))); return sb.toString();
    }

    private void onCalcKey(String key) {
        if (key.equals("C")) calcExpr="";
        else if (key.equals("=")) { String res=tryMath(calcExpr.replace("÷","/").replace("×","*")); calcExpr = res!=null?res.replace("النتيجة: ",""):"خطأ"; }
        else { if (calcExpr.equals("خطأ")) calcExpr=""; calcExpr+=key; }
        if (calcDisplay!=null) calcDisplay.setText(calcExpr.isEmpty()?"0":calcExpr);
    }

    private void buildNotes(LinearLayout content) {
        content.addView(rowTop("الملاحظات"));
        ScrollView scroll = new ScrollView(this); LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(12),dp(16),dp(12));
        scroll.addView(box); content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        EditText noteInput = new EditText(this); noteInput.setHint("اكتب ملاحظة..."); noteInput.setTextColor(TEXT); noteInput.setHintTextColor(MUTED); noteInput.setBackground(rounded(CARD,14)); noteInput.setPadding(dp(14),dp(12),dp(14),dp(12)); box.addView(noteInput, matchWrap());
        Button add = primaryBtn("إضافة"); LinearLayout.LayoutParams ap = matchWrap(); ap.topMargin=dp(8); ap.bottomMargin=dp(12); box.addView(add, ap);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); box.addView(list); renderNotes(list);
        add.setOnClickListener(v -> { String t=noteInput.getText().toString().trim(); if(t.isEmpty())return; notes.add(0,t); saveNotes(); noteInput.setText(""); renderNotes(list); });
    }

    private void renderNotes(LinearLayout list) {
        list.removeAllViews();
        if (notes.isEmpty()) { TextView e=new TextView(this); e.setText("لا توجد ملاحظات"); e.setTextColor(MUTED); list.addView(e); return; }
        for (int i=0;i<notes.size();i++) {
            final int idx=i; final String note=notes.get(i);
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackground(rounded(CARD,14)); card.setPadding(dp(12),dp(12),dp(12),dp(12));
            LinearLayout.LayoutParams cp = matchWrap(); cp.bottomMargin=dp(8);
            TextView tv = new TextView(this); tv.setText(note); tv.setTextColor(TEXT); card.addView(tv);
            LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); actions.setPadding(0,dp(8),0,0);
            Button share = chipBtn("مشاركة"); Button del = chipBtn("حذف"); actions.addView(share); actions.addView(del); card.addView(actions);
            share.setOnClickListener(v -> { Intent in=new Intent(Intent.ACTION_SEND); in.setType("text/plain"); in.putExtra(Intent.EXTRA_TEXT, note); startActivity(Intent.createChooser(in,"مشاركة")); });
            del.setOnClickListener(v -> { notes.remove(idx); saveNotes(); renderNotes(list); });
            list.addView(card, cp);
        }
    }

    private void buildAbout(LinearLayout content) {
        ScrollView scroll = new ScrollView(this); LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(20),dp(20),dp(20));
        scroll.addView(box); content.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        TextView h = new TextView(this); h.setText("حولي"); h.setTextSize(26); h.setTypeface(Typeface.DEFAULT_BOLD); h.setTextColor(TEXT); box.addView(h);
        box.addView(infoCard("إحصائيات", "الاسم: "+userName+"\nالجلسة: "+usage+"\nالإجمالي: "+totalSent+
                "\nتسبيح: "+tasbih+"\nماء: "+water+"/8\nملاحظات: "+notes.size()+"\nالخطة: "+(premium?"Premium":"مجاني")));
        box.addView(section("الثيم"));
        LinearLayout themes = new LinearLayout(this); themes.setOrientation(LinearLayout.HORIZONTAL);
        themes.addView(themeBtn("أخضر",0), navWeight()); themes.addView(themeBtn("أزرق",1), navWeight()); themes.addView(themeBtn("بنفسجي",2), navWeight()); box.addView(themes);
        Button nameBtn = primaryBtn("تغيير الاسم"); LinearLayout.LayoutParams np=matchWrap(); np.topMargin=dp(16); box.addView(nameBtn,np);
        nameBtn.setOnClickListener(v -> changeName());
        if (!premium) {
            Button pro = primaryBtn("تفعيل Premium"); LinearLayout.LayoutParams pp=matchWrap(); pp.topMargin=dp(10); box.addView(pro,pp);
            pro.setOnClickListener(v -> { premium=true; prefs.edit().putBoolean("premium",true).apply(); showMain(); });
        }
        TextView ver = new TextView(this); ver.setText("NOVA AI v1.5 محلي"); ver.setTextColor(MUTED); ver.setGravity(Gravity.CENTER); ver.setPadding(0,dp(20),0,0); box.addView(ver);
    }

    private Button themeBtn(String label, int id) {
        Button b = chipBtn(label);
        if (themeId==id) { b.setBackground(rounded(ACCENT,12)); b.setTextColor(Color.parseColor("#04120E")); }
        b.setOnClickListener(v -> { themeId=id; prefs.edit().putInt("theme_id",themeId).apply(); applyTheme(); showMain(); });
        return b;
    }

    private LinearLayout infoCard(String title, String body) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackground(rounded(CARD,16)); card.setPadding(dp(16),dp(16),dp(16),dp(16));
        LinearLayout.LayoutParams lp=matchWrap(); lp.topMargin=dp(14); card.setLayoutParams(lp);
        TextView t=new TextView(this); t.setText(title); t.setTextColor(ACCENT); t.setTypeface(Typeface.DEFAULT_BOLD); card.addView(t);
        TextView b=new TextView(this); b.setText(body); b.setTextColor(MUTED); b.setPadding(0,dp(8),0,0); card.addView(b); return card;
    }

    private void changeName() {
        final EditText input = new EditText(this); input.setText(userName); input.setTextColor(TEXT); input.setPadding(dp(14),dp(12),dp(14),dp(12));
        new AlertDialog.Builder(this).setTitle("تغيير الاسم").setView(input)
            .setPositiveButton("حفظ",(d,w)->{ String n=input.getText().toString().trim(); if(!n.isEmpty()){ userName=n; prefs.edit().putString("user_name",userName).apply(); showMain(); }})
            .setNegativeButton("إلغاء",null).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("مسح").setMessage("مسح المحادثة؟")
            .setPositiveButton("مسح",(d,w)->{ messages.clear(); messages.add("NOVA AI: تم المسح."); saveMessages(); showMain(); })
            .setNegativeButton("إلغاء",null).show();
    }

    private void refreshMessages(LinearLayout box) {
        box.removeAllViews();
        for (String m : messages) {
            boolean isUser = m.startsWith("أنت:");
            TextView tv = new TextView(this); tv.setText(m); tv.setTextColor(TEXT); tv.setTextSize(15);
            tv.setBackground(rounded(isUser?USER_BUBBLE:BOT_BUBBLE,16)); tv.setPadding(dp(14),dp(12),dp(14),dp(12));
            LinearLayout.LayoutParams lp=matchWrap(); lp.bottomMargin=dp(10); box.addView(tv, lp);
        }
    }

    private String localReply(String message) {
        String m = message.toLowerCase().trim();
        if (has(m,"مع السلامة","باي","وداع","bye","إلى اللقاء","الى اللقاء")) return "مع السلامة "+userName+"!";
        if (has(m,"مباراة","ماتش","نتيجة","نتائج","دوري","هدف","يلعب","بيلعب","فريق"))
            return "ما عندي نتائج مباريات مباشرة. افتح تطبيق رياضي.";
        if (has(m,"سلام","هلا","مرحبا","hello","hi")) return "وعليكم السلام "+userName+"!";
        if (has(m,"كيف حالك","كيفك","شلونك")) return "بخير! وأنت؟";
        if (has(m,"اسمك","من أنت","من انت")) return "أنا NOVA AI v1.5 — مسبحة، ماء، BMI، حاسبة، ملاحظات وأكثر.";
        if (has(m,"شكرا","شكرًا","مشكور")) return "العفو!";
        if (has(m,"مساعدة","ساعدني","help")) return "محادثة • أدوات (مسبحة/ماء/BMI/حرارة/حاسبة) • ملاحظات • حولي";
        if (has(m,"مسبحة","تسبيح","سبحان")) return "من تبويب أدوات → مسبحة. العدد الحالي: "+tasbih;
        if (has(m,"ماء","كوب")) return "من أدوات → عداد الماء. الحالي: "+water+"/8";
        if (has(m,"وقت","ساعه","ساعة","كم الساعه","كم الساعة")) {
            Calendar c=Calendar.getInstance(); return String.format("الوقت: %02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }
        if (has(m,"كم تاريخ","ما التاريخ","التاريخ") || m.equals("تاريخ")) {
            Calendar c=Calendar.getInstance(); return c.get(Calendar.DAY_OF_MONTH)+"/"+(c.get(Calendar.MONTH)+1)+"/"+c.get(Calendar.YEAR);
        }
        if (has(m,"نكتة","نكته","اضحكني")) { String[] j={"ليش الكمبيوتر راح للدكتور؟ عنده فيروس! 😄","بايثون في مطعم: SyntaxError 😂"}; return j[jokeIndex++%j.length]; }
        if (has(m,"نصيحة","تحفيز")) return "خطوة صغيرة كل يوم أفضل من حماسة يوم واحد.";
        if (has(m,"دعاء")) return "اللهم يسّر وأعن.";
        String calc = tryMath(m.replace("÷","/").replace("×","*"));
        if (calc != null) return calc;
        return "فهمت: «"+message+"» — جرب التبويبات.";
    }

    private String tryMath(String m) {
        try {
            m=m.replace(" ","");
            if (m.contains("+")) { String[] p=m.split("\\+"); if(p.length==2) return "النتيجة: "+(Double.parseDouble(p[0])+Double.parseDouble(p[1])); }
            if (m.contains("-") && m.indexOf('-')>0) { String[] p=m.split("-"); if(p.length==2) return "النتيجة: "+(Double.parseDouble(p[0])-Double.parseDouble(p[1])); }
            if (m.contains("*")) { String[] p=m.split("\\*"); if(p.length==2) return "النتيجة: "+(Double.parseDouble(p[0])*Double.parseDouble(p[1])); }
            if (m.contains("/")) { String[] p=m.split("/"); if(p.length==2 && Double.parseDouble(p[1])!=0) return "النتيجة: "+(Double.parseDouble(p[0])/Double.parseDouble(p[1])); }
        } catch (Exception ignored) {}
        return null;
    }

    private TextView header(String t){ TextView v=new TextView(this); v.setText(t); v.setTextSize(24); v.setTypeface(Typeface.DEFAULT_BOLD); v.setTextColor(TEXT); v.setPadding(dp(16),dp(18),dp(16),dp(8)); return v; }
    private TextView section(String t){ TextView v=new TextView(this); v.setText(t); v.setTextColor(ACCENT); v.setTypeface(Typeface.DEFAULT_BOLD); v.setPadding(0,dp(14),0,dp(8)); return v; }
    private boolean has(String text,String... keys){ for(String k:keys) if(text.contains(k.toLowerCase())) return true; return false; }
    private Button primaryBtn(String text){ Button b=new Button(this); b.setText(text); b.setTextColor(Color.parseColor("#04120E")); b.setTypeface(Typeface.DEFAULT_BOLD); b.setBackground(rounded(ACCENT,16)); b.setPadding(dp(14),dp(12),dp(14),dp(12)); return b; }
    private Button chipBtn(String text){ Button b=new Button(this); b.setText(text); b.setTextColor(ACCENT); b.setTextSize(12); b.setBackground(rounded(CARD,14)); b.setPadding(dp(10),dp(6),dp(10),dp(6)); return b; }
    private GradientDrawable rounded(int color,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g; }
    private LinearLayout.LayoutParams matchWrap(){ return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private void loadMessages(){ messages.clear(); String raw=prefs.getString("messages",null); if(raw==null){ messages.add("NOVA AI: أهلًا "+userName+"!\nv1.5 — مسبحة وعداد ماء في الأدوات."); return;} try{ JSONArray a=new JSONArray(raw); for(int i=0;i<a.length();i++) messages.add(a.optString(i)); }catch(Exception e){ messages.add("NOVA AI: مرحبًا!"); } }
    private void saveMessages(){ try{ JSONArray a=new JSONArray(); for(String m:messages) a.put(m); prefs.edit().putString("messages",a.toString()).apply(); }catch(Exception ignored){} }
    private void loadNotes(){ notes.clear(); String raw=prefs.getString("notes",null); if(raw==null)return; try{ JSONArray a=new JSONArray(raw); for(int i=0;i<a.length();i++) notes.add(a.optString(i)); }catch(Exception ignored){} }
    private void saveNotes(){ try{ JSONArray a=new JSONArray(); for(String n:notes) a.put(n); prefs.edit().putString("notes",a.toString()).apply(); }catch(Exception ignored){} }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
}
