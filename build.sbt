lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0",
  scalaVersion := "2.11.4",
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
)

lazy val raster = (project in file("raster")).
  settings(commonSettings: _*).
  settings(
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case "reference.conf" => MergeStrategy.concat
      case "application.conf" => MergeStrategy.concat
      case "META-INF/MANIFEST.MF" => MergeStrategy.discard
      case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    scalaVersion := Version.scala
  )
