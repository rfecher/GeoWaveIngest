#!/bin/bash

java -cp /poke-jar/raster-poke-assembly-0.jar com.daystrom_data_concepts.raster.RasterIngest geodocker-zookeeper gis root GisPwd gwRaster /rasters/TC_NG_Baghdad_IQ_Geo.tif
/spark/bin/spark-submit --master='spark://geodocker-spark-master:7077' --conf 'spark.driver.memory=1G' --class com.example.raster.RasterDisgorge /peek-jar/raster-peek-assembly-0.jar geodocker-zookeeper gis root GisPwd gwRaster
