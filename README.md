# GeoTrellis + GeoWave Hillshade Demo #

## Obtaining the Data ##

### Method 1: Pre-Ingested ###

Obtain the `catalog-cache.tar.gz` file.
Untar the file, and edit the `catalog-cache/_attributes/ned___0___metadata.json` file so that the value associated with the `header.path` key points to the correct location (either on disk or in HDFS).

### Method 2: Download and Ingest ###

#### Obtain Raw Data ####

The demo uses [Mapzen Elevation Data](https://mapzen.com/blog/elevation) at zoom level 9 covering roughly the continental U.S.
Those data can be obtained with the following command:
```bash
for x in $(seq 80 160)
do
  for y in $(seq 180 220)
  do
    wget https://terrain-preview.mapzen.com/geotiff/9/${x}/${y}.tif -O 9_${x}_${y}.tif
  done
done
```

#### Ingest The Data Into GeoTrellis ####

This process requires one additional JAR file and three JSON files.

The extra JAR file can be built directly from the GeoTrellis source tree.
That can be done by typing
```bash
cd $HOME/local/src/geotrellis
./sbt "project spark-etl" assembly
cp spark-etl/target/scala-2.11/geotrellis-spark-etl-assembly-1.0.0-SNAPSHOT.jar /tmp
```
or similar, where paths and the assembly filename are changed as appropriate.

The three required JSON files are in a directory called `json` under the root of this project.
Those three files should be edited as appropriate to make sure that the various paths given therein are correct.
More information about the ETL process can be found [here](https://github.com/geotrellis/geotrellis/blob/master/docs/spark-etl/spark-etl-run-examples.md).

The raw data can be transformed into a GeoTrellis layer with a command similar to either of the following:
```bash
spark-submit \
   --class geotrellis.spark.etl.SinglebandIngest \
   --master 'local[*]' \
   --driver-memory 16G \
   geotrellis-spark-etl-assembly-1.0.0-SNAPSHOT.jar \
   --input "file:///tmp/input.json" \
   --output "file:///tmp/output.json" \
   --backend-profiles "file:///tmp/backend-profiles.json"
```

## Building The Demo Code ##

It is required that `sbt` is installed.

Given those dependencies, the code can be built by typing:
```bash
sbt "project demo" assembly
```

## Running The Demo Code ##

### Locally ###

To run the demo comfortably, one probably needs at least 32 GB of RAM.

The Accumulo container can be started by typing the following:
```bash
docker network create -d bridge geowave
docker run -it --rm -p 50095:50095 --net=geowave --hostname leader --name leader jamesmcclain/geowave:c127c16
```

The ingest into GeoWave can then be performed by starting another container for the demo ingest code:
```bash
docker run -it --rm --net=geowave -v $HOME/local/spark-2.0.0-bin-hadoop2.7:/spark:ro -v $(pwd)/demo/target/scala-2.11:/jars:ro -v /tmp:/tmp:rw openjdk:8-jdk
```
then running the demo ingest within that container
```bash
/spark/bin/spark-submit \
   --master='local[*]' --conf 'spark.driver.memory=32G' \
   --class com.azavea.geotrellis.geowave.Demo /jars/demo-assembly-0.jar \
   leader instance root password gwRaster 'file:/tmp/catalog-cache' ned 0
```

To view the layers, start the GeoServer container
```bash
docker run -it --rm -p 8080:8080 --net=geowave jamesmcclain/geoserver:c127c16
```
and make use of the GeoWave raster plugin for GeoServer.

Because of the size of the data, in all probability you will need to zoom in from the initial view before you will be able to see anything.
Monitor the output from GeoServer to see whether errors are being raised and whether the rendering process is making progress
(for wide views of the map, rendering may take some time).
Once you have zoomed-in enough, the errors in the GeoServer terminal will scroll out of sight and you will begin to see encouraging messages.

Here are some screenshots of the demo running locally:

### EMR ###

Launch an EMR cluster with a command something like the following:
```bash
aws emr create-cluster \
   --name "HILLSHADE" \
   --release-label emr-5.0.0 \
   --output text \
   --use-default-roles \
   --ec2-attributes KeyName=my-key \
   --applications Name=Hadoop Name=Spark Name=Zookeeper \
   --instance-groups \
   --region us-east-1 Name=Master,BidPrice=0.5,InstanceCount=1,InstanceGroupType=MASTER,InstanceType=m3.xlarge Name=Workers,BidPrice=0.5,InstanceCount=3,InstanceGroupType=CORE,InstanceType=m3.xlarge \
   --bootstrap-actions Name=BootstrapGeoWave,Path=s3://geotrellis-test/geodocker/bootstrap-geodocker-accumulo.sh,Args=[-i=quay.io/geodocker/accumulo-geowave:latest,-n=gis,-p=secret]
```

Transfer the `demo-assembly-0.jar` file to the master node of the cluster, as well as the raw or pre-ingested data.

If you are using the pre-ingested data, untar the file, edit the layer metadata as described above, and copy the directory to HDFS:
```bash
tar axvf catalog-cache-ned.tar
nano catalog-cache/_attributes/ned___0___metadata.json
hdfs dfs -copyFromLocal catalog-cache hdfs:/catalog-cache
```

If you are not using the pre-ingested data, perform the ingest using the Azavea DataHub as described above.

With the GeoTrellis layer in place, the demo ingest can now be run:
```bash
spark-submit \
   --master yarn \
   --deploy-mode cluster \
   --class com.azavea.geotrellis.geowave.Demo \
   demo-assembly-0.jar ip-172-31-20-102 gis root secret geowave.gwRaster 'hdfs:/catalog-cache' ned 0
```
Where the name of the zookeeper, `ip-172-31-20-102`, should be appropriately substituted.

The ingest into GeoWave will probably take about 90 minutes.
Once it is complete, you can now start GeoServer.
(Note that another copy of GeoServer is already running, but for whatever reason I have not had success using it for this purpose, so you I suggest that you start a different one using the command below.)

```bash
docker run -it --rm -p 8086:8080 jamesmcclain/geoserver:c127c16
```
Once the GeoServer container is up, you will need to `exec` into it and add an appropriate configuration file for the GeoWave raster plugin:
```bash
docker ps -a
docker exec -it xyz_abc bash
```

In the shell inside of the container, create a file called `/tmp/layer2.xml` with the following contents:
```xml
<config>
   <user>root</user>
   <password>secret</password>
   <zookeeper>ip-172-31-20-102</zookeeper>
   <instance>gis</instance>
   <gwNamespace>geowave.gwRaster</gwNamespace>
</config>
```

As before, the name of the zookeeper should be appropriately substituted.
