# Consumer R8/ProGuard rules for the Amazon Connect Chat SDK. These are packaged in the AAR and
# applied automatically when an app enables minification.

# --- Retrofit ---
# Retrofit 2.9.0 ships no R8 rules. These match the rules Retrofit bundles from 2.10 onwards.
# Without them, R8 full mode (the default since AGP 8) strips the generic signatures Retrofit
# reads, and calls fail with "Unable to create call adapter for interface retrofit2.Call".

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a
# Proxy and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not kept. Suspend
# functions are wrapped in continuations where the type argument is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# --- Gson-serialized request bodies ---
# The metrics request body is serialized by Gson using field names, so the fields must keep
# their names (and must not be removed) for the JSON keys to stay correct.
-keepclassmembers class com.amazon.connect.chat.sdk.model.MetricRequestBody { <fields>; }
-keepclassmembers class com.amazon.connect.chat.sdk.model.Metric { <fields>; }
-keepclassmembers class com.amazon.connect.chat.sdk.model.Dimension { <fields>; }
