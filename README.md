# GeoTrellis + GeoWave Hillshade Demo #

## Obtaining the Data ##

### Method 1: Pre-Ingested ###

Obtain the `catalog-cache.tar.gz` file.
Untar the file, and edit the `catalog-cache/_attributes/ned___0___metadata.json` file so that the value associated with the `header.path` key points to the correct location (either on disk or in HDFS).

### Method 2: Download and Ingest ###

This requires the Azavea DataHub ingest jar `com.azavea.datahub.etl-assembly-0.1.5.jar`.

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

The raw data can be transformed into a GeoTrellis layer with a command similar to the following.
```bash
spark-submit \
    --master='local[*]' \
    --class com.azavea.datahub.etl.Ingest \
    --conf spark.executor.memory=8G \
    com.azavea.datahub.etl-assembly-0.1.5.jar \
    --input hadoop --format geotiff -I path=file://$HOME/Downloads/NED \
    --output hadoop -O path=file:///tmp/catalog-cache \
    --layer ned \
    --crs EPSG:4326 \
    --layoutScheme floating
```
This should be modified with the appropriate input and output locations.

## Building The Demo Code ##

This demo requires the code found in the GeoTrellis [GeoWave subproject pull request](https://github.com/geotrellis/geotrellis/pull/1542).
At time of writing, that pull request has not been merged.
If that is the case at the time of reading, then that branch must be pulled down and published locally, otherwise an appropriate released or pre-release version of GeoTrellis can be used.

It is also required that `sbt` is installed.

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
docker run -it --rm -p 50095:50095 --net=geowave --hostname leader --name leader jamesmcclain/geowave:8760ce2
```

The ingest into GeoWave can then be performed by starting another container for the demo ingest code:
```bash
docker run -it --rm --net=geowave -v $SPARK_HOME:/spark:ro -v $(pwd)/demo/target/scala-2.10:/jars:ro -v /tmp:/tmp:rw openjdk:8-jdk
```
then running the demo ingest within that container
```bash
/spark/bin/spark-submit \
   --master='local[*]' \
   --conf 'spark.driver.memory=32G' \
   --class com.daystrom_data_concepts.raster.RasterDisgorge \
   /jars/raster-peek-assembly-0.jar leader instance root password gwRaster
```

To view the layers, start the GeoServer container:
```bash
docker run -it --rm -p 8080:8080 --net=geowave jamesmcclain/geoserver:8760ce2
```
and make use of the GeoWave raster plugin for GeoServer.

Because of the size of the data, in all probability you will need to zoom in from the initial view before you will be able to see anything.
Monitor the output from GeoServer to see whether errors are being raised and whether the rendering process is making progress (for wide views of the map, rendering may take some time).
Once you have zoomed-in enough, the errors in the GeoServer terminal will scroll out of sight and you will begin to see encouraging messages.

Here are some screenshots of the demo running locally:

![screenshot from 2016-09-16 23_19_59](https://cloud.githubusercontent.com/assets/11281373/18676982/7717a88c-7f25-11e6-8dd3-04c896a5842a.png)
![screenshot from 2016-09-16 23_25_20](https://cloud.githubusercontent.com/assets/11281373/18676981/7716cd7c-7f25-11e6-9d21-0c76ddb943dc.png)
![screenshot from 2016-09-16 23_30_18](https://cloud.githubusercontent.com/assets/11281373/18676980/77147ee6-7f25-11e6-98dc-13cf41fa2580.png)
![screenshot from 2016-09-16 23_31_28](https://cloud.githubusercontent.com/assets/11281373/18676984/771b031a-7f25-11e6-86f9-761ef78a25fb.png)
![screenshot from 2016-09-16 23_31_53](https://cloud.githubusercontent.com/assets/11281373/18676978/7711abf8-7f25-11e6-8ad4-3ef9c8b6202e.png)
![screenshot from 2016-09-16 23_32_16](https://cloud.githubusercontent.com/assets/11281373/18676979/7711f96e-7f25-11e6-89c9-5710e1cee989.png)
![screenshot from 2016-09-16 23_32_24](https://cloud.githubusercontent.com/assets/11281373/18676983/77186dc6-7f25-11e6-9e3d-dd713689d9a9.png)
![screenshot from 2016-09-16 23_32_28](https://cloud.githubusercontent.com/assets/11281373/18677004/832eace2-7f25-11e6-8048-dc077125620f.png)
![screenshot from 2016-09-16 23_43_48](https://cloud.githubusercontent.com/assets/11281373/18677002/832dc188-7f25-11e6-95a1-e02fbc3d9e63.png)
![screenshot from 2016-09-16 23_45_49](https://cloud.githubusercontent.com/assets/11281373/18677006/83328fec-7f25-11e6-93ba-3d53f95b543c.png)
![screenshot from 2016-09-16 23_46_20](https://cloud.githubusercontent.com/assets/11281373/18677001/832b2d1a-7f25-11e6-84cf-1df18401ad2e.png)
![screenshot from 2016-09-16 23_46_29](https://cloud.githubusercontent.com/assets/11281373/18677003/832e6a66-7f25-11e6-8f1b-fbe4fc0f4c04.png)
![screenshot from 2016-09-16 23_46_44](https://cloud.githubusercontent.com/assets/11281373/18677005/83307e64-7f25-11e6-98fd-d69a70084048.png)
![screenshot from 2016-09-16 23_46_55](https://cloud.githubusercontent.com/assets/11281373/18677007/83343b8a-7f25-11e6-88ea-eb706ba72c4c.png)

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
   --class com.daystrom_data_concepts.raster.Demo \
   demo-assembly-0.jar ip-172-31-20-102 gis root secret geowave.gwRaster 'hdfs:/catalog-cache' ned 0
```
Where the name of the zookeeper, `ip-172-31-20-102`, should be appropriately substituted.

The ingest into GeoWave will probably take about 90 minutes.
Once it is complete, you can now start GeoServer.
(Note that another copy of GeoServer is already running, but for whatever reason I have not had success using it for this purpose, so you I suggest that you start a different one using the command below.)

```bash
docker run -it --rm -p 8086:8080 jamesmcclain/geoserver:8760ce2
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

Please enjoy the following screenshots of the demo running on EMR:

![screenshot from 2016-09-19 18_59_48](https://cloud.githubusercontent.com/assets/11281373/18677072/aad0406c-7f25-11e6-8406-dcf809e1a0f2.png)

![screenshot from 2016-09-19 19_09_04](https://cloud.githubusercontent.com/assets/11281373/18677280/4cde5c54-7f26-11e6-9681-d65304cc542b.png)
![screenshot from 2016-09-19 19_09_52](https://cloud.githubusercontent.com/assets/11281373/18677283/4ce2b8a8-7f26-11e6-8756-9569c49fe226.png)
![screenshot from 2016-09-19 19_10_24](https://cloud.githubusercontent.com/assets/11281373/18677285/4ceb62dc-7f26-11e6-9249-ca3227e5edfd.png)
![screenshot from 2016-09-19 19_14_31](https://cloud.githubusercontent.com/assets/11281373/18677284/4ce2c906-7f26-11e6-82f0-177fbb442b81.png)
![screenshot from 2016-09-19 19_15_42](https://cloud.githubusercontent.com/assets/11281373/18677281/4ce08952-7f26-11e6-8fc5-1398b302f87f.png)
![screenshot from 2016-09-19 19_16_43](https://cloud.githubusercontent.com/assets/11281373/18677282/4ce17b8c-7f26-11e6-98c4-337d1ae5cdff.png)
![screenshot from 2016-09-19 19_17_35](https://cloud.githubusercontent.com/assets/11281373/18677286/4d01138e-7f26-11e6-9820-7acbebbc1337.png)
![screenshot from 2016-09-19 19_18_47](https://cloud.githubusercontent.com/assets/11281373/18677298/5835fbde-7f26-11e6-8c96-098490ccf9da.png)
![screenshot from 2016-09-19 19_19_37](https://cloud.githubusercontent.com/assets/11281373/18677299/58381482-7f26-11e6-9f6d-d9686eead79d.png)

![screenshot from 2016-09-19 19_21_02](https://cloud.githubusercontent.com/assets/11281373/18677371/8bcf12b4-7f26-11e6-8089-cab06acbec26.png)
![screenshot from 2016-09-19 19_22_10](https://cloud.githubusercontent.com/assets/11281373/18677368/8bca1ea8-7f26-11e6-991e-de01cd57be02.png)
![screenshot from 2016-09-19 19_22_39](https://cloud.githubusercontent.com/assets/11281373/18677366/8bc6dfc2-7f26-11e6-82cb-195ab635ad78.png)
![screenshot from 2016-09-19 19_23_26](https://cloud.githubusercontent.com/assets/11281373/18677367/8bc7be6a-7f26-11e6-9663-04c348b0d69c.png)
![screenshot from 2016-09-19 19_24_00](https://cloud.githubusercontent.com/assets/11281373/18677365/8bc5d42e-7f26-11e6-8dce-2977feec6532.png)
![screenshot from 2016-09-19 19_24_46](https://cloud.githubusercontent.com/assets/11281373/18677369/8bcd2ab2-7f26-11e6-83b5-6638fb30173f.png)
![screenshot from 2016-09-19 19_24_56](https://cloud.githubusercontent.com/assets/11281373/18677370/8bceff7c-7f26-11e6-8c94-96261e12ad57.png)
![screenshot from 2016-09-19 19_25_01](https://cloud.githubusercontent.com/assets/11281373/18677372/8bd0622c-7f26-11e6-8e92-4c5d0e2ceb45.png)
![screenshot from 2016-09-19 19_25_05](https://cloud.githubusercontent.com/assets/11281373/18677373/8bd70d52-7f26-11e6-805b-5b54f20dded4.png)

![screenshot from 2016-09-19 19_25_26](https://cloud.githubusercontent.com/assets/11281373/18677402/a68a75ee-7f26-11e6-9f4d-bf75c36b61f6.png)
![screenshot from 2016-09-19 19_26_08](https://cloud.githubusercontent.com/assets/11281373/18677408/a694bf86-7f26-11e6-9f63-a8baaebb4d80.png)
![screenshot from 2016-09-19 19_26_54](https://cloud.githubusercontent.com/assets/11281373/18677404/a68e8652-7f26-11e6-9a97-ffd1e729de42.png)
![screenshot from 2016-09-19 19_27_38](https://cloud.githubusercontent.com/assets/11281373/18677403/a68d38ba-7f26-11e6-9ef1-14b731054843.png)
![screenshot from 2016-09-19 19_28_34](https://cloud.githubusercontent.com/assets/11281373/18677406/a68fd7aa-7f26-11e6-815f-3cb6e46168e8.png)
![screenshot from 2016-09-19 19_30_11](https://cloud.githubusercontent.com/assets/11281373/18677405/a68f3b1a-7f26-11e6-8903-be1f0235d48a.png)
![screenshot from 2016-09-19 19_30_34](https://cloud.githubusercontent.com/assets/11281373/18677407/a693c82e-7f26-11e6-84b3-f91c259194a5.png)
