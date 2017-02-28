//proguardOptions in Android ++= Seq("-dontobfuscate", "-dontoptimize")
//proguardOptions in Android ++= Seq("-keepattributes Signature", "-dontwarn sun.misc.Unsafe", "-dontwarn scala.collection.**")
import com.github.aafa.RealmPlugin

enablePlugins(RealmPlugin, android.protify.Plugin)
name := "Post-it-Note"
scalaVersion := "2.11.7"

minSdkVersion in Android := "16"
targetSdkVersion in Android := "23"
platformTarget in Android := "android-23"

compileOrder := CompileOrder.JavaThenScala

// Override the run task with the android:run
//run <<= run in Android

javacOptions ++= Seq(
  "-processor", "io.realm.processor.RealmProcessor", "-proc:only",
  "-source", "1.7",
  "-target", "1.7",
  "-Xlint:unchecked",
  "-Xlint:deprecation"
)
javaOptions in Android := Seq("-Xmx2G -XX:MaxPermSize=702M -XX:ReservedCodeCacheSize=256 -XX:+CMSClassUnloadingEnabled -XX:+UseCodeCacheFlushing"),


scalacOptions ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xcheckinit"
)
lazy val proguardDebug = Seq(
    "-dontobfuscate",
    "-dontoptimize",
    "-dontpreverify"
  )
resolvers += "Local android extras" at "file:///" + System.getenv("ANDROID_HOME") + "/extras/android/m2repository"
resolvers += Resolver.jcenterRepo
proguardScala in Android := true
proguardOptions in Android ++= scala.reflect.io.File("project/proguard.pro").lines().toSeq
proguardOptions in Android ++= proguardDebug

scalacOptions ++= Seq("-feature", "-deprecation")
proguardScala in Android := true
useProguard in Android := true

libraryDependencies ++= Seq(
  "net.sf.proguard" % "proguard-base" % "5.1",
  "com.android.support" % "appcompat-v7" % "23.1.0",
  "com.android.support" % "support-v4" % "23.1.0",
  "com.android.support" % "recyclerview-v7" % "23.1.0",
  "com.android.support" % "cardview-v7" % "23.1.0",
  "com.android.support" % "design" % "23.1.0",
  "io.realm" % "realm-android" % "0.87.4",
  "com.evernote" % "android-sdk" % "2.0.0-RC3",
  "com.astuetz" % "pagerslidingtabstrip" % "1.0.1",
  "com.prolificinteractive" % "material-calendarview" % "0.8.1",
  "com.squareup.picasso" % "picasso" % "2.5.0",
  "com.github.chrisbanes.photoview" % "library" % "1.2.3",
  "com.h6ah4i.android.widget.advrecyclerview" % "advrecyclerview" % "0.6.1"
)

//proguardOptions in Android ++= Seq(
//  "-ignorewarnings",
//  "-keep class scala.Dynamic",
//  "-keep class com.android.support.**")

