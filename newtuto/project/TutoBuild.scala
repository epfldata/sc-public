import java.io.File
import sbt._
import Keys._
import Process._
import ch.epfl.data.sc.purgatory.plugin.PurgatoryPlugin._

object TutoBuild extends Build {
  
  val SCVersion = "0.1.1-SNAPSHOT"
  
  def defaultSettings = Defaults.coreDefaultSettings ++ Seq(
      scalaVersion := "2.11.7",
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        "ch.epfl.data" % "sc-pardis-compiler_2.11" % SCVersion,
        "ch.epfl.lamp" % "scala-yinyang_2.11" % "0.2.0-SNAPSHOT"
      )/*,
      scalacOptions in Test ++= Seq("-optimize")*/
  )
  
  lazy val mylib = Project(id = "mylib", base = file("."), settings = defaultSettings ++ Seq(
      
      libraryDependencies += "ch.epfl.data" % "sc-pardis-quasi_2.11" % SCVersion
      
    ) ++
    // Purgatory settings:
    generatorSettings  ++ Seq(
        generatePlugins += "ch.epfl.data.sc.purgatory.generator.QuasiGenerator",
        pluginLibraries += "ch.epfl.data" % "sc-purgatory-quasi_2.11" % SCVersion,
  
        outputFolder := "mylib-deep/src/main/scala/mylib/deep",
        inputPackage := "mylib.shallow",
        outputPackage := "mylib.deep"
    )
  )
  
  lazy val mylibdeep = Project(id = "mylib-deep", base = file("mylib-deep"), settings = defaultSettings) dependsOn mylib
    
  lazy val mylibgen = Project(id = "mylib-gen", base = file("generator-out"), settings = defaultSettings) dependsOn mylib
  
}

