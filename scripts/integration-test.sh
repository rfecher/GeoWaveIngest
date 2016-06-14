#!/bin/bash

mkdir -p /tmp/tif
rm -f /tmp/tif/*.tif /tmp/tif/*.info
docker run -it --rm --net=geodockercluster_default \
       -v $HOME/Pictures/tif:/rasters:ro \
       -v $SPARK_HOME:/spark:ro -v /tmp/tif:/tmp/tif \
       -v $(pwd)/scripts:/scripts:ro \
       -v $(pwd)/raster-poke/target/scala-2.10:/poke-jar:ro \
       -v $(pwd)/raster-peek/target/scala-2.10:/peek-jar:ro \
       java:openjdk-8u72-jdk /scripts/activities.sh
gdal_merge.py /tmp/tif/direct-*.tif -o /tmp/tif/direct.tif
gdal_merge.py /tmp/tif/geotrellis-*.tif -o /tmp/tif/geotrellis.tif
gdalinfo /tmp/tif/direct.tif > /tmp/tif/direct.info
gdalinfo /tmp/tif/geotrellis.tif > /tmp/tif/geotrellis.info
diff -ru /tmp/tif/direct.info /tmp/tif/geotrellis.info
