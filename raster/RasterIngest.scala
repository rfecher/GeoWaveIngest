package com.example.ingest.raster

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


object RasterIngest {

  val log = Logger.getLogger(RasterIngest.getClass)

  def getAccumuloOperationsInstance(
    zookeepers: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): BasicAccumuloOperations = {
    return new BasicAccumuloOperations(
      zookeepers,
      accumuloInstance,
      accumuloUser,
      accumuloPass,
      geowaveNamespace)
  }

  def createSpatialIndex(): PrimaryIndex =
    new SpatialDimensionalityTypeProvider().createPrimaryIndex

  def getGridCoverage2D(filename: String): GridCoverage2D = {
    val file = new java.io.File(filename)
    val params = Array[GeneralParameterValue]()

    new GeoTiffReader(file).read(params)
  }

  def getGeowaveDataStore(instance: BasicAccumuloOperations): DataStore = {
    return new AccumuloDataStore(
      new AccumuloIndexStore(instance),
      new AccumuloAdapterStore(instance),
      new AccumuloDataStatisticsStore(instance),
      new AccumuloSecondaryIndexDataStore(instance),
      new AccumuloAdapterIndexMappingStore(instance),
      instance);
  }

  def main(args: Array[String]) : Unit = {
    if (args.length < 6) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace, rasterFile");
      System.exit(-1)
    }
    val coverageName = "coverageName" // This must not be empty
    val metadata = new java.util.HashMap[String, String]()
    val image = getGridCoverage2D(args(5))

    val basicOperations = getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))
    val dataStore = getGeowaveDataStore(basicOperations)
    val index = createSpatialIndex
    // https://ngageoint.github.io/geowave/apidocs/mil/nga/giat/geowave/adapter/raster/adapter/RasterDataAdapter.html
    val adapter = new RasterDataAdapter(coverageName, metadata, image, 16, false)

    val indexWriter = dataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[GridCoverage]]

    indexWriter.write(image)
    indexWriter.close
  }

}
