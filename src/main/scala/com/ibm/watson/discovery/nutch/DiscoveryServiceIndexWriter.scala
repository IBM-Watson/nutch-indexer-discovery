package com.ibm.watson.discovery.nutch

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

import com.google.gson.{JsonArray, JsonObject}
import com.ibm.watson.developer_cloud.discovery.v1.model.document.CreateDocumentResponse
import com.ibm.watson.developer_cloud.discovery.v1.{Discovery => DiscoveryClient}
import com.ibm.watson.developer_cloud.http.ServiceCallback
import org.apache.hadoop.conf.Configuration
import org.apache.nutch.indexer.{IndexWriter, NutchDocument}
import org.apache.nutch.metadata.Metadata

import scala.collection.JavaConverters._
import scala.util.Try

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
    client
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