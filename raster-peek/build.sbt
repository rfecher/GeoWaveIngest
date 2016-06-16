name := "raster-peek"
libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-geotools" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-geowave" % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-vector" % "1.0.0-SNAPSHOT",
  "org.apache.hadoop" % "hadoop-client" % Version.hadoop % "provided",
  "org.apache.spark" %% "spark-core" % Version.spark % "provided"
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
