#!/bin/bash


./gradlew

echo "<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>file://$HOME/hbase-data</value>
  </property>
  <property>
    <name>hbase.zookeeper.property.dataDir</name>
    <value>$HOME/zk-data</value>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>false</value>
  </property>
</configuration>" > conf/hbase/hbase-site.xml

./gradlew setupHbase

./gradlew setupNutch

./gradlew buildPlugin

