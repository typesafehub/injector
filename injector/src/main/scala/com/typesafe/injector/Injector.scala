package com.typesafe.injector

import org.rogach.scallop._
import org.rogach.scallop.exceptions.ScallopException
import org.apache.ivy.util.ChecksumHelper
import sbt.PathFinder._
import sbt.Path._
import sbt.IO._
import sbt.GlobFilter
import sbt.DirectoryFilter
import java.io.File
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.JarFile
import java.util.jar.JarEntry

object InjectorMain { def main(args: Array[String]) = (new Injector).start(args, Defaults.name, Defaults.version) }
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
      private def fileExists(p: String, dirs: Boolean) = {
        val f = new File(p)
        val valid = f.exists
        if (!valid) println("This " + (if (dirs) "directory" else "file") + " cannot be found: " + p)
        valid
      }
      val files = opt[List[String]](descr = "Path to the file(s) that should be inserted into jars. They will " +
        "by default be added at the root of the jar; if you would like them at a different location, please " +
        "append a \"@\" and the desired path, as in \"manifest.txt@META-INF/MANIFEST.MF\". If the destination path " +
        "specifies just a directory, please append a '/' to the path string.",
        required = true, validate = (_.map(_.split('@').head).forall(fileExists(_, dirs = false))))
      val directories = opt[List[String]](required = true, descr = "One or more paths to the directories containing the jars " +
        "that will be processed. Every directory will be scanned recursively.", validate = (_.forall(fileExists(_, dirs = true))))
      val jars = opt[List[String]](descr = "Patterns that specify which jars should be considered, in glob format." +
        "For instance, c*.jar will match all jars whose basename begins with c. If multiple patterns are specified, " +
        "all the jars that match at least one pattern will be considered. All patterns must end with \".jar\". " +
        "If omitted, all jars will be processed.",
        validate = (_.forall { j =>
          val valid = j.endsWith(".jar") && !j.contains("/")
          if (!valid) {
            println("This pattern contains a forward slash, or does not end with \".jar\": " + j)
          }
          valid
        }),
        default = Some(List[String]("*.jar")))
      val noChecksums = opt[Boolean](descr = "Do not regenerate the checksum files of the modified jar files. By default, " +
        "New mds and sha1 files will be generated, replacing the old ones.")
      //      val to = opt[String](descr = "By default, the existing files will be overwritten in place. If you would like to "+
      //          "preserve the originals, you can specify using this option a directory where the new files will be stored. The "+
      //          "destination directory will be created, if it does not exist yet.")
      // TODO: add re-signing support, probably
    }
    val conf = new conf(name, version)
    val debug = conf.debug()
    val filesAndTargets = conf.files().map { fileAndTarget =>
      val filePath = fileAndTarget.split('@').head
      val targetBase = fileAndTarget.drop(filePath.length)
      val file = new File(filePath)
      val target = if (targetBase == "" || targetBase.endsWith("/"))
        targetBase + file.getName
      else
        targetBase
      (file, target)
    }
    val patterns = conf.jars()
    val dirs = conf.directories()
    val checksums = !conf.noChecksums()

    // Are we specifying a new destination directory?
    // not implemented yet
    //    val to = conf.to.get
    //... find a common base for all dirs, and make a recursive copy,
    // then use "to" rather than "dirs" as a base to search jars

    // Let's find out which jars need to be processed

    val filter = patterns.map(GlobFilter(_)).reduce(_ | _).--(DirectoryFilter)
    val jars = dirs.map(new File(_)).map(_.**(filter)).reduce(_ +++ _).get // APL?

    val targets = filesAndTargets.map(_._2)
    jars foreach { jar =>
      withTemporaryFile("injector", "tempJar") { temp =>
        val in = new JarInputStream(new BufferedInputStream(new FileInputStream(jar)))
        val out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(jar)))

        // first we copy the old jar, stripping away the entries that will be overwritten

        val bufferSize = 131072
        val buffer = new Array[Byte](bufferSize)
        def writeEntry(where: JarEntry, source: InputStream) = {
          out.putNextEntry(where)
          Stream.continually(source.read(buffer, 0, bufferSize)).takeWhile(_ != -1).
            foreach { size => out.write(buffer, 0, size) }
        }
        Stream.continually(in.getNextJarEntry).takeWhile(_ != null).foreach { entry =>
          if (!targets.contains(entry.getName())) {
            writeEntry(entry, in)
          }
        }

        // then, we insert the new entries at the appropriate target locations

        filesAndTargets.foreach {
          case (file, target) =>
            writeEntry(new JarEntry(target), new BufferedInputStream(new FileInputStream(file)))
        }
        in.close()
        out.flush()
        out.close()

        // time to move the temporary file back to the original location
        move(temp, jar)

        // ok. Do we need to regenerate the checksum files?
        if (checksums) {
          Seq("md5", "sha1") foreach { algorithm =>
            val checksumFile = new File(jar.getCanonicalPath + "." + algorithm)
            if (checksumFile.exists) {
              val writer = new PrintWriter(checksumFile)
              writer.write(ChecksumHelper.computeAsString(jar, algorithm))
              writer.flush()
              writer.close()
            }
          }
        }
      }
    }
  }
}
