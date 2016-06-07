name := "raster-peek"
libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided",
  "mil.nga.giat" % "geowave-adapter-vector" % Version.geowave,
  "mil.nga.giat" % "geowave-core-cli" % Version.geowave,
  "mil.nga.giat" % "geowave-core-ingest" % Version.geowave,
  "mil.nga.giat" % "geowave-core-mapreduce" % Version.geowave,
  "mil.nga.giat" % "geowave-core-store" % Version.geowave,
  "mil.nga.giat" % "geowave-datastore-accumulo" % Version.geowave,
  "org.geoserver" % "gs-wms" % Version.geoserver,
  "org.geotools" % "gt-epsg-wkt" % Version.geotools,
  "org.geotools" % "gt-render" % Version.geotools,
  "org.geotools" % "gt-wps" % Version.geotools
)

resolvers ++= Seq(
  "boundless" at "https://repo.boundlessgeo.com/release",
  "geowave" at "http://geowave-maven.s3-website-us-east-1.amazonaws.com/snapshot",
  "osgeo" at "http://download.osgeo.org/webdav/geotools/"
)

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import com.vividsolutions.jts.geom._
  import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
  import mil.nga.giat.geowave.adapter.raster.query.IndexOnlySpatialQuery
  import mil.nga.giat.geowave.core.geotime.ingest._
  import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy
  import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy.SubStrategy
  import mil.nga.giat.geowave.core.store._
  import mil.nga.giat.geowave.core.store.index.{PrimaryIndex, CustomIdIndex}
  import mil.nga.giat.geowave.core.store.query.QueryOptions
  import mil.nga.giat.geowave.datastore.accumulo._
  import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore
  import mil.nga.giat.geowave.datastore.accumulo.metadata._
  import org.apache.log4j.Logger
  import org.geotools.coverage.grid._
  import org.geotools.coverage.grid.io._
  import org.geotools.gce.geotiff._
  import org.geotools.gce.geotiff.GeoTiffWriter
  import org.opengis.coverage.grid.GridCoverage
  import org.opengis.parameter.GeneralParameterValue

  import scala.collection.JavaConverters._
  """
