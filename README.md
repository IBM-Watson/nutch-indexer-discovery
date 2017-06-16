# IBM Watson Discovery Service IndexWriter plugin for Apache Nutch

[![Build Status](https://travis.ibm.com/watson-crawler/nutch-indexer-discovery.svg?token=xxVzTKgArtoDziLUGyrh&branch=master)](https://travis.ibm.com/watson-crawler/nutch-indexer-discovery/)

Requirements
------------

* OpenJDK 8

Set up Gradle wrapper
----------------
```bash
./gradlew
```

Setting up HBase
----------------
Edit `conf/hbase/hbase-site.xml`
Please make sure you specify a valid hbase directory path and a valid zookeeper data directory path.
```xml
<property>
    <name>hbase.rootdir</name>
    <value>file:///home/USER_NAME/hbase-data</value>
</property>
<property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>/home/USER_NAME/zookeeper-data</value>
</property>
```
This is where hbase data and zookeeper data will be stored.
You can then use the built-in gradle task to setup Hbase.

```bash
./gradlew setupHbase
```

This will download hbase-0.98.8-hadoop2 in build directory.

Now, startup hbase service by going into the hbase directory - $projectDir/build/hbase-0.98.8-hadoop2/bin/
and run -

```bash
./start-hbase.sh
```

Setting up Nutch
----------------

Download and extract apache-nutch-2.3.1 in the build directory -

```bash
./gradlew setupNutch
```

Then edit `conf/nutch-discovery/nutch-site.xml` with discovery credentials,

```xml
  <property>
    <name>discovery.endpoint</name>
    <value></value>
  </property>
  <property>
    <name>discovery.username</name>
    <value></value>
  </property>
  <property>
    <name>discovery.password</name>
    <value></value>
  </property>
  <property>
    <name>discovery.configuration.id</name>
    <value></value>
  </property>
  <property>
    <name>discovery.environment.id</name>
    <value></value>
  </property>
  <property>
    <name>discovery.api.version</name>
    <value></value>
  </property>
  <property>
    <name>discovery.collection.id</name>
    <value></value>
  </property>
```

To build the plugin, run

```bash
./gradlew buildPlugin
```

This will take about 4-5 minutes to complete.
That's it. Everything is now setup to crawl websites.

Adding new Domains to crawl with Nutch
--------------------------------------

1. Edit the textfile `seed/urls.txt` containing a list of seed URLs.

  ```bash
  $ mkdir seed
  $ echo "https://en.wikipedia.org/wiki/Apache_Nutch" >> $projectDir/seed/urls.txt
  ```

2. inject them into Nutch by giving a file URL (!)

  ```bash
  $projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch inject file:///path/to/seed/
  ```

Actual Crawling Procedure
-------------------------

1. Generate a new set of URLs to fetch. This is is based on both the injected URLs as well as outdated URLs in the Nutch crawl db.

  ```bash
  $projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch generate -topN 5
  ```

  The above command will create job batches for 5 URLs. You can also increase/decrease this number.

2. Fetch the URLs. We are not clustering, so we can simply fetch all batches:

  ```bash
  $projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch fetch -all
  ```

3. Now we parse all fetched pages:

  ```bash
  $projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch parse -all
  ```

4. Last step: Update Nutch's internal database:

  ```bash
  $projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch updatedb -all
  ```

On the first run, this will only crawl the injected URLs. The procedure above is supposed to be repeated regulargy to keep the index up to date.

Putting Documents into Discovery
------------------------------------

```bash
$projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch index -all
```

## Contributing

Look at the Nutch source in `src/plugin/indexer-*` for examples of other indexer plugins.
