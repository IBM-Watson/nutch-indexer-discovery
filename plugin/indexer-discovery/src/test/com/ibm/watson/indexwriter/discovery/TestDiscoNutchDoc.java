/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

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

public class TestDiscoNutchDoc {
    private static final String ID = "id";
    private static final String URL = "www.example.com";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void canExtractIdOutOfNutchDocument() throws NoSuchAlgorithmException {
        NutchDocument doc = createNutchDocument();
        DiscoNutchDoc discoNutchDoc = new DiscoNutchDoc.Builder(doc).build();
        String expectedId = discoNutchDoc.hashUrl(URL);
        String actualId = discoNutchDoc.getId();
        assertEquals(actualId, expectedId);
    }

    @Test
    public void extractsSameIdOutForNutchDocumentsWithSameFieldId() {
        NutchDocument doc1 = createNutchDocument();
        DiscoNutchDoc discoNutchDoc1 = new DiscoNutchDoc.Builder(doc1).build();

        NutchDocument doc2 = createNutchDocument();
        DiscoNutchDoc discoNutchDoc2 = new DiscoNutchDoc.Builder(doc2).build();

        String doc1Id = discoNutchDoc1.getId();
        String doc2Id = discoNutchDoc2.getId();
        assertEquals(doc1Id, doc2Id);
    }

    @Test
    public void throwsNullExceptionWhenNutchDocumentIsNull() {
        NutchDocument doc = null;
        exception.expect(IllegalArgumentException.class);
        new DiscoNutchDoc.Builder(doc);
    }

    @Test
    public void convertsNutchDocumentToJsonStream() throws IOException, NoSuchAlgorithmException {
        String content = "Hello";
        String textField = "text";

        NutchDocument doc = createNutchDocument();
        // NutchDocument content field is replaced with text field.
        // This is done intentionally for Discovery to enrich documents by default.
        doc.add("content", content);
        DiscoNutchDoc discoNutchDoc = new DiscoNutchDoc.Builder(doc).build();
        String hashedId = discoNutchDoc.hashUrl(URL);
        Map<String, String> docValuesMap = ImmutableMap.of("id", hashedId, textField, content);
        String expectedJsonString = new Gson().toJson(docValuesMap);

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
