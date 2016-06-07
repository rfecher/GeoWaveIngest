import sbt._
import scala.util.Properties

object Version {
  def either(environmentVariable: String, default: String): String = Properties.envOrElse(environmentVariable, default)

  lazy val hadoop  = either("SPARK_HADOOP_VERSION", "2.6.2")
  lazy val spark   = either("SPARK_VERSION", "1.6.1")
  lazy val geowave = "0.9.2-SNAPSHOT"
  lazy val geotools = "14.2"
  lazy val geoserver = "2.8.2"
}
