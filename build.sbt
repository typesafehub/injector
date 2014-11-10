organization in Global := "com.typesafe"

version in Global := "0.1"

scalaVersion in Global := "2.11.4"

lazy val injector = project

libraryDependencies in Global := (libraryDependencies in Global).value ++ Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "org.scala-sbt" % "launcher-interface" % "0.13.6" % "provided"
)

lazy val root = Project("root", file("."))
  .aggregate(injector)
  .settings(
      run := {
        (run in injector in Compile).evaluated
      },
      publish := (),
      publishLocal := ()
    )
