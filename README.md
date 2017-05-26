# IBM Watson Discovery Service IndexWriter plugin for Apache Nutch

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
Edit `conf/hbase/hbase-site.xml` file to point to the right folder where hbase/zookeeper data will be saved.

You can then use the built-in gradle task to setup Hbase.

```bash
./gradlew setupHbase
```
This will download hbase-0.94.27 in build directory, configure and start up hbase service.

Setting up Nutch
----------------

Download and extract nutch-2.3 in the build directory - 

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

That's it. Everything is now setup to crawl websites.

Adding new Domains to crawl with Nutch
--------------------------------------

1. Edit the textfile `seed/urls.txt` containing a list of seed URLs.

  ```bash
  $ mkdir seed
  $ echo "https://en.wikipedia.org/wiki/Apache_Nutch" >> seed/urls.txt
  ```
  
2. inject them into Nutch by giving a file URL (!)

  ```bash
  $buildDir/nutch-release-2.3/runtime/local/bin/nutch inject file:///path/to/seed/
  ```

Actual Crawling Procedure
-------------------------

1. Generate a new set of URLs to fetch. This is is based on both the injected URLs as well as outdated URLs in the Nutch crawl db.

  ```bash
  $buildDir/nutch-release-2.3/runtime/local/bin/nutch generate -topN 10
  ```

  The above command will create job batches for 10 URLs. You can also increase/decrease this number.

2. Fetch the URLs. We are not clustering, so we can simply fetch all batches:

  ```bash
  $buildDir/nutch-release-2.3/runtime/local/bin/nutch fetch -all
  ```

3. Now we parse all fetched pages:

  ```bash
  $buildDir/nutch-release-2.3/runtime/local/bin/nutch parse -all
  ```

4. Last step: Update Nutch's internal database:

  ```bash
  $buildDir/nutch-release-2.3/runtime/local/bin/nutch updatedb -all
  ```

On the first run, this will only crawl the injected URLs. The procedure above is supposed to be repeated regulargy to keep the index up to date.

Putting Documents into Discovery
------------------------------------

```bash
$buildDir/nutch-release-2.3/runtime/local/bin/nutch index -all
```

## Contributing

Look at the Nutch source in `src/plugin/indexer-*` for examples of other indexer plugins.
