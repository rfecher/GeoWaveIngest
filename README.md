   * docker run -it --rm --link leader -v $(pwd)/raster/target/scala-2.11/:/moo:ro -v $HOME/Desktop/:/woof:ro geowaveclient:0
   * java -cp /moo/ingest-vector-assembly-0.jar com.example.ingest.vector.SimpleIngestIndexWriter leader instance root password gwVector
   * java -cp /moo/ingest-raster-assembly-0.jar com.example.ingest.raster.RasterIngest leader instance root password gwRaster /woof/TC_NG_Baghdad_IQ_Geo.tif
