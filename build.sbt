organization in Global := "com.typesafe.injector"

version in Global := "0.1"

scalaVersion in Global := "2.11.4"

// injector is actually the launcher, with a custom configuration
// while injector-lib is the real code

lazy val injectorLib = project.settings(sourceGenerators in Compile <+= (sourceManaged in Compile, version in injector, name in injector, streams) map { (dir, v, n, s) =>
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
      },
      name := "injector-lib")

libraryDependencies in Global := (libraryDependencies in Global).value ++ {
  val sbtVer = sbtVersion.value
  Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "org.scala-sbt" % "launcher-interface" % sbtVer % "provided",
  "org.apache.ivy" % "ivy" % "2.3.0-rc1",
  "org.scala-sbt" % "io" % sbtVer
)}

lazy val root = Project("root", file("."))
  .aggregate(injector,injectorLib)
  .settings(
      run := {
        (run in injector in Compile).evaluated
      },
      publish := (),
      publishLocal := ()
    )

lazy val commandLine = taskKey[Unit]("")

lazy val injector = project.settings(
    resolvers += Resolver.typesafeIvyRepo("releases"),
    libraryDependencies += "org.scala-sbt" % "sbt-launch" % sbtVersion.value,
    packageBin in Compile := {
      val dir = resourceManaged.value
      val cp = (externalDependencyClasspath in Compile).value
      val launcherJar = cp.find { af =>
        af.get(moduleID.key) match {
          case None => false
          case Some(m) => m.name == "sbt-launch" && m.organization == "org.scala-sbt"
        }
      }.getOrElse(sys.error("Unexpected: sbt launcher jar not found.")).data
println("Using launcher: "+launcherJar.getCanonicalPath)
      // I'd love to invoke (Keys.run in injector), but sbt keeps taunting me
      // with "Illegal dynamic reference". So I call "jar" manually.
      val from=dir/"from"
      val to=dir/"polpetta.jar"
      from.mkdirs()
      IO.delete(from.*("*").get)
      Process(Seq("jar","xf",launcherJar.getCanonicalPath),from).lines.foreach(l=>l)
      IO.write(from / "sbt" / "sbt.boot.properties","""
[scala]
  version: 2.11.4
[app]
  org: com.typesafe.injector
  name: injector-lib
  version: 0.1
  class: com.typesafe.injector.Injector
  components: xsbti
  cross-versioned: binary
[repositories]
  local
  typesafe-ivy-releases: https://repo.typesafe.com/typesafe/ivy-releases/, [organization]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext], bootOnly
  maven-central
[boot]
  directory: ${sbt.boot.directory-${sbt.global.base-${user.home}/.sbt}/boot/}
[ivy]
  ivy-home: ${sbt.ivy.home-${user.home}/.ivy2/}
  checksums: ${sbt.checksums-sha1,md5}
  override-build-repos: ${sbt.override.build.repos-false}
  repository-config: ${sbt.repository.config-${sbt.global.base-${user.home}/.sbt}/repositories}
""")
      IO.copyFile(from / "META-INF" / "MANIFEST.MF",dir / "manifest.txt")
      Process(Seq("jar","cfm",to.getCanonicalPath,"../manifest.txt")++Process("ls",from).lines,from).lines.foreach(l=>l)
      to
}
)
