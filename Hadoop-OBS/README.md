## Hadoop-OBS

The Hadoop plugin that provides a way to access OBS.  
Available for most OBS providers, such as Open Telekom Cloud.

### Build

#### Requirements

- Java 8
- Maven 3

#### Variables

- `hadoop.version` default: `3.1.1`
  This value indicates which Hadoop distribution and verison you are using, and should generally be found at  
  https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-common.  
  For example: `3.1.1.7.2.15.0-147` / `3.1.1-hw-ei-312032`
- `hadoop.plat.version` default: `3.1.1`
  This value indicates which version of Hadoop you are using.  
  Must be the same as the version of `hadoop.version`.
  For example: `2.8.3`

We only support **hadoop-2.8.x** and above.  
If you are using a private Hadoop build, modify all the hadoop-related dependencies in `pom.xml`.

JAR naming: hadoop-otc-{hadoop.plat.version}-{this lib version}

#### Commands

`mvn clean install -Pdist -Dhadoop.plat.version=3.1.1 -Dhadoop.version=3.1.1 -Dmaven.test.skip=true`

Pdist: the hadoop-obs dependency obs java sdk, as well as okttp and other dependencies are shaded for deployment and installation.

`mvn clean package ...` is also working.

You should get a file similar to `Hadoop-OBS\target\hadoop-otc-3.1.1-01.jar`.

### Overview

OBS service implements the HDFS protocol of Hadoop, which can replace the HDFS service of Hadoop system in big data scenario, and realize the big data ecology of Spark, MapReduce, Hive, HBase, etc. and OBS service. It provides "data lake" storage for big data computing.

### Limitations

The following HDFS tokens are not supported:

- Lease
- Symbolic link operations
- Proxy users
- File concat
- File checksum
- File replication factor
- Extended Attributes(XAttrs) operations
- Snapshot operations
- Storage policy
- Quota
- POSIX ACL
- Delegation token operations

### Log Level

This can be changed in a file similar to `/opt/hadoop-2.8.3/etc/hadoop/log4j.properties`:

```ini
log4j.logger.com.obs=ERROR
```

### Install

Take Hadoop 2.8.3 as an example, higher versions are similar.

1. Download the Hadoop distribution, for example: `hadoop-2.8.3.tar.gz`,  
   and extract it to the `/opt/hadoop-2.8.3` directory

2. Add configuration to the `/etc/profile` file

   ```shell
   export HADOOP_HOME=/opt/hadoop-2.8.3
   export PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
   ```

3. Install Hadoop-OBS
   Copy the files you got in Build (`hadoop-otc-2.8.3-01.jar`) to the  
   `/opt/hadoop-2.8.3/share/hadoop/tools/lib` and  
   `/opt/hadoop-2.8.3/share/hadoop/common/lib` directories.

4. Configuring Hadoop
   Modify `/opt/hadoop-2.8.3/etc/hadoop/core-site.xml` to add OBS-related configuration information.

   ```xml
   <property>
   <name>fs.obs.impl</name>
   <value>org.apache.hadoop.fs.obs.OBSFileSystem</value>
   </property>
   <property>
   <name>fs.AbstractFileSystem.obs.impl</name>
   <value>org.apache.hadoop.fs.obs.OBS</value>
   </property>
   <property>
   <name>fs.obs.access.key</name>
   <value>xxx</value>
   <description>OTC access key</description>
   </property>
   <property>
   <name>fs.obs.secret.key</name>
   <value>xxx</value>
   <description>OTC secret key</description>
   </property>
   <property>
   <name>fs.obs.endpoint</name>
   <value>xxx</value>
   <description>OTC endpoint</description>
   </property>
   ```

5. Verify
   You can verify this both from the command line and through the MR program.  
   Examples are as follows:

   ```shell
   hadoop fs -ls obs://obs-bucket/
   ```

   You get:

   ```
   -rw-rw-rw- 1 root root 1087 2018-06-11 07:49 obs://obs-bucket/test1
   -rw-rw-rw- 1 root root 1087 2018-06-11 07:49 obs://obs-bucket/test2
   ```

   MR:

   ```
   Hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.8.3.jar wordcount obs://example-bucket/input/test.txt obs://obs-bucket/output
   ```

