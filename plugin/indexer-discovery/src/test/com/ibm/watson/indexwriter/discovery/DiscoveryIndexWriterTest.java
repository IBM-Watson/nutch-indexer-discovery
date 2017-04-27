package com.ibm.watson.indexwriter.discovery;

import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.util.NutchConfiguration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DiscoveryIndexWriterTest {
	private DiscoveryIndexWriter discoWriter;
	private Configuration conf;
	private NutchDocument doc;
	private boolean documentCreated;
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setup(){
		documentCreated=false;
		discoWriter = new DiscoveryIndexWriter(){
			@Override
			protected boolean createDocumentInDiscovery(String docId, InputStream documentStream) {
				documentCreated = true;
				return true;
			}
		};
		conf = NutchConfiguration.create();
		setDiscoProperties(conf);
		doc = new NutchDocument();
		doc.add("id", "www.example.com");
	}
	
	@Test
	public void DiscoIndexWriterCanLoadConfig(){
		discoWriter.setConf(conf);
		Configuration indexWriterConfig = discoWriter.getConf();
		assertEquals(conf, indexWriterConfig);
	}
	
	@Test
	public void DiscoIndexWriterWillThrowRuntimeExceptionForIncompleteConfig(){
		Configuration conf = NutchConfiguration.create();
		exception.expect(RuntimeException.class);
		discoWriter.setConf(conf);
	}
	
	@Test
	public void canWriteDocumentToDiscovery() throws IOException{
		discoWriter.setConf(conf);
		discoWriter.open(conf);
		discoWriter.write(doc);
		assertTrue(documentCreated);
	}
	
	@Test
	public void canUpdateDocumentToDiscovery() throws IOException{
		discoWriter.setConf(conf);
		discoWriter.open(conf);
		discoWriter.update(doc);
		assertTrue(documentCreated);
	}
	
	private void setDiscoProperties(Configuration conf){
		conf.set(DiscoveryConstants.USERNAME, "USERNAME");
		conf.set(DiscoveryConstants.PASSWORD, "PASSWORD");
		conf.set(DiscoveryConstants.ENVIRONMENT_ID, "ENVIRONMENT_ID");
		conf.set(DiscoveryConstants.ENDPOINT, "ENDPOINT");
		conf.set(DiscoveryConstants.CONFIGURATION_ID, "CONFIGURGATION_ID");
		conf.set(DiscoveryConstants.COLLECTION_ID, "COLLECTION_ID");
		conf.set(DiscoveryConstants.API_VERSION, "API_VERSION");
	}

}