# NOVA AI v0.5

تطبيق محادثة ذكية تجريبي لأندرويد — جاهز للبناء السحابي.

## ما الجديد في 0.5
- واجهة داكنة محسّنة (فقاعات محادثة، شريط استخدام، تمرير تلقائي).
- دعم HTTP للتطوير (`network_security_config` + `usesCleartextTraffic`).
- رسائل خطأ أوضح عند فشل الاتصال بالخادم.
- شاشة خطط أوضح مع تنبيه عند نفاد الاستخدامات.
- مشروع مكتمل البنية (Gradle Wrapper properties، themes، strings).
- GitHub Actions جاهز للبناء من الجوال.

## الميزات الحالية
| الميزة | الحالة |
|--------|--------|
| محادثة مع Backend | ✅ |
| حفظ المحادثة محليًا | ✅ |
| حساب محلي (اسم فقط) | ✅ تجريبي |
| Free: 10 استخدامات | ✅ |
| Premium تجريبي: 100 | ✅ (بدون دفع) |
| بناء APK عبر GitHub | ✅ |

## مهم
- الحساب **محلي تجريبي** وليس نظام مصادقة حقيقي.
- لا يوجد دفع حقيقي بعد.
- قبل الإطلاق: Backend + مصادقة + Google Play Billing + HTTPS.

## إعداد Backend URL
في الملف:
`app/src/main/java/com/novaai/app/MainActivity.kt`

```kotlin
private const val BACKEND_URL = "http://10.0.2.2:3000"
```

| البيئة | العنوان |
|--------|---------|
| محاكي أندرويد | `http://10.0.2.2:3000` |
| جهاز حقيقي (نفس الشبكة) | `http://IP_جهازك:3000` |
| خادم سحابي | `https://your-api.com` |

الـ Backend المتوقع يستقبل:
```
POST /api/chat
Content-Type: application/json
{"message": "نص المستخدم"}

الرد:
{"reply": "نص الرد"}
```

## البناء من الجوال (GitHub Actions)
1. أنشئ مستودعًا جديدًا في GitHub.
2. ارفع **محتويات** مجلد `NOVA_AI` إلى جذر المستودع (ليس المجلد نفسه).
3. افتح تبويب **Actions** → شغّل **Build NOVA AI APK**.
4. بعد النجاح نزّل الـ Artifact: `NOVA-AI-debug-apk`.

انظر أيضًا: `BUILD_FROM_PHONE_AR.md`

## التشغيل المحلي
افتح المجلد في **Android Studio** → Run على Emulator أو جهاز.

## الخطوات القادمة المقترحة
1. ربط Backend حقيقي (Node / Python / ...).
2. مصادقة مستخدمين (Firebase Auth أو JWT).
3. Google Play Billing + تحقق الاشتراك من الخادم.
4. أيقونة تطبيق مخصصة + splash.
5. نسخة Release موقّعة.
