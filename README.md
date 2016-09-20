   * Server
      - docker run -it --rm -p 50095:50095 --net=geowave --hostname leader --name leader jamesmcclain/geowave:8760ce2
      - docker run -it --rm --net=geowave -p 8080:8080 jamesmcclain/geoserver:8760ce2
   * Raster Poke
      - docker run -it --rm --net=geowave -v $HOME/Pictures/tif:/rasters:ro -v $(pwd)/raster-poke/target/scala-2.10:/jars:ro openjdk:8-jdk
      - java -cp /jars/raster-poke-assembly-0.jar com.daystrom_data_concepts.raster.RasterIngest leader instance root password gwRaster /rasters/TC_NG_Baghdad_IQ_Geo.tif
   * Raster Peek
      - docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/raster-peek/target/scala-2.10:/jars:ro -v /tmp/tif:/tmp/tif openjdk:8-jdk
      - /spark/bin/spark-submit --master='local[*]' --conf 'spark.driver.memory=32G' --class com.daystrom_data_concepts.raster.RasterDisgorge /jars/raster-peek-assembly-0.jar leader instance root password gwRaster
   * GeoWaveLayerWriter Demo
      - docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/demo/target/scala-2.10:/jars:ro -v /tmp:/tmp:rw openjdk:8-jdk
      - /spark/bin/spark-submit --master='local[*]' --conf 'spark.driver.memory=32G' --class com.daystrom_data_concepts.raster.Demo /jars/demo-assembly-0.jar leader instance root password gwRaster 'file:/tmp/catalog-cache' ned 0
   * GDELT Peek
      - docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/gdelt-peek/target/scala-2.10:/jars:ro openjdk:8-jdk
      - /spark/bin/spark-submit --master='local[*]' --conf 'spark.driver.memory=32G' --class com.daystrom_data_concepts.gdelt.GdeltDisgorge /jars/gdelt-peek-assembly-0.jar leader instance root password gwRaster
