package com.ibm.watson.indexwriter.discovery;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.nutch.indexer.NutchDocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

public class DiscoNutchDocTest {
    private static final String ID = "id";
    private static final String URL = "www.example.com";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void canExtractIdOutOfNutchDocument() throws NoSuchAlgorithmException {
        NutchDocument doc = createNutchDocument();
        DiscoNutchDoc discoNutchDoc = new DiscoNutchDoc.Builder(doc).build();
        String expectedId = discoNutchDoc.hashUrl(URL);
        String actualId = discoNutchDoc.extractId();
        assertEquals(actualId, expectedId);
    }

    @Test
    public void extractsSameIdOutForNutchDocumentsWithSameFieldId() {
        NutchDocument doc1 = createNutchDocument();
        DiscoNutchDoc discoNutchDoc1 = new DiscoNutchDoc.Builder(doc1).build();

        NutchDocument doc2 = createNutchDocument();
        DiscoNutchDoc discoNutchDoc2 = new DiscoNutchDoc.Builder(doc2).build();

        String doc1Id = discoNutchDoc1.extractId();
        String doc2Id = discoNutchDoc2.extractId();
        assertEquals(doc1Id, doc2Id);
    }

    @Test
    public void throwsNullExceptionWhenNutchDocumentIsNull() {
        NutchDocument doc = null;
        exception.expect(IllegalArgumentException.class);
        new DiscoNutchDoc.Builder(doc);
    }

    @Test
    public void convertsNutchDocumentToJsonStream() throws IOException {
        String content = "Hello";
        String textField = "text";
        Map<String, String> docValuesMap = ImmutableMap.of("id", URL, textField, content);
        String expectedJsonString = new Gson().toJson(docValuesMap);

        NutchDocument doc = createNutchDocument();
        // NutchDocument content field is replaced with text field.
        // This is done intentionally for Discovery to enrich documents by default.
        doc.add("content", content);
        DiscoNutchDoc discoNutchDoc = new DiscoNutchDoc.Builder(doc).build();

        InputStream actualInputStream = discoNutchDoc.convertNutchDocToJsonStream();
        String actualJsonString = convertJsonStreamToString(actualInputStream);
        assertEquals(actualJsonString, expectedJsonString);
    }

    private String convertJsonStreamToString(InputStream jsonStream) throws IOException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(jsonStream, "UTF-8"));
        String inputStr;
        StringBuilder responseStrBuilder = new StringBuilder();
        while ((inputStr = streamReader.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }
        return responseStrBuilder.toString();

    }

    private NutchDocument createNutchDocument() {
        NutchDocument doc = new NutchDocument();
        doc.add(ID, URL);
        return doc;
    }
}
