package com.ibm.watson.indexwriter.discovery;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.nutch.indexer.NutchDocument;

import com.google.gson.Gson;

public class DiscoNutchDoc {
    private static final String ID = "id";
    private static final String CONTENT_FIELD = "content";
    private static final String TEXT_FIELD = "text";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String HEX = "%064x";
    private String docId;
    private InputStream documentStream;
    private NutchDocument doc;

    private DiscoNutchDoc(Builder builder) {
        this.doc = builder.doc;
    }

    public static class Builder {
        private final NutchDocument doc;

        public Builder(NutchDocument doc) {
            checkNutchDocumentFornull(doc);
            this.doc = doc;
        }

        public DiscoNutchDoc build() {
            return new DiscoNutchDoc(this);
        }
    }

    public String extractId() {
        if (this.docId != null) {
            return docId;
        }
        String docUrl = doc.getFieldValue(ID);
        try {
            docId = hashUrl(docUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return docId;
    }

    public InputStream convertNutchDocToJsonStream() {
        if (this.documentStream != null) {
            return documentStream;
        }
        Map<String, Object> documentValuesMap = mapFieldNameToValue(doc);
        String documentJson = new Gson().toJson(documentValuesMap);
        return new ByteArrayInputStream(documentJson.getBytes());
    }

    protected static String hashUrl(String url) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
        return String.format(HEX, new java.math.BigInteger(1, hash));
    }

    private static Map<String, Object> mapFieldNameToValue(NutchDocument doc) {
        Map<String, Object> documentValuesMap = new HashMap<String, Object>();
        for (String fieldName : doc.getFieldNames()) {
            if (fieldName.equals(CONTENT_FIELD)) {
                // NutchDocument content field is replaced with text field.
                // This is done intentionally for Discovery to enrich documents by default.
                documentValuesMap.put(TEXT_FIELD, doc.getFieldValue(fieldName));
                continue;
            }
            if (doc.getFieldValue(fieldName) != null) {
                documentValuesMap.put(fieldName, doc.getFieldValue(fieldName));
            }
        }
        return documentValuesMap;
    }

    private static void checkNutchDocumentFornull(NutchDocument doc) {
        if (doc == null || doc.getFieldValue(ID) == null) {
            throw new IllegalArgumentException("Nutch document and its field id cannot be null.");
        }
    }
}