### Configuration

| Item                                    | Default                                                  | Required | Description                                                                                                                                                                                                                                                                                                                                     |
| --------------------------------------- | -------------------------------------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `fs.obs.impl`                           | org.apache.hadoop.fs.obs.OBSFileSystem                   | Yes      | /                                                                                                                                                                                                                                                                                                                                               |
| `fs.AbstractFileSystem.obs.impl`        | org.apache.hadoop.fs.obs.OBSorg.apache.hadoop.fs.obs.OBS | Yes      | /                                                                                                                                                                                                                                                                                                                                               |
| `fs.obs.endpoint`                       | /                                                        | Yes      | Endpoint of OBS                                                                                                                                                                                                                                                                                                                                 |
| `fs.obs.access.key`                     | /                                                        | Yes      | AK, need to have access to the corresponding bucket of OBS                                                                                                                                                                                                                                                                                      |
| `fs.obs.secret.key`                     | /                                                        | Yes      | SK, need to have access to the corresponding bucket of OBS                                                                                                                                                                                                                                                                                      |
| `fs.obs.session.token`                  | /                                                        | No       | SecurityToken, need to have access to the corresponding bucket of the OBS. Required when using temporary AK/SK                                                                                                                                                                                                                                  |
| `fs.obs.security.provider`              | /                                                        | No       | Class implementing the com.obs.services.IObsCredentialsProvider interface for obtaining credentials to access OBS.                                                                                                                                                                                                                              |
| `fs.obs.connection.ssl.enabled`         | FALSE                                                    | No       | Whether to access OBS via HTTPS.                                                                                                                                                                                                                                                                                                                |
| `fs.obs.threads.keepalivetime`          | 60                                                       | No       | Controls the read/write thread pool parameter keepAliveTime.                                                                                                                                                                                                                                                                                    |
| `fs.obs.threads.max`                    | 20                                                       | No       | Controls the read and write thread pool parameters corePoolSize and maximumPoolSize                                                                                                                                                                                                                                                             |
| `fs.obs.max.total.tasks`                | 20                                                       | No       | Controls the capacity of the read/write thread pool parameter BlockingQueue, which is equal to fs.obs.threads.max + fs.obs.max.total.tasks                                                                                                                                                                                                      |
| `fs.obs.multipart.size`                 | 104857600                                                | No       | Write related configuration with multiple upload sizes.                                                                                                                                                                                                                                                                                         |
| `fs.obs.fast.upload.buffer`             | disk                                                     | No       | Write-related configuration, all data is cached before being written to OBS and then uploaded to OBS, this parameter is used to set the caching method and takes values in the range of: disk: cache on disk; array: cached in the JVM heap memory; bytebuffer: cached outside the JVM heap.                                                    |
| `fs.obs.buffer.dir`                     | ${hadoop.tmp.dir}                                        | No       | Write related configuration, cache directory when fs.obs.fast.upload.buffer is disk, supports multiple directories and is comma separated.                                                                                                                                                                                                      |
| `fs.obs.bufferdir.verify.enable`        | FALSE                                                    | No       | Write-related configuration, whether to verify that the cache directory exists and has write access when fs.obs.fast.upload.buffer is disk.                                                                                                                                                                                                     |
| `fs.obs.fast.upload.active.blocks`      | 4                                                        | No       | Write the relevant configuration for the maximum number of caches that can be used per stream operation (the maximum number of threaded tasks that can be submitted via the multipart upload thread pool), thus limiting the maximum cache space fs.obs.fast.upload.active.blocks\*fs.obs.multipart.size that can be used per stream operation. |
| `fs.obs.fast.upload.array.first.buffer` | 1048576                                                  | No       | Write related configuration, when fs.obs.fast.upload.buffer is array, this parameter controls the JVM in-heap cache initialization size                                                                                                                                                                                                         |
| `fs.obs.readahead.range`                | 1048576                                                  | No       | Write related configuration, pre-read fragment size.                                                                                                                                                                                                                                                                                            |
| `fs.obs.multiobjectdelete.enable`       | TRUE                                                     | No       | Deletion-related configuration, whether to start bulk deletion when deleting directories.                                                                                                                                                                                                                                                       |
| `fs.obs.delete.threads.max`             | 20                                                       | No       | Deletion-related configuration, Control the thread pool parameters maximumPoolSize and corePoolSize                                                                                                                                                                                                                                             |
| `fs.obs.multiobjectdelete.maximum`      | 1000                                                     | No       | Delete-related configuration, the maximum number of objects that can be deleted in a single OBS bulk delete request supported in a bulk delete, with a maximum value of 1000.                                                                                                                                                                   |
| `fs.obs.multiobjectdelete.threshold`    | 3                                                        | No       | Delete-related configuration, bulk delete will not be started when the number of objects is less than the value of this parameter.                                                                                                                                                                                                              |
| `fs.obs.list.threads.core`              | 30                                                       | No       | List-related configuration, controlling the thread pool parameter corePoolSize                                                                                                                                                                                                                                                                  |
| `fs.obs.list.threads.max`               | 60                                                       | No       | List-related configuration, controlling the thread pool parameter maximumPoolSize                                                                                                                                                                                                                                                               |
| `fs.obs.list.workqueue.capacity`        | 1024                                                     | No       | List-related configuration, controlling the capacity of the thread pool parameter BlockingQueue                                                                                                                                                                                                                                                 |
| `fs.obs.list.parallel.factor`           | 30                                                       | No       | List-related configuration to control concurrency factor parameters.                                                                                                                                                                                                                                                                            |
| `fs.obs.paging.maximum`                 | 1000                                                     | No       | List-related configuration, the maximum number of objects to be returned in a single OBS List request, with a maximum value of 1000.                                                                                                                                                                                                            |
| `fs.obs.copy.threads.max`               | 40                                                       | No       | Object bucket rename related configuration, object bucket rename directory when copy thread pool configuration parameter maximumPoolSize, the value of corePoolSize half of this parameter, BlockingQueue capacity of 1024.                                                                                                                     |
| `fs.obs.copypart.size`                  | 104857600                                                | No       | Object bucket rename related configuration, single object copy when the size of the object exceeds the value of this parameter then multi-segment copy, and segment size for this parameter value; otherwise, simple copy.                                                                                                                      |
| `fs.obs.copypart.threads.max`           | 5368709120                                               | No       | Object bucket rename related configuration, a single object copy if a multi-segment copy, multi-segment copy thread pool configuration parameter maximumPoolSize, the value of corePoolSize half of this parameter, BlockingQueue capacity of 1024.                                                                                             |
| `fs.obs.getcanonicalservicename.enable` | FALSE                                                    | No       | Controls the return value of the getCanonicalServiceName() interface. TRUE: obs://bucketname; FALSE: null                                                                                                                                                                                                                                       |
| `fs.obs.multipart.purge`                | FALSE                                                    | No       | Whether to clear the bucket of multi-segment upload tasks when initializing the OBSFilesystem.                                                                                                                                                                                                                                                  |
| `fs.obs.multipart.purge.age`            | 86400                                                    | No       | Initialize the OBSFilesystem to clean up the bucket how long ago the multi-segment upload task was.                                                                                                                                                                                                                                             |
| `fs.obs.trash.enable`                   | FALSE                                                    | No       | Whether to enable garbage collection.                                                                                                                                                                                                                                                                                                           |
| `fs.obs.trash.dir`                      | /                                                        | No       | Garbage collection directory.                                                                                                                                                                                                                                                                                                                   |
| `fs.obs.block.size`                     | 134217728                                                | No       | Block size.                                                                                                                                                                                                                                                                                                                                     |
