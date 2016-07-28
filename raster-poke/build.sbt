name := "raster-poke"
libraryDependencies ++= Seq(
  "mil.nga.giat" % "geowave-adapter-raster" % "0.9.3-SNAPSHOT",
  "mil.nga.giat" % "geowave-core-store" % "0.9.3-SNAPSHOT",
  "mil.nga.giat" % "geowave-datastore-accumulo" % "0.9.3-SNAPSHOT"
)

fork in Test := false
parallelExecution in Test := false
