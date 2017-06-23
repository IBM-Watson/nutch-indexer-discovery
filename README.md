# IBM Watson Discovery Service IndexWriter plugin for Apache Nutch

[![Build Status](https://travis.ibm.com/watson-crawler/nutch-indexer-discovery.svg?token=xxVzTKgArtoDziLUGyrh&branch=master)](https://travis.ibm.com/watson-crawler/nutch-indexer-discovery/)

Requirements
------------

* Java (OpenJDK 8/Oracle JDK)
(HBase, Zookeeper, Nutch, Ant, and Gradle are also required, but will be installed for you when you set up the Gradle wrapper.)
* Make sure `JAVA_HOME` environment variable is set.

  On *MAC*, JAVA_HOME will be something like - `/Library/Java/Home/JavaVirtualMachines/jdk1.8.0_66.jdk/Contents/Home`

  On *LINUX*, JAVA_HOME will be something like - `/usr/lib/jvm/java-8-openjdk-amd64`

Set up Gradle wrapper
----------------
```bash
./gradlew
```

Setting up HBase
----------------

You can now use the built-in gradle task to setup Hbase.

```bash
./gradlew setupHbase
```

* This will create directories within project directory to
   store Hbase and Zookeeper data.
* Downloads hbase-0.98.8-hadoop2 in build directory.

Now, startup hbase service by going into the hbase directory: _projectDir_/build/hbase-0.98.8-hadoop2/bin/
and run:

```bash
./start-hbase.sh
```

Setting up Nutch
----------------

Download and extract apache-nutch-2.3.1 in the build directory:

```bash
./gradlew setupNutch
```

Then edit `conf/nutch-discovery/nutch-site.xml` with Discovery credentials. The values for the first three properties (endpoint, username, and password) are provided by the Discovery service. The others are provided by your specific instance of the Discovery service.

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

To build the plugin, run:

```bash
./gradlew buildPlugin
```

This will take about 4-5 minutes to complete. It may hang a bit; please be patient. 
That's it. Everything is now setup to crawl websites.

Adding new Domains to crawl with Nutch
--------------------------------------

1. Edit the text file `seed/urls.txt` containing a list of seed URLs.

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

1. Generate a new set of URLs to fetch. This is based on both the injected URLs as well as outdated URLs in the Nutch crawl db.

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

On the first run, this will only crawl the injected URLs. The procedure above is supposed to be repeated regularly to keep the index up to date.

Putting Documents into Discovery
------------------------------------

```bash
$projectDir/build/apache-nutch-2.3.1/runtime/local/bin/nutch index -all
```

## Contributing

Look at the Nutch source in `src/plugin/indexer-*` for examples of other indexer plugins.
