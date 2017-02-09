package com.ibm.watson.discovery.nutch

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

import com.google.gson.{JsonArray, JsonObject}
import com.ibm.watson.developer_cloud.discovery.v1.model.document.{CreateDocumentRequest, CreateDocumentResponse, Document, UpdateDocumentRequest}
import com.ibm.watson.developer_cloud.discovery.v1.{Discovery => DiscoveryClient}
import com.ibm.watson.developer_cloud.http.ServiceCallback
import org.apache.hadoop.conf.Configuration
import org.apache.nutch.indexer.{IndexWriter, NutchDocument}
import org.apache.nutch.metadata.Metadata

import scala.collection.JavaConverters._
import scala.util.Try

object Defaults {
  val ApiVersion = "2017-01-30"
  val Endpoint = "https://gateway.watsonplatform.net/discovery/api"
  val NutchContentType = "application/json"
}
object ConfigProperties {
  private val DiscoPrefix = "watson.discovery."
  val ApiVersion = DiscoPrefix + "api_version"
  val Endpoint = DiscoPrefix + "endpoint"
  val Username = DiscoPrefix + "username"
  val Password = DiscoPrefix + "password"
  val EnvironmentId = DiscoPrefix + "environment_id"
  val CollectionId = DiscoPrefix + "collection_id"
  val ConfigurationId = DiscoPrefix + "configuration_id"

  def toDescription = {
    "DiscoveryServiceIndexWriter\n" + List(
      ApiVersion -> s"The required minimum version of the Discovery Service API, defaulting to ${Defaults.ApiVersion}",
      Endpoint -> s"The URL of the Discovery Service API endpoint, defaulting to ${Defaults.Endpoint}",
      Username -> s"The Discovery Service username",
      Password -> s"The Discovery Service password",
      EnvironmentId -> s"The environment in which the target collection resides",
      CollectionId -> s"The collection in which to store documents",
      ConfigurationId -> s"Override the collection's default configuration (optional)"
    ).foldLeft[StringBuilder](new StringBuilder){ (s, kv) =>
      val (prop, desc) = kv
      s.append(s"\t$prop : $desc\n")
      s
    }.toString()
  }
}

case class DiscoveryServiceConfig(
                                   apiVersion: String = Defaults.ApiVersion,
                                   endpoint: String = Defaults.Endpoint,
                                   username: String, password: String,
                                   environmentId: String, collectionId: String, configurationId: Option[String]) {
  def prepareCreateDocumentRequestBuilder: CreateDocumentRequest.Builder = {
    val req = new CreateDocumentRequest.Builder(environmentId, collectionId)
    configurationId.foreach(req.configurationId)
    req
  }
  def prepareUpdateDocumentRequestBuilderForId(documentId: String): UpdateDocumentRequest.Builder = {
    val req = new UpdateDocumentRequest.Builder(environmentId, collectionId, documentId)
    configurationId.foreach(req.configurationId)
    req
  }
}

object DiscoveryServiceConfig {
  def from(conf: Configuration): DiscoveryServiceConfig = {
    DiscoveryServiceConfig(
      apiVersion = conf.get(ConfigProperties.ApiVersion, Defaults.ApiVersion),
      endpoint = conf.get(ConfigProperties.Endpoint, Defaults.Endpoint),
      username = conf.get(ConfigProperties.Username),
      password = conf.get(ConfigProperties.Password),
      environmentId = conf.get(ConfigProperties.EnvironmentId),
      collectionId = conf.get(ConfigProperties.CollectionId),
      configurationId = Option(conf.get(ConfigProperties.ConfigurationId))
    )
  }
}

object DiscoveryClientBuilder {
  def buildFrom(conf: DiscoveryServiceConfig): DiscoveryClient = {
    val client = new DiscoveryClient(conf.apiVersion)
    client.setEndPoint(conf.endpoint)
    client.setUsernameAndPassword(conf.username, conf.password)
    client
  }
}

object JsonMetadata {
  def from(data: Metadata): JsonObject = {
    data.names.foldLeft(new JsonObject) { (json, name) => json.addProperty(name, data.get(name)); json }
  }
}

