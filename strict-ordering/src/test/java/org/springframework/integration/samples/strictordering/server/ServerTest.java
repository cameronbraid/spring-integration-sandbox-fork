package org.springframework.integration.samples.strictordering.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.springframework.integration.samples.strictordering.Client;
import org.springframework.util.StringUtils;

/**
 * Verify the demo output complies with strict ordering and that all messages were processed correctly
 * 
 * @author David Turanski
 *
 */
public class ServerTest {
	private static final int NUM_SERVERS = 3;
	private File file;
	private static Logger logger = Logger.getLogger(ServerTest.class);
    @Test 
    public void test() throws IOException{
    	String tmpdir = System.getProperty("java.io.tmpdir");
		for (int i = 1; i<= NUM_SERVERS; i++){
			 testFile(tmpdir + File.separator + "proccessed-messages-" + i + ".txt");
		}
    	 
     
    }

    private void testFile(String fileName) throws IOException{
    	logger.info("testing output file: " + fileName);
    	file = new File(fileName);
    	assertTrue(file.exists());
    	
    	BufferedReader reader = new BufferedReader(new FileReader(file));
    	String text;
    	Map<String,List<String>> results = new LinkedHashMap<String,List<String>>();
    	int numLines = 0;
    	while ((text = reader.readLine()) != null)
        {
    		numLines++;
            String[] vals = StringUtils.commaDelimitedListToStringArray(text);
            String key = vals[0];
            String seq = vals[1];
            if ( !results.containsKey(key) ){
            	results.put(key,new ArrayList<String>());
            }
            results.get(key).add(seq);
        }

    	// Verify that the file contains the expected number of lines
    	assertEquals("file size is incorrect", Client.MAX_MESSAGES, numLines);
    	
    	// Verify that the messages are unique (key + seq)
    	assertEquals("file size is incorrect", Client.MAX_MESSAGES, numLines);
    	int uniqueMessages = 0;
    	for (List<String> sequence: results.values()){
    		uniqueMessages += sequence.size();
    	}
    	
    	assertEquals("messages are not unique", Client.MAX_MESSAGES, uniqueMessages);
    	
    	verifyStrictOrder(results);
        
    	
    }
	private void verifyStrictOrder(Map<String, List<String>> results) {
	     for (Entry<String,List<String>> entry: results.entrySet()){
	    	int minSequence = 0;
	    	for (String val: entry.getValue()){
	    		int sequence = Integer.valueOf(val);
	    		assertTrue("out of order [" + entry.getKey() + "] sequence [" + sequence + "]", sequence > minSequence);
	    		minSequence = sequence;
	    	}
	     }
		
	}
	
	@After
	public void cleanUp() throws IOException{
		//assertTrue(file.delete());
		//new File(file.getAbsolutePath()).createNewFile();
	}
  
}
