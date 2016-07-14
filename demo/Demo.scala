package com.daystrom_data_concepts.raster

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.geowave._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._

import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue


object Demo {

  val logger = Logger.getLogger(Demo.getClass)
  val CACHE_DIR = "/tmp/catalog-cache"

  def projection(quantiles: Array[Double])(x: Double): Int = {
    val i = java.util.Arrays.binarySearch(quantiles, x)
    if (i < 0) ((-1 * i) - 1); else i
  }

  def main(args: Array[String]) : Unit = {

    /* Command line arguments */
    if (args.length < 7) {
      logger.error("Invalid arguments, expected: <zookeepers> <accumuloInstance> <accumuloUser> <accumuloPass> <geowaveNamespace> <layerName> <zoomLevel>");
      System.exit(-1)
    }

    /* Spark context */
    val sparkConf = new SparkConf().setAppName("GeoTrellis+GeoWave Demo")
    val sparkContext = new SparkContext(sparkConf)
    implicit val sc = sparkContext

    /* Assign arguments to variables with meaningful names */
    val zookeepers = args(0)
    val accumuloInstance = args(1)
    val accumuloUser = args(2)
    val accumuloPass = args(3)
    val geowaveNamespace = args(4)

    val gwAttributeStore = new GeowaveAttributeStore(zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace)
    val layerWriter = new GeowaveLayerWriter(gwAttributeStore)
    val gwLayerId = LayerId(args(5), args(6).toInt)

    val rdd0 = {
      val s3AttributeStore = S3AttributeStore("osm-elevation", "catalog")
      val s3LayerReader = S3LayerReader(s3AttributeStore)
      val inLayerId = LayerId(args(5), args(6).toInt)

      if (HadoopAttributeStore(CACHE_DIR).layerExists(inLayerId)) {
        println(s"Found cached layer ${CACHE_DIR}:${inLayerId}.")
        val hadoopLayerReader = HadoopLayerReader(CACHE_DIR)
        hadoopLayerReader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
      }
      else {
        println(s"Could not find cached layer ${CACHE_DIR}:${inLayerId}, pulling from S3 and creating.")
        val rdd = s3LayerReader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
        val hadoopLayerWriter = HadoopLayerWriter(CACHE_DIR)
        hadoopLayerWriter.write(inLayerId, rdd, ZCurveKeyIndexMethod)
        rdd
      }
    }

    /* Produce viewable elevation layer. */
    val histogram = rdd0.histogram(512)
    val breaks = histogram.quantileBreaks(256)
    val p = projection(breaks)(_)
    println(s"METADATA=${rdd0.metadata}\n")
    println(s"QUANTILES=${breaks.toList}\n")
    println(s"LAYERS=${gwAttributeStore.layerIds}\n")
    val rdd1 = ContextRDD(
      rdd0.map({case (key, tile) =>
        val newTile = UByteArrayTile.empty(tile.cols, tile.rows)

        var i = 0; while (i < tile.cols) {
          var j = 0; while (j < tile.rows) {
            newTile.set(i, j, p(tile.getDouble(i, j)))
            j += 1
          }
          i += 1
        }

        (key, newTile)
      }),
      TileLayerMetadata(
        UByteConstantNoDataCellType,
        rdd0.metadata.layout,
        rdd0.metadata.extent,
        rdd0.metadata.crs,
        rdd0.metadata.bounds
      )
    )

    /* Write RDD into GeoWave */
    layerWriter.write(gwLayerId, rdd1, ZCurveKeyIndexMethod)

    /* Read RDD out of GeoWave */
    val gwLayerReader = new GeowaveLayerReader(gwAttributeStore)
    val rdd2 = gwLayerReader
      .query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId(args(5), 10))
      .where(Intersects(rdd1.metadata.extent))
      .result
    rdd2.collect.foreach({ case (k, v) =>
      val extent = rdd2.metadata.mapTransform(k)
      val pr = ProjectedRaster(Raster(v, extent), LatLng)
      val gc = pr.toGridCoverage2D
      val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/${args(5)}/${System.currentTimeMillis}.tif"))
      writer.write(gc, Array.empty[GeneralParameterValue])
    })
  }

}
