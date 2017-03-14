# IBM Watson Discovery Service IndexWriter plugin for Apache Nutch

Status: **Inactive Development**

## Introduction

This plugin enables Apache Nutch users to upload documents to Watson Discovery
Service natively as an index store, just like pushing to Solr or ElasticSearch.

Use Nutch's help feature to see the configuration properties beginning with
`watson.discovery` for what information is needed to push to Discovery Service.

## Building

    ./gradlew

## Contributing

Look at the Nutch source in `src/plugin/indexer-*` for examples of other indexer plugins. **This plugin still needs an assemble task that builds a plugin artifact that Nutch can recognize.** 
