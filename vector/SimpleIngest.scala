package com.example.ingest.vector

import java.util.Date

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter
import mil.nga.giat.geowave.core.geotime.GeometryUtils
import mil.nga.giat.geowave.core.geotime.ingest.SpatialDimensionalityTypeProvider
import mil.nga.giat.geowave.core.store.DataStore
import mil.nga.giat.geowave.core.store.index.PrimaryIndex
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary.AccumuloSecondaryIndexDataStore
import mil.nga.giat.geowave.datastore.accumulo.metadata._

import org.apache.accumulo.core.client._
import org.apache.log4j.Logger
import org.geotools.feature.AttributeTypeBuilder
import org.geotools.feature.simple._
import org.opengis.feature.simple._

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry

import scala.collection.mutable


class SimpleIngest {

  val log = Logger.getLogger(classOf[SimpleIngest])
  val FEATURE_NAME = "GridPoint"

  def getGriddedFeatures(pointBuilder: SimpleFeatureBuilder, firstFeatureId: Int): List[SimpleFeature] = {
    var featureId: Int = firstFeatureId
    val feats = mutable.ArrayBuffer.empty[SimpleFeature]

    var longitude = -180; while (longitude <= 180) {
      var latitude = -90; while (latitude <= 90) {
        pointBuilder.set(
          "geometry",
          GeometryUtils.GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude))
        )
        pointBuilder.set("TimeStamp", new Date())
        pointBuilder.set("Latitude", latitude)
        pointBuilder.set("Longitude", longitude)
        // Note since trajectoryID and comment are marked as nillable we
        // don't need to set them (they default ot null).

        feats += pointBuilder.buildFeature(String.valueOf(featureId))
        featureId += 1

        latitude += 5
      }
      longitude += 5
    }

    feats.toList
  }

  def getGeowaveDataStore(instance: BasicAccumuloOperations): DataStore = {

    // GeoWave persists both the index and data adapter to the same accumulo
    // namespace as the data. The intent here
    // is that all data is discoverable without configuration/classes stored
    // outside of the accumulo instance.
    return new AccumuloDataStore(
      new AccumuloIndexStore(instance),
      new AccumuloAdapterStore(instance),
      new AccumuloDataStatisticsStore(instance),
      new AccumuloSecondaryIndexDataStore(instance),
      new AccumuloAdapterIndexMappingStore(instance),
      instance);
  }

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

  def createDataAdapter(sft: SimpleFeatureType): FeatureDataAdapter = {
    return new FeatureDataAdapter(sft)
  }

  def createSpatialIndex(): PrimaryIndex = {
    // Reasonable values for spatial and spatio-temporal are provided
    // through static factory methods.
    // They are intended to be a reasonable starting place - though creating
    // a custom index may provide better
    // performance is the distribution/characterization of the data is well
    // known.
    return new SpatialDimensionalityTypeProvider().createPrimaryIndex()
  }

  def createPointFeatureType(): SimpleFeatureType = {

    val builder = new SimpleFeatureTypeBuilder()
    val ab = new AttributeTypeBuilder()

    // Names should be unique (at least for a given GeoWave namespace) -
    // think about names in the same sense as a full classname
    // The value you set here will also persist through discovery - so when
    // people are looking at a dataset they will see the
    // type names associated with the data.
    builder.setName(FEATURE_NAME)

    // The data is persisted in a sparse format, so if data is nullable it
    // will not take up any space if no values are persisted.
    // Data which is included in the primary index (in this example
    // lattitude/longtiude) can not be null
    // Calling out latitude an longitude separately is not strictly needed,
    // as the geometry contains that information. But it's
    // convienent in many use cases to get a text representation without
    // having to handle geometries.
    builder.add(
      ab
        .binding(classOf[Geometry])
        .nillable(false)
        .buildDescriptor("geometry")
    )
    builder.add(
      ab
        .binding(classOf[Date])
        .nillable(true)
        .buildDescriptor("TimeStamp")
    )
    builder.add(
      ab
        .binding(classOf[java.lang.Double])
        .nillable(false)
        .buildDescriptor("Latitude")
    )
    builder.add(
      ab
        .binding(classOf[java.lang.Double])
        .nillable(false)
        .buildDescriptor("Longitude")
    )
    builder.add(
      ab
        .binding(classOf[String])
        .nillable(true)
        .buildDescriptor("TrajectoryID")
    )
    builder.add(
      ab
        .binding(classOf[String])
        .nillable(true)
        .buildDescriptor("Comment")
    )

    return builder.buildFeatureType();
  }
}
