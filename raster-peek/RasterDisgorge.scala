package com.example.raster

import com.vividsolutions.jts.geom._
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.adapter.raster.query.IndexOnlySpatialQuery
import mil.nga.giat.geowave.core.geotime.ingest._
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
import org.apache.accumulo.core.data.{Key, Value}
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue
import org.apache.hadoop.io.ObjectWritable

import scala.collection.JavaConverters._


object RasterDisgorge {

  val log = Logger.getLogger(RasterDisgorge.getClass)

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

  def peek(bo: BasicAccumuloOperations, aro: AccumuloRequiredOptions, sc: SparkContext): Unit = {
    /* Construct Query */
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

    /* Construct org.apache.hadoop.conf.Configuration */
    val dspo = new DataStorePluginOptions
    dspo.selectPlugin("accumulo")
    dspo.setFactoryOptions(aro)
    val configOptions = dspo.getFactoryOptionsAsMap
    val config = Job.getInstance(sc.hadoopConfiguration).getConfiguration
    GeoWaveInputFormat.setStoreConfigOptions(config, configOptions)
    // GeoWaveInputFormat.setQuery(config, query)
    // GeoWaveInputFormat.setQueryOptions(config, queryOptions)

    sc.newAPIHadoopRDD(
      config,
      classOf[GeoWaveInputFormat[ObjectWritable]],
      classOf[GeoWaveInputKey],
      classOf[ObjectWritable]
    )
      .collect
      // .foreach({ case (_, value) => println(value) })

    // ds.query(queryOptions, query)
    //   .asInstanceOf[CloseableIterator[GridCoverage2D]].asScala
    //   .zip(Iterator.from(0))
    //   .foreach({ case (gc: GridCoverage2D, i: Int) =>
    //     val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/${i}.tif"))
    //     writer.write(gc, Array.empty[GeneralParameterValue])
    //   })
  }

  def main(args: Array[String]) : Unit = {
    val sparkConf = new SparkConf().setAppName("GeoWaveInputFormat")
    val sparkContext = new SparkContext(sparkConf)

    if (args.length < 5) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace");
      System.exit(-1)
    }
    val basicOperations = getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))

    val accumuloRequiredOptions = new AccumuloRequiredOptions
    accumuloRequiredOptions.setZookeeper(args(0))
    accumuloRequiredOptions.setInstance(args(1))
    accumuloRequiredOptions.setUser(args(2))
    accumuloRequiredOptions.setPassword(args(3))
    accumuloRequiredOptions.setGeowaveNamespace(args(4))

    peek(basicOperations, accumuloRequiredOptions, sparkContext)
  }

}
