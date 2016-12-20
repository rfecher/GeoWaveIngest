package com.daystrom_data_concepts.raster

import geotrellis.geotools._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.geowave._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._
import geotrellis.vector.Extent
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.geotools.gce.geotiff._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod.spatialKeyIndexMethod
import geotrellis.spark.io.accumulo.HdfsWriteStrategy
import geotrellis.spark.io.accumulo.SocketWriteStrategy


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

    /* Give arguments meaningful names */
    val zookeepers = args(0)
    val accumuloInstance = args(1)
    val accumuloUser = args(2)
    val accumuloPass = args(3)
    val geowaveNamespace = args(4)
    val layerName = args(5)
    val zoomLevel = args(6)

    val gwAttributeStore = new GeowaveAttributeStore(zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace)
    val layerWriter = new GeowaveLayerWriter(gwAttributeStore, if(args.length > 7) HdfsWriteStrategy(args(7)) else SocketWriteStrategy())
    val gwLayerId = LayerId(layerName, zoomLevel.toInt)

    val rdd0 = {
      val s3AttributeStore = S3AttributeStore("osm-elevation", "catalog")
      val s3LayerReader = S3LayerReader(s3AttributeStore)
      val inLayerId = LayerId(args(5), args(6).toInt)
      val subset = Extent(-87.1875, 34.43409789359469, -78.15673828125, 39.87601941962116)

      if (HadoopAttributeStore(CACHE_DIR).layerExists(inLayerId)) {
        println(s"Found cached layer ${CACHE_DIR}:${inLayerId}.")
        HadoopLayerReader(CACHE_DIR)
          .query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
          .where(Intersects(subset))
          .result
      }
      else {
        println(s"Could not find cached layer ${CACHE_DIR}:${inLayerId}, pulling from S3 and creating.")
        val rdd = s3LayerReader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
        HadoopLayerWriter(CACHE_DIR).write(inLayerId, rdd, ZCurveKeyIndexMethod)
        HadoopLayerReader(CACHE_DIR)
          .query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
          .where(Intersects(subset))
          .result
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
        val newTile = UByteArrayTile.empty(tile.cols, tile.rows, UByteUserDefinedNoDataCellType(0))

        var i = 0; while (i < tile.cols) {
          var j = 0; while (j < tile.rows) {
            newTile.set(i, j, p(tile.getDouble(i, j)))
            j += 1
          }
          i += 1
        }
        //3 band data using 1 band?
        (key, ArrayMultibandTile(newTile, newTile, newTile).asInstanceOf[MultibandTile])
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

    // /* Read RDD out of GeoWave */
    // val gwLayerReader = new GeowaveLayerReader(gwAttributeStore)
    // val rdd2 = gwLayerReader
    //   .query[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](LayerId(args(5), 10))
    //   .where(Intersects(rdd1.metadata.extent))
    //   .result
    // rdd2.collect.foreach({ case (k, v) =>
    //   val extent = rdd2.metadata.mapTransform(k)
    //   val pr = ProjectedRaster(Raster(v, extent), LatLng)
    //   val gc = pr.toGridCoverage2D
    //   val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/${args(5)}/${System.currentTimeMillis}.tif"))
    //   writer.write(gc, Array.empty[GeneralParameterValue])
    // })

    println("done")
  }

}
