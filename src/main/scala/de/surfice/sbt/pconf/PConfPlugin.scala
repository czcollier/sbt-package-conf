// Copyright (c) 2018. Distributed under the MIT License (see included LICENSE file).
package de.surfice.sbt.pconf

import java.io.{File => _, _}
import java.net.JarURLConnection

import sbt._
import Keys._
import com.typesafe.config.{Config, ConfigFactory}

object PConfPlugin extends AutoPlugin {

  object autoImport {
    val pconfConfig: TaskKey[Config] =
      taskKey[Config]("Configuration loaded from package.conf files in libraries")

    val pconfConfigString: TaskKey[String] =
      taskKey[String]("Concatenation of all package.conf and project.conf files")

    val pconfConfigFile: SettingKey[File] =
      settingKey[File]("Project-specific pconf configuration file")

    val pconfDefaultConfigPrefix: SettingKey[String] =
      settingKey[String]("Prefix for configuration files to be loaded first (can be used to enforce a default config to be loaded first)")

    val pconfPlatform: SettingKey[Option[String]] =
      settingKey[Option[String]]("If defined, load platform-specific configuration files after default config files (otherwise, no platform-specific files will be loaded)")

    object PConfPlatform {
      val Linux: Option[String] = Some("linux")
      val MacOS: Option[String] = Some("macos")
      val Windows: Option[String] = Some("win")
    }
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    pconfDefaultConfigPrefix := "",
    pconfPlatform := None,
    pconfConfigFile := baseDirectory.value / "project.conf",

    pconfConfigString :=
    loadPackageConfigs((dependencyClasspath in Compile).value, pconfConfigFile.value, pconfDefaultConfigPrefix.value, pconfPlatform.value)
      .foldLeft(""){ (s,in) =>
        s + "# SOURCE: "+in._1+"\n"+
        IO.readLines(new BufferedReader(new InputStreamReader(in._2))).mkString("\n") + "\n\n"
      },

    pconfConfig :=
      ConfigFactory.parseString(pconfConfigString.value).resolve()
  )


  private def loadPackageConfigs(dependencyClasspath: Classpath, projectConfig: File, defaultPrefix: String, platform: Option[String]): Seq[(String,InputStream)] =
    loadDepPackageConfigs(dependencyClasspath,defaultPrefix,platform) ++ loadProjectConfig(projectConfig)

  private def loadProjectConfig(projectConfig: File): Option[(String,InputStream)] =
    if(projectConfig.canRead)
      Some((projectConfig.getAbsolutePath,fin(projectConfig)))
    else None

  private def loadDepPackageConfigs(cp: Classpath, defaultPrefix: String, platform: Option[String]): Seq[(String,InputStream)] = {
    val (dirs,jars) = cp.files.partition(_.isDirectory)
    loadJarPackageConfigs(jars, defaultPrefix) ++
      { if(platform.isDefined) loadJarPackageConfigs(jars,defaultPrefix,platform) else Nil }
    //++ loadDirPackageConfigs(dirs,log)
  }

  private def loadJarPackageConfigs(jars: Seq[File], defaultPrefix: String, suffix: Option[String] = None): Seq[(String,InputStream)] = {
    val file = suffix.map("package_"+_+".conf").getOrElse("package.conf")
    val files = jars
      .map( f => (f.getName, new URL("jar:" + f.toURI + "!/" +file).openConnection()) )
      .map {
        case (f,c: JarURLConnection) => try{
          Some((f,c.getInputStream))
        } catch {
          case _: FileNotFoundException => None
        }
      }
      .collect{
        case Some(in) => in
      }
      // ensure that default configuration is loaded first
      .partition(_._1.startsWith(defaultPrefix))
    files._1 ++ files._2
  }


  private def fin(file: File): BufferedInputStream = new BufferedInputStream(new FileInputStream(file))

}
