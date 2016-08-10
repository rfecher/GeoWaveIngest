package com.daystrom_data_concepts.raster

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.hillshade._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.SocketWriteStrategy
import geotrellis.spark.io.geowave._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3._
import geotrellis.vector.Extent

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue


object Demo {

  val logger = Logger.getLogger(Demo.getClass)
  val CACHE_DIR = "/tmp/catalog-cache"

  /**
    * Project a value (from some raw raster) into a viewable range
    * by use of a list of quantiles.
    */
  def projection(quantiles: Array[Double])(x: Double): Int = {
    val i = java.util.Arrays.binarySearch(quantiles, x)
    if (i < 0) ((-1 * i) - 1); else i
  }

  /**
    * Project all of the values of the pixel values of a key+tile RDD
    * into a viewable range.
    */
  def viewable(rdd: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]) = {
    val histogram = rdd.histogram(512)
    val breaks = histogram.quantileBreaks(256)
    val p = projection(breaks)(_)

    ContextRDD(
      rdd.map({case (key, tile) =>
        val newTile = UByteArrayTile.empty(tile.cols, tile.rows, UByteUserDefinedNoDataCellType(0))

        var i = 0; while (i < tile.cols) {
          var j = 0; while (j < tile.rows) {
            newTile.set(i, j, p(tile.getDouble(i, j)))
            j += 1
          }
          i += 1
        }

        (key, newTile.asInstanceOf[Tile])
      }),
      TileLayerMetadata(
        UByteConstantNoDataCellType,
        rdd.metadata.layout,
        rdd.metadata.extent,
        rdd.metadata.crs,
        rdd.metadata.bounds
      )
    )
  }

  def main(args: Array[String]) : Unit = {

    /* Command line arguments */
    if (args.length < 7) {
      logger.error("Invalid arguments, expected: <zookeepers> <accumuloInstance> <accumuloUser> <accumuloPass> <gwNamespace> <layerName> <zoomLevel>")
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
    val zoomLevel = args(6).toInt

    val gwAttributeStore = new GeowaveAttributeStore(zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace)
    val layerWriter = new GeowaveLayerWriter(gwAttributeStore, SocketWriteStrategy())
    val layerReader = new GeowaveLayerReader(gwAttributeStore)

    /* Read a starting layer */
    val rdd1 = {
      val inLayerId = LayerId(layerName, zoomLevel)
      val subset = Extent(-87.1875, 34.43409789359469, -78.15673828125, 39.87601941962116)

      require(HadoopAttributeStore(CACHE_DIR).layerExists(inLayerId))
      HadoopLayerReader(CACHE_DIR)
        // .read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
        .query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](inLayerId)
        .where(Intersects(subset))
        .result
    }

    layerWriter.write(LayerId(layerName + "-raw", 11), rdd1)
    layerWriter.write(LayerId(layerName + "-viewable", 11), viewable(rdd1))

    val rdd2 = layerReader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId(layerName + "-raw", 11))

    // val mt = rdd2.metadata.mapTransform
    // viewable(rdd2).collect.foreach({ case (k, v) =>
    //   val extent = mt(k)
    //   val pr = ProjectedRaster(Raster(v, extent), LatLng)
    //   val gc = pr.toGridCoverage2D
    //   val writer = new GeoTiffWriter(new java.io.File(s"/tmp/tif/geotrellis-${System.currentTimeMillis}.tif"))
    //   writer.write(gc, Array.empty[GeneralParameterValue])
    // })

    layerWriter.write(LayerId(layerName + "-hillshade", 11), viewable(rdd2.hillshade()))
  }

}
