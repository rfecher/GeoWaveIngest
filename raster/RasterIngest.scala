package com.example.ingest.raster

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

  def getGridCoverage2D(filename: String): GridCoverage2D = {
    val file = new java.io.File(filename)
    val params = Array[GeneralParameterValue]()

    new GeoTiffReader(file).read(params)
  }

  def poke(bo: BasicAccumuloOperations, img: GridCoverage2D): Unit = {
    val coverageName = "coverageName" // This must not be empty
    val metadata = new java.util.HashMap[String, String]()
    val dataStore = new AccumuloDataStore(bo)
    val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).setAllTiers(true).createIndex()
    val adapter = new RasterDataAdapter(coverageName, metadata, img, 256, true) // img only used for metadata, not data
    val indexWriter = dataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[GridCoverage]]

    indexWriter.write(img)
    indexWriter.close
  }

  def peek(bo: BasicAccumuloOperations): Unit = {
    val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).setAllTiers(true).createIndex()
    val as = new AccumuloAdapterStore(bo)
    val ds = new AccumuloDataStore(bo)
    val adapter = as.getAdapters.next.asInstanceOf[RasterDataAdapter]
    val envelope = new Envelope(44.1, 44.7, 33.0, 33.6)
    val geom = (new GeometryFactory).toGeometry(envelope)
    val strats = index.getIndexStrategy.asInstanceOf[HierarchicalNumericIndexStrategy].getSubStrategies
    val target = strats.filter({ substrat =>
      val ranges = substrat.getIndexStrategy.getHighestPrecisionIdRangePerDimension
      ((ranges(0) <= envelope.getWidth) && (ranges(1) <= envelope.getHeight))
    }).head
    val customIndex = new CustomIdIndex(target.getIndexStrategy, index.getIndexModel, index.getId)
    val queryOptions = new QueryOptions(adapter, customIndex)
    val query = new IndexOnlySpatialQuery(geom)

    ds.query(queryOptions, query)
      .asInstanceOf[CloseableIterator[GridCoverage2D]].asScala
      .zip(Iterator.from(0))
      .foreach({ case (gc: GridCoverage2D, i: Int) =>
        val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/${i}.tif"))
        writer.write(gc, Array.empty[GeneralParameterValue])
      })
  }

  def main(args: Array[String]) : Unit = {
    if (args.length < 6) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace, rasterFile");
      System.exit(-1)
    }
    val basicOperations = getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))
    val gridCoverage = getGridCoverage2D(args(5))

    println("POKE"); poke(basicOperations, gridCoverage)
    println("PEEK"); peek(basicOperations)
  }

}
