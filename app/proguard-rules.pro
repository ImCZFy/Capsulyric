-keep class com.hchen.superlyricapi.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Prevent stripping of internal coroutines classes
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }

-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }

# Strip logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Keep all Attributes (Annotations, Signatures, etc.)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, Exceptions, SourceFile, LineNumberTable

-keep class android.** { *; }
-keep interface android.** { *; }
-keep class com.android.** { *; }
-keep interface com.android.** { *; }
-keep class dalvik.** { *; }
-keep interface dalvik.** { *; }

# Keep AIDL interfaces and IPC related classes
-keep class * extends android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }
-keep class * extends android.os.Parcelable { *; }

# Keep Shizuku Entry Point & Service Implementation
# Ensure the class name invoked by shell command remains unchanged
-keep class **.UserService { *; }
-keep class **.PrivilegedServiceImpl { *; }
-keep class **.IPrivilegedService { *; }
-keep class **.IPrivilegedService$Stub { *; }
-keep class **.IPrivilegedService$Default { *; }

# Allow reflection for basic types
-keepnames class * {
    native <methods>;
}
