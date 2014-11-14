# Injector

## Description

*Injector* is a simple tool that inserts additional files into existing jar archives.

## Download

Please download the executable jar file from this location: [injector-0.3.jar](http://repo.typesafe.com/typesafe/releases/com/typesafe/injector/injector/0.3/injector-0.3.jar)

You can then launch the program using:

    java -jar injector-0.3.jar <args>

## Use as a library

If you would like to call Injector from your program, please include this library:

    "com.typesafe.injector" %% "injector-lib" % "0.3"

This library has been published for Scala 2.11 and 2.10. You will also need to add the repository:

    resolvers += Resolver.typesafeRepo("releases")

Then, call:

    com.typesafe.injector.Injector(arg, ...)

## Usage

    Typesafe injector 0.3
    Usage: injector [OPTIONS]
    Injector is a simple tool that will inject additional files into a set of
    artifact jar files, and recalculate their checksum files appropriately.
    Options:
    
          --debug                   Print more debugging information
      -d, --directories  <arg>...   One or more paths to the directories containing
                                    the jars that will be processed. Every directory
                                    will be scanned recursively. In place of a
                                    directory, you can specify individual jar files.
      -f, --files  <arg>...         Path to the file(s) that should be inserted into
                                    jars. They will by default be added at the root of
                                    the jar; if you would like them at a different
                                    location, please append a "@" and the desired
                                    path, as in "manifest.txt@META-INF/MANIFEST.MF".
                                    If the destination path specifies just a
                                    directory, please append '/' to the path string.
      -j, --jars  <arg>...          Patterns that specify which jars should be
                                    considered, in glob format. For instance, c*.jar
                                    will match all jars whose basename begins with c.
                                    If multiple patterns are specified, all the jars
                                    that match at least one pattern will be
                                    considered. All patterns must end with ".jar". If
                                    omitted, all jars will be processed.
                                    (default = List(*.jar))
      -n, --no-checksums            Do not regenerate the checksum files of the
                                    modified jar files. By default, new mds and sha1
                                    files will be generated, replacing the old ones.
      -q, --quiet                   Do not print messages on the console.
      -t, --to  <arg>               By default, the jar files will be overwritten in
                                    place. If you would like to preserve the
                                    originals, you can specify using this option a
                                    directory where the new files will be stored. The
                                    destination directory will be created, if it does
                                    not exist yet.
          --help                    Show help message
          --version                 Show version of this program
    
    For most options, you can specify multiple files or paths separated by blanks,
    or multiple times the same option to add elements.
    For additional information, please contact http://www.typesafe.com.

