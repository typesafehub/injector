name := "injector"

organization := "com.typesafe"

version := "0.1"

scalaVersion := "2.11.4"

lazy val injector = project

libraryDependencies := libraryDependencies.value ++ Seq(
  "org.rogach" %% "scallop" % "0.9.5"
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
