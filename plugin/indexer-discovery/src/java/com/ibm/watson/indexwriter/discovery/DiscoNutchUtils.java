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

public class DiscoNutchUtils {
    private static final String ID = "id";

    public static String extractId(NutchDocument doc) {
        checkNutchDocumentFornull(doc);
        String docUrl = doc.getFieldValue(ID);
        String docId = null;
        try {
            docId = hashUrl(docUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return docId;
    }

    public static InputStream convertNutchDocToJsonStream(NutchDocument doc) {
        checkNutchDocumentFornull(doc);
        Map<String, Object> documentValuesMap = new HashMap<String, Object>();
        for (String fieldName : doc.getFieldNames()) {
            if (doc.getFieldValue(fieldName) != null) {
                documentValuesMap.put(fieldName, doc.getFieldValue(fieldName));
            }
        }
        String documentJson = new Gson().toJson(documentValuesMap);
        return new ByteArrayInputStream(documentJson.getBytes());
    }

    protected static String hashUrl(String url) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
        return String.format("%064x", new java.math.BigInteger(1, hash));
    }

    private static void checkNutchDocumentFornull(NutchDocument doc) {
        if (doc == null || doc.getFieldValue(ID) == null) {
            throw new NullPointerException("Nutch document and its field id cannot be null.");
        }
    }

}
