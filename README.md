# IBM Watson Discovery Service IndexWriter plugin for Apache Nutch

Requirements
------------

* OpenJDK 7 & ant
* [Nutch 2.3 RC](https://github.com/apache/nutch/archive/release-2.3.tar.gz) (yes, you **need** 2.3, 2.2 will not work)
* [HBase 0.94.27](https://archive.apache.org/dist/hbase/hbase-0.94.27/hbase-0.94.27.tar.gz)

Extract Nutch and HBase somewhere. We will refer to the Nutch root directory by ``$NUTCH_ROOT`` and the HBase root by ``$HBASE_ROOT``.

Setting up HBase
----------------

1. edit ``$HBASE_ROOT/conf/hbase-site.xml`` and add
  
  ```xml
  <configuration>
    <property>
      <name>hbase.rootdir</name>
      <value>file:///full/path/to/where/the/data/should/be/stored</value>
    </property>
    <property>
      <name>hbase.cluster.distributed</name>
      <value>false</value>
    </property>
  </configuration>
  ```

2. kick off HBase:

  ```bash
  $HBASE_ROOT/bin/start-hbase.sh
  ```

Setting up Nutch
----------------
1. Copy this repo to ``$NUTCH_ROOT/src/plugin/``

2. enable the HBase dependency in ``$NUTCH_ROOT/ivy/ivy.xml`` by uncommenting the line

  ```xml
  <dependency org="org.apache.gora" name="gora-hbase" rev="0.5" conf="*->default" />
  ```

3. configure the HBase adapter by editing the `$NUTCH_ROOT/conf/gora.properties`:

  ```diff
  -#gora.datastore.default=org.apache.gora.mock.store.MockDataStore
  +gora.datastore.default=org.apache.gora.hbase.store.HBaseStore
  ```
4. In order for ant to compile and deploy indexer-discovery plugin you need to edit the $NUTCH_ROOT/src/plugin/build.xml file.  You'll see a number of lines that look like

  ```xml
  <ant dir="[plugin-name]" target="deploy" />
  ```

 Edit this block to add a line for your plugin before the </target> tag.

  ```xml
  <ant dir="indexer-discovery" target="deploy" />
  <ant dir="indexer-discovery" target="clean" />
  ```

5. build Nutch

  ```shell
  $ cd $NUTCH_ROOT
  $ ant clean
  $ ant runtime
  ```

  This *can take a while* and creates ``$NUTCH_ROOT/runtime/local``.

6. configure Nutch by editing ``$NUTCH_ROOT/runtime/local/conf/nutch-site.xml``:

  ```xml
  <configuration>
    <property>
      <name>http.agent.name</name>
      <value>mycrawlername</value> <!-- this can be changed to something more sane if you like -->
    </property>
    <property>
      <name>http.robots.agents</name>
      <value>mycrawlername</value> <!-- this is the robot name we're looking for in robots.txt files -->
    </property>
    <property>
      <name>storage.data.store.class</name>
      <value>org.apache.gora.hbase.store.HBaseStore</value>
    </property>
    <property>
      <name>plugin.includes</name>
      <!-- do **NOT** enable the parse-html plugin, if you want proper HTML parsing. Use something like parse-tika! -->
      <value>protocol-httpclient|urlfilter-regex|parse-(text|tika|js)|index-(basic|anchor)|query-(basic|site|url)|response-(json|xml)|summary-basic|scoring-opic|urlnormalizer-(pass|regex|basic)|indexer-discovery</value>
    </property>
    <property>
      <name>db.ignore.external.links</name>
      <value>true</value> <!-- do not leave the seeded domains (optional) -->
    </property>
    <property>
      <name>discovery.endpoint</name>
      <value>discovery_api_endpoint</value>
    </property>
    <property>
      <name>discovery.username</name>
      <value> </value>
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
      <value>2016-12-15</value>
    </property>
    <property>
      <name>discovery.collection.id</name>
      <value></value>
    </property>
  </configuration>
  ```

7. configure HBase integration by editing ``$NUTCH_ROOT/runtime/local/conf/hbase-site.xml``:

  ```xml
  <configuration>
    <property>
      <name>hbase.rootdir</name>
      <value>file:///full/path/to/where/the/data/should/be/stored</value> <!-- same path as you've given for HBase above -->
    </property>
    <property>
      <name>hbase.cluster.distributed</name>
      <value>false</value>
    </property>
  </configuration>
  ```

That's it. Everything is now setup to crawl websites.

Adding new Domains to crawl with Nutch
--------------------------------------

1. create an empty directory. Add a textfile containing a list of seed URLs.

  ```bash
  $ mkdir seed
  $ echo "https://en.wikipedia.org/wiki/Apache_Nutch" >> seed/urls.txt
  ```
  
2. inject them into Nutch by giving a file URL (!)

  ```bash
  $ $NUTCH_ROOT/runtime/local/bin/nutch inject file:///path/to/seed/
  ```

Actual Crawling Procedure
-------------------------

1. Generate a new set of URLs to fetch. This is is based on both the injected URLs as well as outdated URLs in the Nutch crawl db.

  ```bash
  $ $NUTCH_ROOT/runtime/local/bin/nutch generate -topN 10
  ```

  The above command will create job batches for 10 URLs.

2. Fetch the URLs. We are not clustering, so we can simply fetch all batches:

  ```bash
  $ $NUTCH_ROOT/runtime/local/bin/nutch fetch -all
  ```

3. Now we parse all fetched pages:

  ```bash
  $ $NUTCH_ROOT/runtime/local/bin/nutch parse -all
  ```

4. Last step: Update Nutch's internal database:

  ```bash
  $ $NUTCH_ROOT/runtime/local/bin/nutch updatedb -all
  ```

On the first run, this will only crawl the injected URLs. The procedure above is supposed to be repeated regulargy to keep the index up to date.

Putting Documents into Discovery
------------------------------------

```bash
$ $NUTCH_ROOT/runtime/local/bin/nutch index -all
```

## Building

ant -f ./build-ivy.xml to fetch dependencies

ant

## Contributing

Look at the Nutch source in `src/plugin/indexer-*` for examples of other indexer plugins.
