docker run -it --rm --link leader -v $(pwd)/raster/target/scala-2.11/:/moo:ro -v $HOME/Desktop/:/woof:ro geowaveclient:0
java -cp /moo/ingest-raster-assembly-0.jar com.example.ingest.raster.RasterIngest leader instance root password gw /woof/TC_NG_Baghdad_IQ_Geo.tif
