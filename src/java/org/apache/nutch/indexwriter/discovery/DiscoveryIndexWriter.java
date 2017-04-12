
package org.apache.nutch.indexwriter.discovery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexWriter;
import org.apache.nutch.indexer.NutchDocument;
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
 */
public class DiscoveryIndexWriter implements IndexWriter {
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
        port = job.getInt(DiscoveryConstants.PORT, 80);
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
        String id = (String) doc.getFieldValue("id");
        String type = doc.getDocumentMeta().get("type");
        if (type == null)
            type = "doc";

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
                    Thread.sleep(500);
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
        if (StringUtils.isBlank(value)) {
            String message = "Missing " + value + " Should be set in nutch-site.xml";
            message += "\n" + describe();
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }
}
