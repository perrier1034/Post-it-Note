//import android.Dependencies
//import android.Keys._
//import com.github.aafa.RealmPlugin
//import sbt.Keys._
//import sbt._
//
//object Build extends android.AutoBuild {
//
//  lazy val app = {
//    Project(id = "app", base = file(".")).settings(appsSettings ++ Dependencies.libs: _*).enablePlugins(RealmPlugin, android.protify.Plugin)
//  }
//
//
//  lazy val appsSettings = Seq(
//    name := "realm-test",
//    versionName in Android := Some("1.0.0"),
//    scalaVersion := Versions.scalaVersion,
//    platformTarget in Android := Versions.platformTarget,
//
//    proguardScala in Android := true,
//    proguardOptions in Android ++= scala.reflect.io.File("project/proguard.pro").lines().toSeq,
//    proguardOptions in Android ++= Settings.proguardDebug,
//
//    scalacOptions ++= Seq("-feature", "-deprecation"),
//    minSdkVersion in Android := "21",
//
//    run <<= run in Android,
//    test <<= test in Android,
//
//    javacOptions in Compile ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation"),
//    javaOptions in Android := Seq("-Xmx2G -XX:MaxPermSize=702M -XX:ReservedCodeCacheSize=256 -XX:+CMSClassUnloadingEnabled -XX:+UseCodeCacheFlushing"),
//
//    dexShards in Android := true,
//    dexMaxHeap in Android := "4G",
//    packagingOptions in Android := PackagingOptions(excludes = Settings.apkExcludeStrings)
//
//  )
//
//}