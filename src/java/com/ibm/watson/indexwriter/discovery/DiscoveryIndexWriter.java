
package com.ibm.watson.indexwriter.discovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexWriter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexwriter.discovery.DiscoveryConstants;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.discovery.v1.model.document.CreateDocumentRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.document.CreateDocumentResponse;
import com.ibm.watson.developer_cloud.discovery.v1.model.document.Document;
import com.ibm.watson.developer_cloud.discovery.v1.model.document.GetDocumentRequest;
import com.ibm.watson.developer_cloud.discovery.v1.model.document.GetDocumentResponse;
import com.ibm.watson.developer_cloud.http.HttpMediaType;

/**
 * Sends NutchDocuments to a configured Discovery collection.
 */
public class DiscoveryIndexWriter implements IndexWriter {
    private static final int DEFAULT_SLEEP_MILLIS = 500;
    public static Logger LOG = LoggerFactory.getLogger(DiscoveryIndexWriter.class);
    private String defaultIndex;

    private Configuration config;
    private Discovery discoveryClient;
    private String endpoint = null;
    private Integer port = null;
    private String clusterName = null;
    private String apiVersion = null;
    private String username = null;
    private String password = null;
    private String collectionId = null;
    private String environmentId = null;

    @Override
    public void open(Configuration job) throws IOException {
        endpoint = job.get(DiscoveryConstants.ENDPOINT);
        username = job.get(DiscoveryConstants.USERNAME);
        password = job.get(DiscoveryConstants.PASSWORD);
        collectionId = job.get(DiscoveryConstants.COLLECTION_ID);
        environmentId = job.get(DiscoveryConstants.ENVIRONMENT_ID);
        apiVersion = job.get(DiscoveryConstants.API_VERSION);
        discoveryClient = new Discovery(apiVersion);
        discoveryClient.setEndPoint(endpoint);
        discoveryClient.setUsernameAndPassword(username, password);
    }

    @Override
    public void write(NutchDocument doc) throws IOException {
        // Add each field of this doc to the index source
        Map<String, Object> documentValuesMap = new HashMap<String, Object>();
        for (String fieldName : doc.getFieldNames()) {
            if (doc.getFieldValue(fieldName) != null) {
                documentValuesMap.put(fieldName, doc.getFieldValue(fieldName));
            }
        }
        Gson gson = new Gson();
        String documentJson = gson.toJson(documentValuesMap);
        InputStream documentStream = new ByteArrayInputStream(documentJson.getBytes());
        indexDocumentToDiscovery(documentStream);

    }

    private void indexDocumentToDiscovery(InputStream documentStream) {
        CreateDocumentRequest.Builder createDocumentBuilder = new CreateDocumentRequest.Builder(environmentId,
                collectionId);
        createDocumentBuilder.inputStream(documentStream, HttpMediaType.APPLICATION_JSON);
        CreateDocumentResponse createDocumentResponse = discoveryClient.createDocument(createDocumentBuilder.build())
                .execute();
        String documentId = createDocumentResponse.getDocumentId();
        LOG.debug("Created a document ID: " + documentId);

        // wait for document to be ready
        Log.debug("Waiting for document to be ready...");
        boolean documentReady = false;
        while (!documentReady) {
            GetDocumentRequest getDocumentRequest = new GetDocumentRequest.Builder(environmentId, collectionId,
                    documentId).build();
            GetDocumentResponse getDocumentResponse = discoveryClient.getDocument(getDocumentRequest).execute();
            documentReady = !getDocumentResponse.getStatus().equals(Document.Status.PROCESSING);
            try {
                if (!documentReady) {
                    Thread.sleep(DEFAULT_SLEEP_MILLIS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted");
            }
        }
        LOG.debug("Document Ready!");
    }

    @Override
    public void delete(String key) throws IOException {
        // do nothing
    }

    @Override
    public void update(NutchDocument doc) throws IOException {
        // do nothing
    }

    @Override
    public void commit() throws IOException {
        // do nothing
    }

    @Override
    public void close() throws IOException {
        commit();

    }

    @Override
    public String describe() {
        StringBuffer sb = new StringBuffer("DiscoveryIndexWriter\n");
        sb.append("\t").append(DiscoveryConstants.ENDPOINT)
                .append(" : Discovery endpoint url\n");
        sb.append("\t").append(DiscoveryConstants.USERNAME)
                .append(" : Discovery Username  \n");
        sb.append("\t").append(DiscoveryConstants.PASSWORD)
                .append(" : Discovery password\n");
        sb.append("\t").append(DiscoveryConstants.ENVIRONMENT_ID)
                .append(" : Discovery Environment id\n");
        sb.append("\t").append(DiscoveryConstants.COLLECTION_ID)
                .append(" : Discovery Collection id\n");
        sb.append("\t").append(DiscoveryConstants.CONFIGURATION_ID)
                .append(" : Discovery Configuration id\n");
        sb.append("\t").append(DiscoveryConstants.API_VERSION)
                .append(" : Discovery Api version\n");
        return sb.toString();
    }

    @Override
    public void setConf(Configuration conf) {
        config = conf;
        checkNotNull(DiscoveryConstants.USERNAME, conf.get(DiscoveryConstants.USERNAME));
        checkNotNull(DiscoveryConstants.ENDPOINT, conf.get(DiscoveryConstants.ENDPOINT));
        checkNotNull(DiscoveryConstants.PASSWORD, conf.get(DiscoveryConstants.PASSWORD));
        checkNotNull(DiscoveryConstants.ENVIRONMENT_ID, conf.get(DiscoveryConstants.ENVIRONMENT_ID));
        checkNotNull(DiscoveryConstants.COLLECTION_ID, conf.get(DiscoveryConstants.COLLECTION_ID));
        checkNotNull(DiscoveryConstants.CONFIGURATION_ID, conf.get(DiscoveryConstants.CONFIGURATION_ID));
        checkNotNull(DiscoveryConstants.API_VERSION, conf.get(DiscoveryConstants.API_VERSION));
    }

    @Override
    public Configuration getConf() {
        return config;
    }

    private void checkNotNull(String key, String value) {
        if (value == null || StringUtils.isBlank(value)) {
            String message = "Missing " + key + " Should be set in nutch-site.xml";
            message += "\n" + describe();
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }
}
