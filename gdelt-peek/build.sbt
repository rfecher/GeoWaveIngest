name := "gdelt-peek"
libraryDependencies ++= Seq(
  "mil.nga.giat" % "geowave-adapter-vector" % "0.9.2-SNAPSHOT",
  "mil.nga.giat" % "geowave-core-store" % "0.9.2-SNAPSHOT",
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.2-SNAPSHOT",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided"
)

fork in Test := false
parallelExecution in Test := false
