FROM openjdk:8

RUN mkdir nutch-indexer-discovery

RUN mkdir nutch-indexer-discovery/plugin
COPY plugin /nutch-indexer-discovery/plugin

RUN mkdir nutch-indexer-discovery/gradle
COPY gradle /nutch-indexer-discovery/gradle

RUN mkdir nutch-indexer-discovery/conf
COPY conf /nutch-indexer-discovery/conf

COPY build.gradle /nutch-indexer-discovery
COPY gradlew /nutch-indexer-discovery


RUN mkdir nutch-indexer-discovery/seed
COPY seed /nutch-indexer-discovery/seed

COPY docker_scripts nutch-indexer-discovery/
COPY crawl nutch-indexer-discovery

RUN cd /nutch-indexer-discovery && ./build_nutch_discovery.sh
