
package com.ibm.watson.indexwriter.discovery;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.indexer.IndexWriter;
import org.apache.nutch.indexer.NutchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int MAX_ATTEMPTS = 1000;
    public static Logger LOG = LoggerFactory.getLogger(DiscoveryIndexWriter.class);

    private Configuration config;
    private Discovery discoveryClient;
    private String endpoint = null;
    private String apiVersion = null;
    private String username = null;
    private String password = null;
    private String collectionId = null;
    private String environmentId = null;
    private String configurationId = null;

    @Override
    public void open(Configuration job) throws IOException {
        endpoint = job.get(DiscoveryConstants.ENDPOINT);
        username = job.get(DiscoveryConstants.USERNAME);
        password = job.get(DiscoveryConstants.PASSWORD);
        collectionId = job.get(DiscoveryConstants.COLLECTION_ID);
        environmentId = job.get(DiscoveryConstants.ENVIRONMENT_ID);
        configurationId = job.get(DiscoveryConstants.CONFIGURATION_ID);
        apiVersion = job.get(DiscoveryConstants.API_VERSION);
        discoveryClient = new Discovery(apiVersion);
        discoveryClient.setEndPoint(endpoint);
        discoveryClient.setUsernameAndPassword(username, password);
    }

    @Override
    public void write(NutchDocument doc) throws IOException {
        String docId = DiscoNutchUtils.extractId(doc);
        InputStream documentStream = DiscoNutchUtils.convertNutchDocToJsonStream(doc);
        boolean documentSent = createDocumentInDiscovery(docId, documentStream);
        if (!documentSent) {
            LOG.info("Document id" + docId + "not created Successfully");
            return;
        }
        LOG.info("Document id" + docId + "created successfully");
    }

    @Override
    public void update(NutchDocument doc) throws IOException {
        write(doc);
    }

    protected boolean createDocumentInDiscovery(String docId, InputStream documentStream) {
        CreateDocumentRequest.Builder createDocumentBuilder = new CreateDocumentRequest.Builder(environmentId,
                collectionId).documentId(docId).configurationId(configurationId);
        createDocumentBuilder.file(documentStream, HttpMediaType.APPLICATION_JSON);
        CreateDocumentResponse createDocumentResponse = discoveryClient.createDocument(createDocumentBuilder.build())
                .execute();
        String documentId = createDocumentResponse.getDocumentId();
        LOG.info("Creating a document ID: " + documentId);

        LOG.info("Waiting for document to be created...");
        return waitForDocumentToBeReady(docId);
    }

    private boolean waitForDocumentToBeReady(String docId) {
        boolean documentReady = false;
        int attempts = 0;
        // Wait for up to 50 seconds per document
        while (!documentReady && attempts <= MAX_ATTEMPTS) {
            GetDocumentRequest getDocumentRequest = new GetDocumentRequest.Builder(environmentId, collectionId,
                    docId).build();
            GetDocumentResponse getDocumentResponse = discoveryClient.getDocument(getDocumentRequest).execute();
            documentReady = !getDocumentResponse.getStatus().equals(Document.Status.PROCESSING);
            try {
                if (!documentReady) {
                    Thread.sleep(DEFAULT_SLEEP_MILLIS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted");
            }
            attempts++;
        }
        return documentReady;
    }

    @Override
    public void delete(String key) throws IOException {
        // do nothing
    }

    @Override
    public void commit() throws IOException {
        // do nothing
    }

    @Override
    public void close() throws IOException {

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
