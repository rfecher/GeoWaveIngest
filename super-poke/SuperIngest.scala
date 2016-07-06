package com.example.raster

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.geowave._
import geotrellis.spark.io.index._

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.Logger


object SuperIngest {

  val log = Logger.getLogger(SuperIngest.getClass)

  def main(args: Array[String]) : Unit = {
    if (args.length < 9) {
      log.error("Invalid arguments, expected: <zookeepers> <accumuloInstance> <accumuloUser> <accumuloPass> <geowaveNamespace> <HadoopPath> <LayerName> <ZoomLevel> <KeyType> <TileType>");
      System.exit(-1)
    }

    val sparkConf = new SparkConf().setAppName("GeoWaveInputFormat")
    val sparkContext = new SparkContext(sparkConf)
    implicit val sc = sparkContext

    val inAttributeStore = HadoopAttributeStore(args(5))
    val layerReader = HadoopLayerReader(inAttributeStore)
    val outAttributeStore = new GeowaveAttributeStore(args(0), args(1), args(2), args(3), args(4))
    val layerWriter = new GeowaveLayerWriter(outAttributeStore)
    val layerId = LayerId(args(6), args(7).toInt)

    (args(8), args(9)) match {
      case ("SpatialKey", "Tile") =>
        val rdd = layerReader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
        layerWriter.write(layerId, rdd, ZCurveKeyIndexMethod)
      case ("SpatialKey", "MultibandTile") =>
        val rdd = layerReader.read[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](layerId)
        layerWriter.write(layerId, rdd, ZCurveKeyIndexMethod)
      case ("SpaceTimeKey", "Tile") =>
        val rdd = layerReader.read[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](layerId)
        val time = rdd.first._1.time
        val rdd1 = rdd.toSpatial(time)
        val rdd2 = ContextRDD(rdd1, rdd1.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]])
        layerWriter.write(layerId, rdd2, ZCurveKeyIndexMethod)
      case ("SpaceTimeKey", "MultibandTile") =>
        val rdd = layerReader.read[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](layerId)
        val time = rdd.first._1.time
        val rdd1 = rdd.toSpatial(time)
        val rdd2 = ContextRDD(rdd1, rdd1.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]])
        layerWriter.write(layerId, rdd2, ZCurveKeyIndexMethod)
      case _ => throw new Exception
    }
  }

}
