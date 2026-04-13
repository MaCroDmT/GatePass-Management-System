# Add project specific ProGuard rules here.
# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firestore Models
-keep class com.sonia.gatepass.data.model.** { *; }
