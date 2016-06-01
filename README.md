   * Poke
      - docker run -it --rm --net=geowave -v $HOME/Pictures/tif:/rasters:ro -v $(pwd)/raster-poke/target/scala-2.11:/jars:ro java:openjdk-8u72-jdk
      - java -cp /jars/raster-poke-assembly-0.jar com.example.raster.RasterIngest leader instance root password gwRaster /rasters/TC_NG_Baghdad_IQ_Geo.tif
   * Peek
      - docker run -it --rm --net=geowave -v $(pwd)/raster-peek/target/scala-2.11:/jars:ro -v /tmp/tif:/tmp/tif java:openjdk-8u72-jdk
      - java -cp /jars/raster-peek-assembly-0.jar com.example.raster.RasterDisgorge leader instance root password gwRaster
