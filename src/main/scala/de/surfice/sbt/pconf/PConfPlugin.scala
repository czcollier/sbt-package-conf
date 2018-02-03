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
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    pconfConfigFile := baseDirectory.value / "project.conf",

    pconfConfigString :=
    loadPackageConfigs((dependencyClasspath in Compile).value, pconfConfigFile.value)
      .foldLeft(""){ (s,in) =>
        s + "# SOURCE: "+in._1+"\n"+
        IO.readLines(new BufferedReader(new InputStreamReader(in._2))).mkString("\n") + "\n\n"
      },

    pconfConfig :=
      ConfigFactory.parseString(pconfConfigString.value).resolve()
  )


  private def loadPackageConfigs(dependencyClasspath: Classpath, projectConfig: File): Seq[(String,InputStream)] =
    loadDepPackageConfigs(dependencyClasspath) ++ loadProjectConfig(projectConfig)

  private def loadProjectConfig(projectConfig: File): Option[(String,InputStream)] =
    if(projectConfig.canRead)
      Some((projectConfig.getAbsolutePath,fin(projectConfig)))
    else None

  private def loadDepPackageConfigs(cp: Classpath): Seq[(String,InputStream)] = {
    val (dirs,jars) = cp.files.partition(_.isDirectory)
    loadJarPackageConfigs(jars) // ++ loadDirPackageConfigs(dirs,log)
  }

  private def loadJarPackageConfigs(jars: Seq[File]): Seq[(String,InputStream)] = {
    val files = jars
      .map( f => (f.getName, new URL("jar:" + f.toURI + "!/package.conf").openConnection()) )
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
      // ensure that default configuration provided by sbt-node is loaded first
      .partition(_._1.startsWith("sbt-node-config_"))
    files._1 ++ files._2
  }


  private def fin(file: File): BufferedInputStream = new BufferedInputStream(new FileInputStream(file))

//  private object DepBuilder extends DependencyBuilders
}
