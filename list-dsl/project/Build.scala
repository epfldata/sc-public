import java.io.File
import sbt._
import Keys._
import Process._
import ch.epfl.data.sc.purgatory.plugin.PurgatoryPlugin._

object Build extends Build {
  
  val SCVersion = "0.1.1-SNAPSHOT"
  
  def defaultSettings = Defaults.coreDefaultSettings ++ Seq(
      scalaVersion := "2.11.7",
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        "ch.epfl.data" % "sc-pardis-compiler_2.11" % SCVersion,
        "ch.epfl.lamp" % "scala-yinyang_2.11" % "0.2.0-SNAPSHOT"
      )
      //, scalacOptions in Test ++= Seq("-optimize")
  )
  
  lazy val list = Project(id = "list", base = file("."), settings = defaultSettings ++ Seq(
      
      libraryDependencies += "ch.epfl.data" % "sc-pardis-quasi_2.11" % SCVersion
      
    ) ++
    // Purgatory settings:
    generatorSettings  ++ Seq(
        generatePlugins += "ch.epfl.data.sc.purgatory.generator.QuasiGenerator",
        pluginLibraries += "ch.epfl.data" % "sc-purgatory-quasi_2.11" % SCVersion,
  
        outputFolder := "list-deep/src/main/scala/list/deep",
        inputPackage := "list.shallow",
        outputPackage := "list.deep"
    )
  )
  
  lazy val listdeep = Project(id = "list-deep", base = file("list-deep"), settings = defaultSettings) dependsOn list
    
  lazy val listgen = Project(id = "list-gen", base = file("generator-out"), settings = defaultSettings) dependsOn list
  
}

