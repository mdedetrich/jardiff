val buildName = "jardiff"

val scala212Version = "2.12.18"
val scala213Version = "2.13.12"

inThisBuild(Seq[Setting[_]](
  organization := "com.lightbend",
  scalaVersion := "2.13.12",
  startYear := Some(2017),
  organizationName := "Lightbend Inc. <https://www.lightbend.com>",
  licenses := List(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))),
  homepage := Some(url("https://github.com/lightbend-labs/jardiff")),
  scmInfo := Some(ScmInfo(url("https://github.com/lightbend-labs/jardiff"), "scm:git:git@github.com:lightbend-labs/jardiff.git")),
  developers := List(
    Developer("retronym", "Jason Zaugg", "@retronym", url("https://github.com/retronym")),
  ),
  scalacOptions := Seq("-feature", "-deprecation", "-Xlint", "-Werror"),
  githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest"),
  githubWorkflowJavaVersions := Seq(
    JavaSpec.temurin("8"),
    JavaSpec.temurin("11"),
    JavaSpec.temurin("17")
  ),
  githubWorkflowTargetTags ++= Seq ("v*"),
  githubWorkflowPublishTargetBranches :=  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  ),
  githubWorkflowPublish := Seq (
    WorkflowStep.Sbt(
      commands = List("ci-release"),
      name = Some("Publish project"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),
  headerLicense := Some(HeaderLicense.Custom("Copyright (C) Lightbend Inc. <https://www.lightbend.com>")),
))

ThisBuild / publishMavenStyle      := true
ThisBuild / publishTo              := sonatypePublishToBundle.value
ThisBuild / test / publishArtifact := false
ThisBuild / pomIncludeRepository   := (_ => false)

lazy val root = (
  project.in(file("."))
  aggregate(core, cli)
  settings(
    name := buildName,
    publish / skip := true,
    // See https://github.com/sbt/sbt/issues/4262#issuecomment-405607763
    crossScalaVersions := Seq.empty
  )
)

val AsmVersion = "9.6"

lazy val core = project.
  settings(
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm" % AsmVersion,
      "org.ow2.asm" % "asm-util" % AsmVersion,
      "org.scala-lang" % "scalap" % System.getProperty("scalap.version", scalaVersion.value),
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.2.202306221912-r",
      "org.slf4j" % "slf4j-api" % "2.0.9",
      "org.slf4j" % "log4j-over-slf4j" % "2.0.9", // for any java classes looking for this
      "ch.qos.logback" % "logback-classic" % "1.3.11",
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
    ),
    name := buildName + "-core",
    crossScalaVersions := Seq(scala212Version, scala213Version),
    scalaVersion := scala212Version
  )

lazy val cli = project.
  settings(
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % "1.6.0",
    ),
    name := buildName + "-cli",
    assembly / assemblyMergeStrategy := {
      case "rootdoc.txt" => MergeStrategy.discard
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value(x)
    },
    // Having Scala 2.13 here in crossScalaVersions is redundant but due to how
    // sbt-github-actions generates the sbt test command (i.e. sbt '++ 2.13.12' test),
    // sbt's update task cannot handle projects with different crossScalaVersions well
    crossScalaVersions := Seq(scala212Version, scala213Version),
    scalaVersion := scala212Version,
    // cli is not meant to be published
    publish / skip := true,
  ).dependsOn(core)
