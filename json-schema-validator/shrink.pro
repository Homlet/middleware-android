-dontoptimize
-dontobfuscate

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

-keep public class com.google.common.annotations.Beta { public *; }
-keep public class com.google.common.annotations.VisibleForTesting { public *; }
-keep public class com.google.common.base.** { public *; }
-keep public class com.google.common.cache.** { public *; }
-keep public class com.google.common.collect.ArrayListMultimap { public *; }
-keep public class com.google.common.collect.ImmutableList { public *; }
-keep public class com.google.common.collect.ImmutableMap { public *; }
-keep public class com.google.common.collect.ImmutableMultimap { public *; }
-keep public class com.google.common.collect.ImmutableSet { public *; }
-keep public class com.google.common.collect.ListMultimap { public *; }
-keep public class com.google.common.collect.Lists { public *; }
-keep public class com.google.common.collect.Maps { public *; }
-keep public class com.google.common.collect.Queues { public *; }
-keep public class com.google.common.collect.Sets { public *; }
-keep public class com.google.common.escape.Escaper { public *; }
-keep public class com.google.common.io.Closer { public *; }
-keep public class com.google.common.net.InetAddresses { public *; }
-keep public class com.google.common.net.InternetDomainName { public *; }
-keep public class com.google.common.net.MediaType { public *; }
-keep public class com.google.common.net.PercentEscaper { public *; }
-keep public class com.google.common.primitives.Longs { public *; }

-keep public class com.google.i18n.phonenumbers.NumberParseException { public *; }
-keep public class com.google.i18n.phonenumbers.PhoneNumberUtil { public *; }

-keep public class com.fasterxml.jackson.databind.JsonNode { public *; }
-keep public class com.fasterxml.jackson.databind.JsonMappingException { public *; }
-keep public class com.fasterxml.jackson.databind.JsonParseException { public *; }
-keep public class com.fasterxml.jackson.databind.ObjectMapper { public *; }
-keep public class com.fasterxml.jackson.databind.ObjectReader { public *; }
-keep public class com.fasterxml.jackson.databind.ObjectWriter { public *; }
-keep public class com.fasterxml.jackson.databind.MappingIterator { public *; }
-keep public class com.fasterxml.jackson.databind.annotation.** { public *; }
-keep public class com.fasterxml.jackson.databind.node.** { public *; }
-keep public class com.fasterxml.jackson.databind.ser.std.ToStringSerializer { public *; }

-keep public class javax.mail.internet.AddressException { public *; }
-keep public class javax.mail.internet.InternetAddress { public *; }

-keep public class org.joda.time.format.DateTimeFormatter { public *; }
-keep public class org.joda.time.format.DateTimeFormatterBuilder { public *; }
-keep public class org.joda.time.format.DateTimeParser { public *; }

-keep public class org.mozilla.javascript.Context { public *; }
-keep public class org.mozilla.javascript.Function { public *; }
-keep public class org.mozilla.javascript.Scriptable { public *; }

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