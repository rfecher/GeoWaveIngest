val generalDeps = Seq(
  "org.apache.accumulo" % "accumulo-core" % "1.7.1"
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.apache.hadoop" % "hadoop-client" % "2.6.2"
)

lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0",
  scalaVersion := "2.11.4",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
    case _ => MergeStrategy.first
  },
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(libraryDependencies ++= generalDeps)

lazy val raster = (project in file("raster")).
  dependsOn(root).
  settings(commonSettings: _*)

lazy val vector = (project in file("vector")).
  dependsOn(root).
  settings(commonSettings: _*)
