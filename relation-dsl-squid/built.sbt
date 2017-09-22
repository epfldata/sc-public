name := "relation-dsl"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"))

libraryDependencies += "ch.epfl.data" %% "squid" % "0.1.1-SNAPSHOT"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
