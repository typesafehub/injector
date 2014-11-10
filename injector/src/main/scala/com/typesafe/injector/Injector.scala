package com.typesafe.injector

import org.rogach.scallop._
import org.rogach.scallop.exceptions.ScallopException

object InjectorMain { def main(args: Array[String]) = (new Injector).start(args) }
case class Exit(val code: Int) extends xsbti.Exit
class Injector extends xsbti.AppMain {
  def run(configuration: xsbti.AppConfiguration) = {
    try {
      start(configuration.arguments, configuration.provider.id.name, configuration.provider.id.version)
      Exit(0)
    } catch {
      case e: Exception =>
        e.printStackTrace
        Exit(1)
    }
  }
  def start(args: Array[String], name: String = "injector", version: String = "") = {
    class conf(n: String, v: String) extends ScallopConf(args.toList) {
      printedName = n
      version("Typesafe " + n + " " + v)
      banner(("""Usage: """ + n + """ [OPTIONS]
                |""" + (n.capitalize) + """ is a simple tool that will inject additional files into a set of
                |artifact jar files, and recalculate their checksum files appropriately.
                |Options:
                |""").stripMargin)
      footer("""
               |For most options, you can specify multiple files or paths separated by blanks,
               |or multiple times the same option to add elements.
               |For additional information, please contact http://www.typesafe.com.""".stripMargin)
      val debug = opt[Boolean](noshort = true, descr = "Print more debugging information")
      val files = opt[List[String]](descr = "Path to the file(s) that should be inserted into jars.",
        required = true)
      val jars = opt[List[String]](descr = "Patterns that specify which jars should be considered." +
        "For instance, c*.jar will match all jars whose basename begins with c. If multiple patterns are specified, " +
        "all the jars that match at least one pattern will be considered. All patterns must end with \".jar\". " +
        "If omitted, all jars will be processed.",
        validate = (_.forall(_.endsWith(".jar"))),
        default = Some(List[String]("*.jar")))
      val directories = opt[List[String]](required = true, descr = "One or more paths to the directories containing the jars " +
        "that will be processed. Every directory will be scanned recursively.")
      val noChecksums = opt[Boolean](descr = "Do not regenerate the checksum files of the modified jar files. By default, " +
        "New mds and sha1 files will be generated, to replace the existing ones.")
    }
    val conf = new conf(name, version)
    val debug = conf.debug()
    val file = conf.files()
    val jars = conf.jars()
    val dirs = conf.directories()
    val checksums = !conf.noChecksums()
  }
}
