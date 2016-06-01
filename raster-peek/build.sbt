name := "raster-peek"

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