object NutchFieldsAsJson {
  def from(doc: NutchDocument): JsonObject = {
    doc.iterator.asScala.foldLeft(new JsonObject) { (json, field) =>
      val fieldName = field.getKey
      val fieldContents = field.getValue.asScala
      json.add(fieldName, toJsonArray(fieldContents))
      json
    }
  }
  private def toJsonArray(collection: Seq[String]): JsonArray = {
    collection.foldLeft(new JsonArray){ (arr, content) => arr.add(content); arr }
  }
}

class DiscoveryServiceIndexWriter extends IndexWriter {

  val Logger = new Logger("discovery-service-index-writer")
  var conf: Option[DiscoveryServiceConfig] = _
  var client: Option[DiscoveryClient] = _

  def withConf(f: DiscoveryServiceConfig => Unit): Unit = {
    withInitializedResource(conf, f)
  }
  def withClient(f: DiscoveryClient => Unit): Unit = {
    withInitializedResource(client, f)
  }
  private def withInitializedResource[Resource](resource: Option[Resource], function: Resource => Unit): Unit = {
    function(resource.getOrElse(throw new DiscoveryServiceUninitialized))
  }

  override def update(doc: NutchDocument): Unit = {
    val id = doc.getFieldValue("id")
    Logger.severe(s"Updating not yet implemented, $id not updated.")
  }

  override def describe(): String = ConfigProperties.toDescription

  override def delete(key: String): Unit = {
    Logger.severe(s"Deletion not yet implemented, $key not deleted.")
  }

  override def write(doc: NutchDocument): Unit = {
    withConf { conf =>
      val req = conf.prepareCreateDocumentRequestBuilder
      req.metadata(JsonMetadata.from(doc.getDocumentMeta))
      val nutchJson = NutchFieldsAsJson.from(doc)
      val stream = new ByteArrayInputStream(nutchJson.toString.getBytes(StandardCharsets.UTF_8))
      req.inputStream(stream, Defaults.NutchContentType)

      withClient { client =>
        val resp = client.createDocument(req.build())
        resp.enqueue(buildCreateDocumentHandler)
      }
    }
  }

  def buildCreateDocumentHandler: ServiceCallback[CreateDocumentResponse] = {
    new ServiceCallback[CreateDocumentResponse] {
      override def onFailure(e: Exception) = {
        Logger.warning(e.getMessage)
      }

      override def onResponse(response: CreateDocumentResponse) = {
        Logger.info(s"Document ${response.getDocumentId} is ${response.getStatus.name} (${response.getStatusDescription})")
        if(!response.getNotices.isEmpty){
          val notices = response.getNotices.asScala.map{ notice =>
            s"\t${notice.getSeverity} in ${notice.getStep} at ${notice.getCreated} : ${notice.getNoticeId}: ${notice.getDescription}"
          }
          Logger.warning(s"Document ${response.getDocumentId} had notices:\n$notices")
        }

      }
    }
  }

  override def close(): Unit = {
    client = None
    Logger.info("Discovery Service IndexWriter shut down.")
  }

  override def commit(): Unit = {
    //pass, no commit operation for WDS
  }

  override def open(jobConf: Configuration): Unit = {
    // also constructs the client
    setConf(jobConf)
  }

  override def getConf: Configuration = ???

  /**
    * This also constructs the `Discovery` client
    * @param configuration provided by Nutch
    */
  override def setConf(configuration: Configuration): Unit = {
    Logger.info(s"Reconfiguring Disco IndexWriter with $conf")

    val stuff = for {
      conf <- Try(DiscoveryServiceConfig.from(configuration))
      client <- Try(DiscoveryClientBuilder.buildFrom(conf))
    } yield (conf, client)

    val pair = stuff.getOrElse { throw new DiscoveryServiceConfigInvalid(stuff.failed.get) }
    conf = Option(pair._1)
    client = Option(pair._2)
  }
}

class DiscoveryServiceConfigInvalid(cause: Throwable) extends RuntimeException(cause)
class DiscoveryServiceUninitialized extends RuntimeException("Discovery Service IndexWriter was not initialized.")