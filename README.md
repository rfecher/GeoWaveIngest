   * docker run -it --rm --link leader -v $(JAR_LOCATION):/jars:ro -v $(RASTER_LOCATION):/rasters:ro geowaveclient:0
   * java -cp /jars/ingest-vector-assembly-0.jar com.example.ingest.vector.SimpleIngestIndexWriter leader instance root password gwVector
   * java -cp /jars/ingest-raster-assembly-0.jar com.example.ingest.raster.RasterIngest leader instance root password gwRaster /rasters/raster.tiff

   * globalVisibility = context.getConfiguration().get(AbstractMapReduceIngest.GLOBAL_VISIBILITY_KEY);
   * primaryIndexIds = AbstractMapReduceIngest.getPrimaryIndexIds(context.getConfiguration());
