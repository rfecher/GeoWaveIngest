Requires [a default constructor in `IndexOnlySpatialQuery`](https://github.com/ngageoint/geowave/pull/818) and [the custom query fix](https://github.com/ngageoint/geowave/pull/819) in order to work.
Commit `ccf6fc3` on the GeoWave `master` branch should be sufficient.

```
java.io.IOException: The stream is closed
        at org.apache.hadoop.net.SocketOutputStream.write(SocketOutputStream.java:118)
        at java.io.BufferedOutputStream.flushBuffer(BufferedOutputStream.java:82)
        at java.io.BufferedOutputStream.flush(BufferedOutputStream.java:140)
        at java.io.FilterOutputStream.close(FilterOutputStream.java:158)
        at org.apache.thrift.transport.TIOStreamTransport.close(TIOStreamTransport.java:110)
        at org.apache.thrift.transport.TFramedTransport.close(TFramedTransport.java:89)
        at org.apache.accumulo.core.client.impl.ThriftTransportPool$CachedTTransport.close(ThriftTransportPool.java:309)
        at org.apache.accumulo.core.client.impl.ThriftTransportPool.returnTransport(ThriftTransportPool.java:571)
        at org.apache.accumulo.core.rpc.ThriftUtil.returnClient(ThriftUtil.java:151)
        at org.apache.accumulo.core.client.impl.TabletServerBatchReaderIterator.doLookup(TabletServerBatchReaderIterator.java:686)
        at org.apache.accumulo.core.client.impl.TabletServerBatchReaderIterator$QueryTask.run(TabletServerBatchReaderIterator.java:349)
        at org.apache.htrace.wrappers.TraceRunnable.run(TraceRunnable.java:57)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at org.apache.accumulo.fate.util.LoggingRunnable.run(LoggingRunnable.java:35)
        at java.lang.Thread.run(Thread.java:745)
```
