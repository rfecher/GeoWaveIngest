import sbtassembly.PathList

val generalDeps = Seq(
  "org.apache.accumulo" % "accumulo-core" % "1.7.1"
    exclude("org.jboss.netty", "netty")
    exclude("org.apache.hadoop", "hadoop-client"),
  "org.apache.hadoop" % "hadoop-client" % "2.6.2"
)

val extraResolvers = Seq(
  "osgeo" at "http://download.osgeo.org/webdav/geotools/",
  "geowave" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot"
)

lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0",
  scalaVersion := "2.10.6",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case "reference.conf" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) =>
      xs match {
        case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
          // Concatenate everything in the services directory to keep
          // GeoTools happy.
        case ("services" :: _ :: Nil) =>
          MergeStrategy.concat
          // Concatenate these to keep JAI happy.
        case ("javax.media.jai.registryFile.jai" :: Nil) | ("registryFile.jai" :: Nil) | ("registryFile.jaiext" :: Nil) =>
          MergeStrategy.concat
        case (name :: Nil) => {
          // Must exclude META-INF/*.([RD]SA|SF) to avoid "Invalid
          // signature file digest for Manifest main attributes"
          // exception.
          if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".SF"))
            MergeStrategy.discard
          else
            MergeStrategy.first
        }
        case _ => MergeStrategy.first
      }
    case _ => MergeStrategy.first
  },
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= generalDeps)

lazy val rasterPoke = (project in file("raster-poke"))
  .dependsOn(root)
  .settings(commonSettings: _*)
  .settings(resolvers ++= extraResolvers)

lazy val superPoke = (project in file("super-poke"))
  .dependsOn(root)
  .settings(commonSettings: _*)
  .settings(resolvers ++= extraResolvers)

lazy val rasterPeek = (project in file("raster-peek"))
  .dependsOn(root)
  .settings(commonSettings: _*)
  .settings(resolvers ++= extraResolvers)

lazy val vector = (project in file("vector"))
  .dependsOn(root)
  .settings(commonSettings: _*)
  .settings(resolvers ++= extraResolvers)
