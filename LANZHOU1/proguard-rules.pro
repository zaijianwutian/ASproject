# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:/Android/adt-bundle-windows-x86-20140624/adt-bundle-windows-x86-20140624/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5          # ָ�������ѹ������
-dontusemixedcaseclassnames   # �Ƿ�ʹ�ô�Сд���
-dontpreverify           # ����ʱ�Ƿ���ԤУ��
-verbose                # ����ʱ�Ƿ��¼��־
-dontwarn com.umeng.**
#����com.umeng.**��������������������з�������������(û�����˵ļ���ʱɾ���˾�)
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # ����ʱ�����õ��㷨

-keep public class * extends android.app.Application   # ������Щ�಻������
-keep public class * extends android.app.Service       # ������Щ�಻������
-keep public class * extends android.content.BroadcastReceiver  # ������Щ�಻������
-keep public class * extends android.content.ContentProvider    # ������Щ�಻������
-keep public class * extends android.app.backup.BackupAgentHelper # ������Щ�಻������
-keep public class * extends android.preference.Preference        # ������Щ�಻������
-keep public class com.android.vending.licensing.ILicensingService    # ������Щ�಻������

-keepclasseswithmembernames class * {  # ���� native ������������
    native <methods>;
}
-keepclasseswithmembers class * {   # �����Զ���ؼ��಻������
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {# �����Զ���ؼ��಻������
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity {  # �����Զ���ؼ��಻������
    public void *(android.view.View);
}
-keepclassmembers enum * {     # ����ö�� enum �಻������
 public static **[] values();
 public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable { # ���� Parcelable ��������
 public static final android.os.Parcelable$Creator *;
}