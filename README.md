<div dir="rtl">

# 🛡️ درع الخصوصية — Privacy Shield

تطبيق أندرويد احترافي لمراقبة الخصوصية وحماية الكاميرا والميكروفون، **يعمل محلياً بالكامل** على الجهاز دون إرسال أي بيانات إلى أي خادم خارجي.

> **ملاحظة مهمة:** هذا التطبيق **نظام مراقبة وحماية للخصوصية** (Privacy Monitor & Shield) وليس مضاد فيروسات. يراقب استخدام الكاميرا والميكروفون والموقع والصلاحيات الحساسة، ويشرح لك ما يحدث بلغة بسيطة، مع الالتزام الصريح بقيود نظام أندرويد.

## المزايا الرئيسية

- **لوحة تحكم مباشرة**: حالة الكاميرا والميكروفون والموقع والبلوتوث وNFC ومشاركة الشاشة، ومستوى الأمان الحالي (منخفض / متوسط / مرتفع)، وآخر الأحداث.
- **مراقبة فورية للكاميرا والميكروفون**: كشف لحظي لأي تطبيق يشغّل الكاميرا أو الميكروفون عبر واجهة `AppOpsManager` الرسمية في أندرويد.
- **تحليل ذكي للسبب**: لا يكتفي بعرض «يوجد خطر»، بل يشرح **لماذا** — مثال: «استخدم التطبيق الميكروفون ١٨ دقيقة بينما الشاشة مغلقة دون مكالمة أو تسجيل معروف، لذلك صُنّف مريباً».
- **تصنيف الخطورة**: 🟢 طبيعي · 🟡 يحتاج انتباه · 🟠 مريب · 🔴 خطر جداً — مع سبب واضح لكل تصنيف.
- **القائمة البيضاء (استثناءات)**: لكل تطبيق ومستشعر: السماح دائماً / أثناء الفتح / أثناء تشغيل الشاشة / منع بالخلفية / منع دائماً.
- **سجل أحداث كامل**: تاريخ ووقت ومدة وحالة الخلفية ومستوى الخطورة، مع بحث وتصفية وتصدير CSV.
- **مراقبة الصلاحيات**: يعرض أي تطبيق يملك صلاحية حساسة، ويكشف الصلاحيات الجديدة الممنوحة ويشرح لماذا قد تكون خطيرة.
- **وضع الحماية القصوى**: يرفع مستوى التنبيه لأقصى درجة ويوجهك لمفاتيح مستشعرات النظام.
- **تقارير أسبوعية وشهرية**: أكثر التطبيقات استخداماً لكل مستشعر، وأعداد المحاولات حسب الخطورة، وتوصيات لتحسين الخصوصية.
- **خصوصية مطلقة**: لا خوادم، لا سحابة، لا إعلانات، لا تتبع. كل السجلات محلية على الجهاز.
- **واجهة عربية/إنجليزية** حديثة مع دعم الوضع الداكن ورسوم بيانية.

## قيود أندرويد (بصراحة)

لا يسمح أندرويد لتطبيق عادي بـ**منع** وصول تطبيق آخر للكاميرا أو الميكروفون. لذلك عندما يتعذر المنع المباشر، يقوم التطبيق بـ:
1. اكتشاف النشاط وإبلاغك فوراً مع شرح واضح.
2. تقديم خيارات مثل فتح إعدادات التطبيق لإيقافه أو إزالة الصلاحية.
3. توجيهك إلى مفاتيح مستشعرات النظام (أندرويد ١٢+) التي تقطع الطاقة فعلياً عن الكاميرا/الميكروفون.

الكشف الفوري متاح على **أندرويد ١١+**؛ وعلى الإصدارات الأقدم يعتمد التطبيق على مراقبة الصلاحيات والحالة القابلة للقراءة.

</div>

---

## Building the APK

The project builds with the standard Android/Gradle toolchain.

```bash
# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (minified + resource-shrunk)
./gradlew assembleRelease

# Unit tests (pure risk-engine logic)
./gradlew testDebugUnitTest
```

A **GitHub Actions** workflow (`.github/workflows/android.yml`) builds the debug APK and runs the tests on every push, and uploads the APK as a build artifact.

### Requirements
- JDK 17
- Android SDK with platform `android-35` and build-tools `35.0.0`
- `minSdk = 26` (Android 8.0), `targetSdk = 35`

## Architecture

Clean, layered, and dependency-injection-light (a hand-rolled `AppContainer`, no annotation-processor DI) — fast to build and easy to follow.

```
core/            Pure domain: models + the RiskAnalyzer rule engine (no Android deps, unit-tested)
  analysis/      RiskAnalyzer, AccessContext — turns observations into severity + a reason
  model/         SecurityEvent, RiskLevel, SensorType, WhitelistRule, TrackedPermission
  util/          CsvExporter
data/
  db/            Room database: events + whitelist, DAOs, converters
  prefs/         DataStore-backed settings
  repo/          EventRepository, WhitelistRepository, AppRepository
monitor/         The engine:
  SensorAccessMonitor   AppOpsManager.startWatchingActive → real-time cam/mic/location detection
  MonitorService        Foreground service wiring detection → analysis → persistence → alerts
  PermissionScanner     Periodic permission-grant diffing
  PeriodicScanWorker    Battery-friendly WorkManager job (scan + retention)
  MaxProtectionController, DeviceState, DeviceStatusProvider, BootReceiver
notify/          Notifier — channels + risk-scaled alerts
ui/              Jetpack Compose (Material 3): dashboard, events, permissions, reports,
                 settings, whitelist, about — ViewModels + a shared factory
```

### How real-time detection works
`AppOpsManager.startWatchingActive(...)` (API 30+) is the only officially supported way a non-privileged app can learn, in real time, that **any** app started/stopped using the camera, microphone or location. The `MonitorService` registers a watcher, and on each change it builds an `AccessContext` (foreground? screen on? in a call? whitelisted? duration? background frequency?), runs the pure `RiskAnalyzer`, persists a `SecurityEvent`, updates the live dashboard state, and posts an alert whose urgency scales with the assessed risk.

### Privacy by construction
There is **no networking code** in the app. All data lives in an on-device Room database and DataStore, both excluded from cloud backup and device transfer. CSV export is shared only where the user explicitly chooses via the system share sheet.

## License

See [LICENSE](LICENSE).
