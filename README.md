   * Server
      - docker run -it --rm -p 9995:9995 --net=geowave --hostname leader --name leader jamesmcclain/geowave:3
      - docker run -it --rm --net=geowave -p 8080:8080 jamesmcclain/geoserver:2
   * Vector
      - docker run -it --rm --net=geowave -v $(pwd)/vector/target/scala-2.10:/jars:ro java:openjdk-8u72-jdk
      - java -cp /jars/vector-assembly-0.jar com.example.ingest.vector.SimpleIngestIndexWriter leader instance root password gwVector
   * Poke
      - docker run -it --rm --net=geowave -v $HOME/Pictures/tif:/rasters:ro -v $(pwd)/raster-poke/target/scala-2.10:/jars:ro java:openjdk-8u72-jdk
      - java -cp /jars/raster-poke-assembly-0.jar com.daystrom_data_concepts.raster.RasterIngest leader instance root password gwRaster /rasters/TC_NG_Baghdad_IQ_Geo.tif
   * SuperPoke
      - docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/super-poke/target/scala-2.10:/jars:ro -v $HOME/catalog:/catalog:ro -v /tmp/tif:/tmp/tif java:openjdk-8u72-jdk
      - /spark/bin/spark-submit --master='local[1]' --conf 'spark.driver.memory=8G' --class com.example.raster.SuperIngest /jars/super-poke-assembly-0.jar leader instance root password gwRaster 'file:///catalog' ccsm4-rcp45 0 SpatialKey Tile
   * Peek
      - docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/raster-peek/target/scala-2.10:/jars:ro -v /tmp/tif:/tmp/tif java:openjdk-8u72-jdk
      - /spark/bin/spark-submit --master='local[1]' --conf 'spark.driver.memory=1G' --class com.daystrom_data_concepts.raster.RasterDisgorge /jars/raster-peek-assembly-0.jar leader instance root password gwRaster
