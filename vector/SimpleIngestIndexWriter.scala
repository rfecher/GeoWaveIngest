package com.example.ingest.vector

import java.io.IOException;

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter;
import mil.nga.giat.geowave.core.store.DataStore;
import mil.nga.giat.geowave.core.store.IndexWriter;
import mil.nga.giat.geowave.core.store.index.PrimaryIndex;
import mil.nga.giat.geowave.core.store.memory.DataStoreUtils;
import mil.nga.giat.geowave.datastore.accumulo.BasicAccumuloOperations;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;


object SimpleIngestIndexWriter {
  def main(args: Array[String]): Unit = (new SimpleIngestIndexWriter).mainFunction(args)
}

class SimpleIngestIndexWriter extends SimpleIngest {

  override val log = Logger.getLogger(classOf[SimpleIngestIndexWriter])

  def mainFunction(args: Array[String]) : Unit = {
    if (args.length != 5) {
      log.error("Invalid arguments, expected: zookeepers, accumuloInstance, accumuloUser, accumuloPass, geowaveNamespace");
      System.exit(1);
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
        System.exit(1)
    }

    System.out.println(s"Finished ingesting data to namespace: ${args(4)} at accumulo instance: ${args(1)}")
  }

  def generateGrid(geowaveDataStore: DataStore): Unit = {

    // In order to store data we need to determine the type of data store
    val point = createPointFeatureType()

    // This a factory class that builds simple feature objects based on the
    // type passed
    val  pointBuilder = new SimpleFeatureBuilder(point)

    // This is an adapter, that is needed to describe how to persist the
    // data type passed
    val adapter = createDataAdapter(point)

    // This describes how to index the data
    val index = createSpatialIndex()

    // features require a featureID - this should be unqiue as it's a
    // foreign key on the feature
    // (i.e. sending in a new feature with the same feature id will
    // overwrite the existing feature)
    var featureId = 0

    // get a handle on a GeoWave index writer which wraps the Accumulo
    // BatchWriter, make sure to close it (here we use a try with resources
    // block to close it automatically)
    try {
      val indexWriter = geowaveDataStore.createIndexWriter(index, DataStoreUtils.DEFAULT_VISIBILITY)

      // build a grid of points across the globe at each whole
      // lattitude/longitude intersection

      for (sft <- getGriddedFeatures(pointBuilder,1000)) {
        indexWriter.write(adapter, sft)
      }
    }
    catch {
      case e: IOException =>
      log.warn("Unable to close index writer", e);
    }
  }
}
