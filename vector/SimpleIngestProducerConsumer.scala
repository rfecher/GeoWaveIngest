package com.example.ingest.vector

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index.PrimaryIndex
import mil.nga.giat.geowave.core.store.IndexWriter
import mil.nga.giat.geowave.core.store.memory.DataStoreUtils
import mil.nga.giat.geowave.datastore.accumulo.BasicAccumuloOperations

import org.apache.log4j.Logger
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.opengis.feature.simple._

import java.io.IOException
import java.util.Iterator
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


object SimpleIngestProducerConsumer {
  def main(args: Array[String]): Unit = (new SimpleIngestProducerConsumer).mainFunction(args)
}

class SimpleIngestProducerConsumer extends SimpleIngest {

  override val log = Logger.getLogger(classOf[SimpleIngestProducerConsumer])
  val features = new FeatureCollection()

  def mainFunction(args: Array[String]): Unit = {
    if (args.length != 5) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace")
      System.exit(1)
    }

    val si = new SimpleIngestProducerConsumer()

    try {
      val bao = si.getAccumuloOperationsInstance(args(0), args(1), args(2), args(3), args(4))
      val geowaveDataStore = si.getGeowaveDataStore(bao)
      si.generateGrid(geowaveDataStore)
    }
    catch {
      case e: Exception =>
        log.error("Error creating BasicAccumuloOperations", e)
        System.exit(1);
    }

    System.out.println(s"Finished ingesting data to namespace: ${args(4)} at accumulo instance: ${args(1)}")
  }

  def generateGrid(geowaveDataStore: DataStore): Unit = {

    // In order to store data we need to determine the type of data store
    val point = createPointFeatureType()

    // This a factory class that builds simple feature objects based on the
    // type passed
    val pointBuilder = new SimpleFeatureBuilder(point)

    // This is an adapter, that is needed to describe how to persist the
    // data type passed
    val adapter = createDataAdapter(point)

    // This describes how to index the data
    val index = createSpatialIndex()

    val ingestThread = new Thread(
      new Runnable() {
        override def run(): Unit = {
          try {
            val writer = geowaveDataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[SimpleFeature]]
            while (features.hasNext()) {
              val sft = features.next()
              writer.write(sft)
            }
          }
          catch {
            case e: IOException =>
              // TODO Auto-generated catch block
              e.printStackTrace()
          }
        }
      },
      "Ingestion Thread")

    ingestThread.start()

    // build a grid of points across the globe at each whole
    // latitude/longitude intersection
    for (sft <- getGriddedFeatures(pointBuilder, -10000)) {
      features.add(sft)
    }
    features.ingestCompleted = true
    try {
      ingestThread.join();
    }
    catch {
      case e: InterruptedException =>
        log.error("Error joining ingest thread", e);
    }
  }

  class FeatureCollection extends Iterator[SimpleFeature] {
    val queue = new LinkedBlockingQueue[SimpleFeature](10000)
    var ingestCompleted = false

    def add(sft: SimpleFeature): Unit = {
      try {
        queue.put(sft)
      }
      catch {
        case e: InterruptedException =>
          log.error("Error inserting next item into queue", e);
      }
    }

    override def hasNext(): Boolean = {
      !(ingestCompleted && queue.isEmpty())
    }

    override def next(): SimpleFeature = {
      try {
        return queue.take()
      }
      catch {
        case e: InterruptedException =>
          log.error("Error getting next item from queue", e)
      }
      return null;
    }

    override def remove(): Unit = {
      log.error("Remove called, method not implemented");
    }
  }
}
