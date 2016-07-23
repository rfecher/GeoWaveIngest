name := "gdelt-peek"
libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-geotools" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-raster" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-spark" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-vector" % "1.0.0-SNAPSHOT",
  "mil.nga.giat" % "geowave-adapter-vector" % "0.9.3-SNAPSHOT",
  "mil.nga.giat" % "geowave-core-store" % "0.9.3-SNAPSHOT",
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.3-SNAPSHOT",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided"
)

compileOrder in Compile := CompileOrder.JavaThenScala

fork in Test := false
parallelExecution in Test := false
