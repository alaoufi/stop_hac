# Room and Kotlin metadata are handled by their own consumer rules. We keep the
# enum names because they are persisted by name in the database.
-keepclassmembers enum com.privacyshield.monitor.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep model classes used as Room entities / reflection targets intact.
-keep class com.privacyshield.monitor.core.model.** { *; }
-keep class com.privacyshield.monitor.data.db.** { *; }
