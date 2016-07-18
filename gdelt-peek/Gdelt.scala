package com.daystrom_data_concepts.gdelt

import com.vividsolutions.jts.geom.Point
import org.geotools.feature.AttributeTypeBuilder
import org.geotools.feature.simple._
import org.opengis.feature.simple._


/**
  * https://github.com/moradology/gwVectorIngest/blob/feature/rddWriteAndRead
  */
object Gdelt {
  val FEATURE_NAME = "gdeltevent"

  val gdeltCodeNames = List("GlobalEventId",                // 0
                            "Day",                          // 1
                            "MonthYear",                    // 2
                            "Year",                         // 3
                            "FractionDate",                 // 4
                            "Actor1Code",                   // 5
                            "Actor1Name",                   // 6
                            "Actor1CountryCode",            // 7
                            "Actor1KnownGroupCode",         // 8
                            "Actor1EthnicCode",             // 9
                            "Actor1Religion1Code",          // 10
                            "Actor1Religion2Code",          // 11
                            "Actor1Type1Code",              // 12
                            "Actor1Type2Code",              // 13
                            "Actor1Type3Code",              // 14
                            "Actor2Code",                   // 15
                            "Actor2Name",                   // 16
                            "Actor2CountryCode",            // 17
                            "Actor2KnownGroupCode",         // 18
                            "Actor2EthnicCode",             // 19
                            "Actor2Religion1Code",          // 20
                            "Actor2Religion2Code",          // 21
                            "Actor2Type1Code",              // 22
                            "Actor2Type2Code",              // 23
                            "Actor2Type3Code",              // 24
                            "IsRootEvent",                  // 25
                            "EventCode",                    // 26
                            "EventBaseCode",                // 27
                            "EventRootCode",                // 28
                            "QuadClass",                    // 29
                            "GoldsteinScale",               // 30
                            "NumMentions",                  // 31
                            "NumSources",                   // 32
                            "NumArticles",                  // 33
                            "AvgTone",                      // 34
                            "Actor1Geo_Type",               // 35
                            "Actor1Geo_Fullname",           // 36
                            "Actor1Geo_CountryCode",        // 37
                            "Actor1Geo_ADM1Code",           // 38
                            "Actor1Geo_Lat",                // 39
                            "Actor1Geo_Long",               // 40
                            "Actor1Geo_FeatureID",          // 41
                            "Actor2Geo_Type",               // 42
                            "Actor2Geo_Fullname",           // 43
                            "Actor2Geo_CountryCode",        // 44
                            "Actor2Geo_ADM1Code",           // 45
                            "Actor2Geo_Lat",                // 46
                            "Actor2Geo_Long",               // 47
                            "Actor2Geo_FeatureID",          // 48
                            "ActionGeo_Type",               // 49
                            "ActionGeo_Fullname",           // 50
                            "ActionGeo_CountryCode",        // 51
                            "ActionGeo_ADM1Code",           // 52
                            "ActionGeo_Lat",                // 53
                            "ActionGeo_Long",               // 54
                            "ActionGeo_FeatureID",          // 55
                            "DateAdded",                    // 56
                            "SourceURL")                    // 57

  def createGdeltFeatureType(): SimpleFeatureType = {

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

    // GlobalEventId
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(0)))
    // Day
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(1)))
    // MonthYear
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(2)))
    // Year
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(3)))
    // FractionDate
    builder.add(ab.binding(classOf[java.lang.Double]).nillable(false).buildDescriptor(gdeltCodeNames(4)))
    // Actor1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(5)))
    // Actor1Name
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(6)))
    // Actor1CountryCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(7)))
    // Actor1KnownGroupCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(8)))
    // Actor1EthnicCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(9)))
    // Actor1Religion1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(10)))
    // Actor1Religion2Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(11)))
    // Actor1Type1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(12)))
    // Actor1Type2Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(13)))
    // Actor1Type3Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(14)))
    // Actor2Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(15)))
    // Actor2Name
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(16)))
    // Actor2CountryCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(17)))
    // Actor2KnownGroupCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(18)))
    // Actor2EthnicCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(19)))
    // Actor2Religion1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(20)))
    // Actor2Religion2Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(21)))
    // Actor2Type1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(22)))
    // Actor2Type2Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(23)))
    // Actor2Type3Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(24)))
    // IsRootEvent
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(25)))
    // EventCode
    builder.add(ab.binding(classOf[String]).nillable(false).buildDescriptor(gdeltCodeNames(26)))
    // EventBaseCode
    builder.add(ab.binding(classOf[String]).nillable(false).buildDescriptor(gdeltCodeNames(27)))
    // EventRootCode
    builder.add(ab.binding(classOf[String]).nillable(false).buildDescriptor(gdeltCodeNames(28)))
    // QuadClass
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(29)))
    // GoldsteinScale
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(30)))
    // NumMentions
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(31)))
    // NumSources
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(32)))
    // NumArticles
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(33)))
    // AvgTone
    builder.add(ab.binding(classOf[java.lang.Double]).nillable(false).buildDescriptor(gdeltCodeNames(34)))
    // Actor1Geo_Type
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(35)))
    // Actor1Geo_Fullname
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(36)))
    // Actor1Geo_CountryCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(37)))
    // Actor1Geo_ADM1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(38)))
    // Actor1Geo_Lat
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(39)))
    // Actor1Geo_Long
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(40)))
    // Actor1Geo_FeatureID
    builder.add(ab.binding(classOf[Integer]).buildDescriptor(gdeltCodeNames(41)))
    // Actor2Geo_Type
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(42)))
    // Actor2Geo_Fullname
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(43)))
    // Actor2Geo_CountryCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(44)))
    // Actor2Geo_ADM1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(45)))
    // Actor2Geo_Lat
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(46)))
    // Actor2Geo_Long
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(47)))
    // Actor2Geo_FeatureID
    builder.add(ab.binding(classOf[Integer]).buildDescriptor(gdeltCodeNames(48)))
    // ActionGeo_Type
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(49)))
    // ActionGeo_Fullname
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(50)))
    // ActionGeo_CountryCode
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(51)))
    // ActionGeo_ADM1Code
    builder.add(ab.binding(classOf[String]).buildDescriptor(gdeltCodeNames(52)))
    // ActionGeo_Lat
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(53)))
    // ActionGeo_Long
    builder.add(ab.binding(classOf[java.lang.Double]).buildDescriptor(gdeltCodeNames(54)))
    // ActionGeo_FeatureID
    builder.add(ab.binding(classOf[Integer]).buildDescriptor(gdeltCodeNames(55)))
    // DateAdded
    builder.add(ab.binding(classOf[Integer]).nillable(false).buildDescriptor(gdeltCodeNames(56)))
    // SourceURL
    builder.add(ab.binding(classOf[String]).nillable(false).buildDescriptor(gdeltCodeNames(57)))

    builder.add(ab.binding(classOf[Point]).nillable(false).buildDescriptor("geometry"))

    return builder.buildFeatureType()
  }
}
