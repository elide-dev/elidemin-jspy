-keep public class com.github.ajalt.clikt.** { *; }
-keep public class com.github.ajalt.mordant.terminal.terminalinterface.nativeimage.TerminalInterfaceProviderNativeImage { *; }
-keep public class elidemin.dev.elide.ApplicationKt {
    public static void main(java.lang.String[]);
}
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepparameternames
-keepattributes Exceptions,InnerClasses,Signature,Record,PermittedSubclasses,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,Synthetic,
                MethodParameters,LocalVariableTable,LocalVariableTypeTable

-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF,META-INF/services/*,META-INF/native-image/*,META-INF/native-image/*/*

-dontwarn org.graalvm.**
-dontwarn com.github.ajalt.mordant.terminal.terminalinterface.nativeimage.**
-dontwarn com.github.ajalt.mordant.internal.**
-dontwarn kotlinx.coroutines.debug.internal.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn android.**

-dontnote android.**
-dontnote jdk.internal.**
-dontnote kotlinx.coroutines.internal.**
-dontnote kotlin.coroutines.jvm.internal.**
-dontnote kotlin.internal.**
