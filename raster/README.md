```
java.lang.Throwable
        at org.apache.accumulo.core.client.impl.TabletServerBatchReader.<init>(TabletServerBatchReader.java:69)
        at org.apache.accumulo.core.client.impl.ConnectorImpl.createBatchScanner(ConnectorImpl.java:96)
        at mil.nga.giat.geowave.datastore.accumulo.BasicAccumuloOperations.createBatchScanner(BasicAccumuloOperations.java:594)
        at mil.nga.giat.geowave.datastore.accumulo.metadata.AbstractAccumuloPersistence.getScanner(AbstractAccumuloPersistence.java:390)
        at mil.nga.giat.geowave.datastore.accumulo.metadata.AbstractAccumuloPersistence.getFullScanner(AbstractAccumuloPersistence.java:379)
        at mil.nga.giat.geowave.datastore.accumulo.metadata.AbstractAccumuloPersistence.getObjects(AbstractAccumuloPersistence.java:345)
        at mil.nga.giat.geowave.datastore.accumulo.metadata.AccumuloAdapterStore.getAdapters(AccumuloAdapterStore.java:59)
        at com.example.ingest.raster.RasterIngest$.peek(RasterIngest.scala:66)
        at com.example.ingest.raster.RasterIngest$.main(RasterIngest.scala:90)
        at com.example.ingest.raster.RasterIngest.main(RasterIngest.scala)
```
