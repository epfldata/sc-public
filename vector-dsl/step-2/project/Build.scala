import java.io.File
import sbt._
import Keys._
import Process._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import ch.epfl.data.sc.purgatory.plugin.PurgatoryPlugin._

object VectorBuild extends Build {

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  lazy val defaults = Project.defaultSettings ++ formatSettings ++ Seq(
    resolvers += Resolver.sonatypeRepo("snapshots"),
    // add the library, reflect and the compiler as libraries
    libraryDependencies ++= Seq(
      "junit" % "junit-dep" % "4.10" % "test",
      "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
    ),
    scalaVersion := "2.11.2"
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }

  def purgatorySettings = Seq(
    outputFolder := "vector-compiler/src/main/scala/ch/epfl/data/vector/deep",
    inputPackage := "ch.epfl.data.vector.shallow",
    outputPackage := "ch.epfl.data.vector.deep") ++ generatorSettings

  lazy val vector             = Project(id = "root", base = file("."), settings = defaults) aggregate (vector_interpreter, vector_compiler)
  lazy val vector_interpreter = Project(id = "vector-interpreter", base = file("vector-interpreter"), settings = defaults ++ purgatorySettings ++ Seq(
    name := "vector-interpreter"))
  lazy val vector_compiler    = Project(id = "vector-compiler", base = file("vector-compiler"), settings = defaults ++ Seq(name := "vector-compiler",
      libraryDependencies += "ch.epfl.data" % "sc-pardis-compiler_2.11" % "0.1.1-SNAPSHOT",
      libraryDependencies += "ch.epfl.lamp" % "scala-yinyang_2.11" % "0.2.0-SNAPSHOT",
      scalacOptions in Test ++= Seq("-optimize"))) dependsOn(vector_interpreter)
}
