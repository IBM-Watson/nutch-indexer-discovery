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

public class DiscoNutchUtilsTest {
    private static final String URL = "www.example.com";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void canExtractIdOutOfNutchDocument() throws NoSuchAlgorithmException {
        NutchDocument doc = new NutchDocument();
        doc.add("id", URL);
        String expectedId = DiscoNutchUtils.hashUrl(URL);
        String actualId = DiscoNutchUtils.extractId(doc);
        assertEquals(actualId, expectedId);
    }

    @Test
    public void extractsSameIdOutForNutchDocumentsWithSameFieldId() {
        NutchDocument doc1 = new NutchDocument();
        doc1.add("id", URL);

        NutchDocument doc2 = new NutchDocument();
        doc2.add("id", URL);

        String doc1Id = DiscoNutchUtils.extractId(doc1);
        String doc2Id = DiscoNutchUtils.extractId(doc2);
        assertEquals(doc1Id, doc2Id);
    }

    @Test
    public void extractIdthrowsNullExceptionWhenNutchDocumentIsNull() {
        NutchDocument doc = null;
        exception.expect(NullPointerException.class);
        DiscoNutchUtils.extractId(doc);
    }

    @Test
    public void convertsNutchDocumentToJsonStream() throws IOException {
        String content = "Hello";
        String textField = "text";
        Map<String, String> docValuesMap = ImmutableMap.of("id", URL, textField, content);
        String expectedJsonString = new Gson().toJson(docValuesMap);

        NutchDocument doc = new NutchDocument();
        doc.add("id", URL);
        // NutchDocument content field is replaced with text field.
        // This is done intentionally for Discovery to enrich documents by default.
        doc.add("content", content);

        InputStream actualInputStream = DiscoNutchUtils.convertNutchDocToJsonStream(doc);
        String actualJsonString = convertJsonStreamToString(actualInputStream);
        assertEquals(actualJsonString, expectedJsonString);
    }

    @Test
    public void convertNutchDocToJsonThrowsExceptionWhenDocIsNUll() {
        NutchDocument doc = new NutchDocument();
        exception.expect(NullPointerException.class);
        DiscoNutchUtils.convertNutchDocToJsonStream(doc);
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
}
