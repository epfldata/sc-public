import java.io.File
import sbt._
import Keys._
import Process._
import ch.epfl.data.sc.purgatory.plugin.PurgatoryPlugin._

object Build extends Build {
  
  val SCVersion = "0.1.2-SNAPSHOT"
  
  def defaultSettings = Defaults.coreDefaultSettings ++ Seq(
      scalaVersion := "2.11.7",
      resolvers += Resolver.sonatypeRepo("snapshots"),
      libraryDependencies ++= Seq(
        "junit" % "junit-dep" % "4.10" % "test",
        "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test",
        "ch.epfl.data" % "sc-pardis-compiler_2.11" % SCVersion,
        "ch.epfl.lamp" % "scala-yinyang_2.11" % "0.2.0"
      )
      //, scalacOptions in Test ++= Seq("-optimize")
  )
  
  lazy val relation = Project(id = "relation", base = file("."), settings = defaultSettings ++ Seq(
      
      libraryDependencies += "ch.epfl.data" % "sc-pardis-quasi_2.11" % SCVersion
      
    ) ++
    // Purgatory settings:
    generatorSettings  ++ Seq(
        generatePlugins += "ch.epfl.data.sc.purgatory.generator.QuasiGenerator",
        pluginLibraries += "ch.epfl.data" % "sc-purgatory-quasi_2.11" % SCVersion,
  
        outputFolder := "relation-deep/src/main/scala/relation/deep",
        inputPackage := "relation.shallow",
        outputPackage := "relation.deep"
    )
  )
  
  lazy val relationdeep = Project(id = "relation-deep", base = file("relation-deep"), settings = defaultSettings) dependsOn relation
    
  lazy val relationgen = Project(id = "relation-gen", base = file("generator-out"), settings = defaultSettings) dependsOn relation
  
}

