name := "vector"
libraryDependencies ++= Seq(
  "mil.nga.giat" % "geowave-adapter-vector" % "0.9.2-SNAPSHOT",
  "mil.nga.giat" % "geowave-core-store" % "0.9.2-SNAPSHOT",
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.2-SNAPSHOT"
)

fork in Test := false
parallelExecution in Test := false
