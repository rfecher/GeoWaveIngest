package com.example.raster

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.geowave._
import geotrellis.vector._

import com.vividsolutions.jts.geom.{Envelope, GeometryFactory}
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.adapter.raster.query.IndexOnlySpatialQuery
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.geotime.store.statistics.BoundingBoxDataStatistics
import mil.nga.giat.geowave.core.index.ByteArrayId
import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy
import mil.nga.giat.geowave.core.index.HierarchicalNumericIndexStrategy.SubStrategy
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.core.store.index.{PrimaryIndex, CustomIdIndex}
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import mil.nga.giat.geowave.core.store.query.QueryOptions
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloRequiredOptions
import mil.nga.giat.geowave.mapreduce.input.{GeoWaveInputKey, GeoWaveInputFormat}
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue

import scala.collection.JavaConverters._


object RasterDisgorge {

  val log = Logger.getLogger(RasterDisgorge.getClass)

  /**
    * Construct a BasicAccumuloOperations from the given data.
    */
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

  /**
    * Report various metainformation.  This information is roughly
    * equivalent to that contained in a Geotrellis LayerMetadata.
    */
  def report(bo: BasicAccumuloOperations, adapter: RasterDataAdapter): Unit = {
    val dss = new AccumuloDataStatisticsStore(bo)
    val aid = adapter.getAdapterId
    val bboxStats = BoundingBoxDataStatistics.STATS_ID
    val bbox = dss.getDataStatistics(aid, bboxStats).asInstanceOf[BoundingBoxDataStatistics[Any]]
    val sm = adapter.getSampleModel

    println(s"Extent <- $bbox ${bbox.getMinX} ${bbox.getMinY} ${bbox.getMaxX} ${bbox.getMaxY}")
    println(s"TileLayout <- ${adapter.getTileSize}")
  }

  /**
    * Use a GeoWaveInputFormat-style Query to peek into the GeoWave
    * database.
    */
  def peek(bo: BasicAccumuloOperations, aro: AccumuloRequiredOptions, sc: SparkContext): Unit = {
    /* Construct query */
    val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).setAllTiers(true).createIndex()
    val ds = new AccumuloDataStore(bo)
    val adapter = {
      val as = new AccumuloAdapterStore(bo).getAdapters
      val adapter = as.next.asInstanceOf[RasterDataAdapter]

      as.close
      adapter
    }
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

    /* Construct org.apache.hadoop.conf.Configuration */
    val dspo = new DataStorePluginOptions
    dspo.setFactoryOptions(aro)
    val configOptions = dspo.getFactoryOptionsAsMap
    val job = Job.getInstance(sc.hadoopConfiguration)
    val config = job.getConfiguration
    GeoWaveInputFormat.setDataStoreName(config, "accumulo")
    GeoWaveInputFormat.setStoreConfigOptions(config, configOptions)
    GeoWaveInputFormat.setQuery(config, query)
    GeoWaveInputFormat.setQueryOptions(config, queryOptions)

    report(bo, adapter)

    /* Submit query */
    sc.newAPIHadoopRDD(
      config,
      classOf[GeoWaveInputFormat[GridCoverage2D]],
      classOf[GeoWaveInputKey],
      classOf[GridCoverage2D])
      .collect
      .foreach({ case (_, gc) =>
        val file = new java.io.File(s"/tmp/tif/direct-${System.currentTimeMillis}.tif")
        val writer = new GeoTiffWriter(file)
        writer.write(gc, Array.empty[GeneralParameterValue])
      })
  }

  /**
    * Main Function
    */
  def main(args: Array[String]) : Unit = {
    if (args.length < 5) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace");
      System.exit(-1)
    }

    /* ======================================================================== */

    val sparkConf = new SparkConf().setAppName("GeoWaveInputFormat")
    val sparkContext = new SparkContext(sparkConf)
    val basicOperations = getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))
    val accumuloRequiredOptions = {
      val aro = new AccumuloRequiredOptions
      aro.setZookeeper(args(0))
      aro.setInstance(args(1))
      aro.setUser(args(2))
      aro.setPassword(args(3))
      aro.setGeowaveNamespace(args(4))
      aro
    }

    peek(basicOperations, accumuloRequiredOptions, sparkContext)

    /* ======================================================================== */

    implicit val sc = sparkContext
    val attributeStore = new GeowaveAttributeStore(args(0), args(1), args(2), args(3), args(4))

    println(attributeStore.getBoundingBoxes)
    println(attributeStore.getLeastZooms)
    println(attributeStore.layerIds)

    val layerName = "coverageName"
    val catalog = new GeowaveLayerReader(attributeStore)
    val polygon = Polygon(List(
      // Latitude, Longitude
      Point(32.6953125, 43.9453125),
      Point(32.6953125, 45.0),
      Point(33.75, 45.0),
      Point(33.75, 43.9453125),
      Point(32.6953125, 43.9453125)))

    println("---------------------------------")
    val rdd = catalog
      .query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId(layerName, 10))
      .where(Intersects(polygon.envelope))
      .result
    val mt = rdd.metadata.mapTransform
    println(s"METADATA=${rdd.metadata}")
    rdd.collect.foreach({ case (k, v) =>
      val extent = mt(k)
      val pr = ProjectedRaster(Raster(v, extent), LatLng)
      val gc = pr.toGridCoverage2D
      val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/geotrellis-${System.currentTimeMillis}.tif"))

      println(s"$k $extent $gc")
      writer.write(gc, Array.empty[GeneralParameterValue])
    })


    println("---------------------------------")
    catalog
      .query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId(layerName, 10))
      .where(Intersects(Point(32.701, 44.7499).envelope) or Intersects(Point(33.399, 44.301).envelope))
      .result
      .collect
      .foreach({ case (k, v) => println(s"$k $v") })

    println("---------------------------------")
    catalog
      .query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId(layerName, 14))
      .where(Intersects(Point(32.701, 44.7499).envelope) or Intersects(Point(33.399, 44.301).envelope))
      .result
      .collect
      .foreach({ case (k, v) => println(s"$k $v") })
  }

}
