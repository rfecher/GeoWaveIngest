name := "ingest-raster"

fork in Test := false
parallelExecution in Test := false

initialCommands in console :=
  """
  import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
  import mil.nga.giat.geowave.core.geotime.ingest._
  import mil.nga.giat.geowave.core.store._
  import mil.nga.giat.geowave.core.store.index.PrimaryIndex
  import mil.nga.giat.geowave.datastore.accumulo._
  import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore
  import mil.nga.giat.geowave.datastore.accumulo.metadata._

  import org.apache.log4j.Logger
  import org.geotools.coverage.grid._
  import org.geotools.coverage.grid.io._
  import org.geotools.gce.geotiff._
  import org.opengis.coverage.grid.GridCoverage
  import org.opengis.parameter.GeneralParameterValue

  val policy = AbstractGridFormat.OVERVIEW_POLICY.createValue; policy.setValue(OverviewPolicy.IGNORE)
  val gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue; gridSize.setValue("256,256")
  val useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue; useJaiRead.setValue(true)
  """
