   * docker run -it --rm --net=geowave -v $HOME/Pictures/tif:/rasters:ro -v $(pwd)/raster/target/scala-2.11:/jars:ro -v /tmp/tif:/tmp/tif java:openjdk-8u72-jdk
   * java -cp /jars/ingest-vector-assembly-0.jar com.example.ingest.vector.SimpleIngestIndexWriter leader instance root password gwVector
   * java -cp /jars/ingest-raster-assembly-0.jar com.example.ingest.raster.RasterIngest leader instance root password gwRaster /rasters/TC_NG_Baghdad_IQ_Geo.tif
