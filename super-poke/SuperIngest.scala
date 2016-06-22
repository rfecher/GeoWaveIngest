package com.example.raster

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import geotrellis.spark.io.geowave._
import geotrellis.spark.io.index._

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.Logger


object SuperIngest {

  val log = Logger.getLogger(SuperIngest.getClass)

  def main(args: Array[String]) : Unit = {
    if (args.length < 9) {
      log.error("Invalid arguments, expected: <zookeepers> <accumuloInstance> <accumuloUser> <accumuloPass> <geowaveNamespace> <local catalog path> <zoomlevel> <key> <tile>");
      System.exit(-1)
    }

    val sparkConf = new SparkConf().setAppName("GeoWaveInputFormat")
    val sparkContext = new SparkContext(sparkConf)
    implicit val sc = sparkContext

    val fas = FileAttributeStore(args(5))
    val flr = FileLayerReader(fas)
    val gwas = new GeowaveAttributeStore(args(0), args(1), args(2), args(3), args(4))
    val gwlw = new GeowaveLayerWriter(gwas)
    val layerId = LayerId("landsat", args(6).toInt)

    (args(7), args(8)) match {
      case ("SpatialKey", "Tile") =>
        val rdd = flr.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
        val rdd1 = ContextRDD(sc.parallelize(rdd.take(1)), rdd.metadata)
        gwlw.write(layerId, rdd1, ZCurveKeyIndexMethod)
      case ("SpatialKey", "MultibandTile") =>
        val rdd = flr.read[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](layerId)
        val rdd1 = ContextRDD(sc.parallelize(rdd.take(1)), rdd.metadata)
        gwlw.write(layerId, rdd1, ZCurveKeyIndexMethod)
      case ("SpaceTimeKey", "Tile") =>
        val rdd = flr.read[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](layerId)
        val time = rdd.first._1.time
        val rdd2 = rdd.toSpatial(time)
        val rdd1 = ContextRDD(sc.parallelize(rdd2.take(1)), rdd2.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]])
        gwlw.write(layerId, rdd1, ZCurveKeyIndexMethod)
      case ("SpaceTimeKey", "MultibandTile") =>
        val rdd = flr.read[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](layerId)
        val time = rdd.first._1.time
        val rdd2 = rdd.toSpatial(time)
        val rdd1 = ContextRDD(sc.parallelize(rdd2.take(args(9).toInt)), rdd2.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]])
        gwlw.write(layerId, rdd1, ZCurveKeyIndexMethod)
      case _ => throw new Exception
    }
  }

}
