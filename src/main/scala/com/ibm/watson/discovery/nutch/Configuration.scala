package com.ibm.watson.discovery.nutch

import com.ibm.watson.developer_cloud.discovery.v1.model.document.{CreateDocumentRequest, UpdateDocumentRequest}
import org.apache.hadoop.conf.Configuration

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