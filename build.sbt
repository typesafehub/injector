organization in Global := "com.typesafe"

version in Global := "0.1"

scalaVersion in Global := "2.11.4"

lazy val injector = project.settings(sourceGenerators in Compile <+= (sourceManaged in Compile, version, name, streams) map { (dir, v, n, s) =>
        val file = dir / "Defaults.scala"
        if(!dir.isDirectory) dir.mkdirs()
        s.log.info("Generating "+file.getName+" into "+file.getCanonicalPath)
        IO.write(file, """
          package com.typesafe.injector
          object Defaults {
            val version = "%s"
            val name = "%s"
            val hash = "%s"
          }
        """ format (v, n, Process("git log --pretty=format:%H -n 1").lines.head))
        Seq(file)
      })

libraryDependencies in Global := (libraryDependencies in Global).value ++ {
  val sbtVer = sbtVersion.value
  Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "org.scala-sbt" % "launcher-interface" % sbtVer % "provided",
  "org.apache.ivy" % "ivy" % "2.3.0-rc1",
  "org.scala-sbt" % "io" % sbtVer
)}

lazy val root = Project("root", file("."))
  .aggregate(injector)
  .settings(
      run := {
        (run in injector in Compile).evaluated
      },
      publish := (),
      publishLocal := ()
    )
