
val Version = new {
  val plugin  = "0.1.0"
  val sbt13   = "0.13.6"
  val sbt10   = "1.1.0"
  val config  = "1.3.1"
}

val commonSettings = Seq(
  version := Version.plugin,
  organization := "de.surfice",
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  crossSbtVersions := Seq(Version.sbt13, Version.sbt10)
)

lazy val plugin = project
  .in(file("."))
  .settings(commonSettings ++ publishingSettings: _*)
  .settings(
    name := "sbt-package-conf",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % Version.config
      )
  )


lazy val publishingSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/jokade/sbt-package-conf</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jokade/sbt-package-conf</url>
      <connection>scm:git:git@github.com:jokade/sbt-package-conf.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jokade</id>
        <name>Johannes Kastner</name>
        <email>jokade@karchedon.de</email>
      </developer>
    </developers>
  )
)
 
lazy val dontPublish = Seq(
    publish := {},
    publishLocal := {},
    com.typesafe.sbt.pgp.PgpKeys.publishSigned := {},
    com.typesafe.sbt.pgp.PgpKeys.publishLocalSigned := {},
    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository",file("target/unusedrepo")))
  )

lazy val scriptedSettings = ScriptedPlugin.scriptedSettings ++ Seq(
  scriptedLaunchOpts += "-Dplugin.version=" + version.value,
  scriptedBufferLog := false
)
