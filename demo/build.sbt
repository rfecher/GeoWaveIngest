name := "demo"
libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-accumulo" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-geotools" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-geowave" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-s3" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-spark" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-vector" % "1.0.0-SNAPSHOT",
  "mil.nga.giat" % "geowave-adapter-raster" % "0.9.3-SNAPSHOT" % "provided",
  "mil.nga.giat" % "geowave-core-store" % "0.9.3-SNAPSHOT" % "provided",
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.3-SNAPSHOT" % "provided",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided"
)

resourceDirectory in Compile := baseDirectory.value / "resources"

fork in Test := false
parallelExecution in Test := false
