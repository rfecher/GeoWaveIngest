package com.daystrom_data_concepts.gdelt

import geotrellis.geotools._
import geotrellis.raster.TileLayout
import geotrellis.spark.join.VectorJoin
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector._

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions
import mil.nga.giat.geowave.core.store.query.QueryOptions
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloRequiredOptions
import mil.nga.giat.geowave.format.gdelt.GDELTUtils
import mil.nga.giat.geowave.mapreduce.input.{GeoWaveInputKey, GeoWaveInputFormat}
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.geotools.feature.simple._
import org.opengis.feature.simple._


/**
  * Requires a locally-published version of [1] which is based on [2]
  * (the former is just the latter rebased on top of 536ccd9).
  *
  * 1. https://github.com/jamesmcclain/geotrellis/tree/feature/vector-join
  * 2. https://github.com/geotrellis/geotrellis/pull/1555
  */
object GdeltDisgorge {

  val log = Logger.getLogger(GdeltDisgorge.getClass)
  val ε = 0.5

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

  def main(args: Array[String]) : Unit = {
    if (args.length < 5) {
      log.error("Invalid arguments, expected: <Zookeepers> <AccumuloInstance> <AccumuloUser> <AccumuloPass> <GeoWaveNamespace>");
      System.exit(-1)
    }

    val sparkConf = new SparkConf().setAppName("GeoWaveInputFormat")
    val sparkContext = new SparkContext(sparkConf)
    implicit val sc = sparkContext

    val job = Job.getInstance(sparkContext.hadoopConfiguration)
    val config = job.getConfiguration
    val configOptions = {
      val aro = new AccumuloRequiredOptions
      aro.setZookeeper(args(0))
      aro.setInstance(args(1))
      aro.setUser(args(2))
      aro.setPassword(args(3))
      aro.setGeowaveNamespace(args(4))

      val dspo = new DataStorePluginOptions
      dspo.setFactoryOptions(aro)

      dspo.getFactoryOptionsAsMap
    }

    val basicOperations = getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))
    val adapter = new FeatureDataAdapter(GDELTUtils.createGDELTEventDataType(true))
    val customIndex = (new SpatialDimensionalityTypeProvider).createPrimaryIndex

    GeoWaveInputFormat.setDataStoreName(config, "accumulo")
    GeoWaveInputFormat.setStoreConfigOptions(config, configOptions)
    // GeoWaveInputFormat.setQuery(config, new SpatialQuery(utah))
    GeoWaveInputFormat.setQueryOptions(config, new QueryOptions(adapter, customIndex))

    val _rdd = sparkContext.newAPIHadoopRDD(config,
      classOf[GeoWaveInputFormat[SimpleFeature]],
      classOf[GeoWaveInputKey],
      classOf[SimpleFeature])
      .map({ case (_, simpleFeature) => simpleFeature.toFeature[Point] })
    val rdd = sparkContext.parallelize(_rdd.take(1<<5))

    val bufferedRdd = rdd.map({ feature =>
      val point: Point = feature.geom
      val x = point.x
      val y = point.y
      val poly = Polygon(
        Point(x-ε,y-ε),
        Point(x+ε,y-ε),
        Point(x+ε,y+ε),
        Point(x-ε,y+ε),
        Point(x-ε,y-ε)
      )
      val data = feature.data

      Feature(poly, data)
    })

    val extent = Extent(-180, -90, 180, 90)
    val tileLayout = TileLayout(1000, 1000, 256, 256)
    val layoutDefinition = LayoutDefinition(extent, tileLayout)

    val joinedRdd1 = VectorJoin(rdd, bufferedRdd, layoutDefinition, { (l, r) => l.intersects(r) })
    val joinedRdd2 = VectorJoin(rdd, bufferedRdd, { (l, r) => l.intersects(r) })

    joinedRdd2
      .collect
      .foreach({ case(l,r) =>
        println(s"LEFT=$l")
        println(s"RIGHT=$r")
        println
      })

    println(s"${joinedRdd1.count} and ${joinedRdd2.count}")
  }

}
