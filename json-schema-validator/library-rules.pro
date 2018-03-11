# ===================== BEGIN DEPENDENCIES CONFIGURATION =====================

# General.
-dontnote android.net.http.*
-dontnote org.apache.http.**

-dontwarn build.**
-dontwarn java.awt.**
-dontwarn java.bean.**
-dontwarn java.lang.ClassValue
-dontwarn javax.swing.**
-dontwarn javax.activation.**
-dontwarn javax.lang.model.element.Modifier
-dontwarn com.sun.activation.**
-dontwarn sun.misc.Unsafe
-dontwarn org.joda.convert.**
-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.w3c.dom.**

# ====================== END DEPENDENCIES CONFIGURATION ======================

# ======================== BEGIN LIBRARY CONFIGURATION =======================

# Preserve all annotations.

-keepattributes *Annotation*

# Preserve all public classes, and their public and protected fields and
# methods.

-keep public class * {
    public protected *;
}

# Preserve all .class method names.

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve the special static methods that are required in all enumeration
# classes.

-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your library doesn't use serialization.
# If your code contains serializable classes that have to be backward
# compatible, please refer to the manual.

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================= END LIBRARY CONFIGURATION ========================
