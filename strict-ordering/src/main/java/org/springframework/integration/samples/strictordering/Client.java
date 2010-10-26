package org.springframework.integration.samples.strictordering;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
/**
 * 
 * @author David Turanski
 *
 */
public class Client {
	private static final String QUEUE_SERVER_CONTROL = "queue.serverControl";
	private static final String QUEUE_ORDERED = "queue.ordered";

	//Number of messages to send
	public static final int MAX_MESSAGES = 2000;

	//Number of unique IDs generated
	public static final int MAX_IDS = 5000;

	//Probability of a key being repeated
	public static final int REPEAT_KEY_PERCENT = 30;

	//Maximum number of repeats
	public static final int MAX_REPEATS = 5;

	private static Logger logger = Logger.getLogger(Client.class);

	private static Map<String,Integer> sequenceMap;



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2){
			System.out.println("usage: Client queueName numServers, e.g. Client queue.ordered 3");
		}
		//'queue.ordered' or 'queue.unordered' (to bypass the Dispatcher)  - See jms config
		String queueName = args[0]; 

		//the number of servers used in the test
		int numServers = Integer.parseInt(args[1]);



		// Create test parameters to initialize servers
		Map<String,Object> testParameters  = initTestParameters(queueName, numServers);

		ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/jms-infrastructure-config.xml","META-INF/spring/client-config.xml");

		// Re-initialize each server using the server's control queue
		JmsTemplate jmsTemplate = context.getBean("jmsTemplate",JmsTemplate.class);  


		while (true){
			final KeyGenerator keyGen = new KeyGenerator(MAX_IDS, REPEAT_KEY_PERCENT, MAX_REPEATS);
			// Keep track of message count (sequence number) for each key to verify strict ordering
			sequenceMap = new HashMap<String,Integer>(); 

			for (int i=1; i<= numServers; i++){
				jmsTemplate.convertAndSend(QUEUE_SERVER_CONTROL + i,testParameters);
			}

			// Send messages
			logger.info("sending messages to "+ queueName);
			for (int i=0; i < MAX_MESSAGES; i++){
				jmsTemplate.send (queueName, new MessageCreator(){

					@Override
					public Message createMessage(Session session) throws JMSException {
						TextMessage textMessage = session.createTextMessage();
						String payload = "order-"+ keyGen.nextKey();
						textMessage.setText(payload);
						int sequence = getSequence(payload);
						textMessage.setIntProperty("sequence",sequence);
						logger.debug("sending message "+"[" + payload + "] sequence [" + sequence +"]");
						return textMessage;
					}

					// Track the sequence number for each key
					private int getSequence(String payload) {
						if (null == sequenceMap.get(payload)) {
							sequenceMap.put(payload,0);
						}
						int sequence = sequenceMap.get(payload);
						sequenceMap.put(payload,++sequence);
						return sequence;
					}});
			}
			logger.info(MAX_MESSAGES + " messages sent");


			System.out.println("quit to terminate, <ENTER> to run again");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			try {
				String answer = bufferedReader.readLine();
				if (answer.equalsIgnoreCase("quit")){
					System.exit(0);

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<String, Object> initTestParameters(String queueName, int numServers) {
		Map<String,Object> testParameters = new HashMap<String,Object>();
		String tmpdir = System.getProperty("java.io.tmpdir");
		for (int i = 1; i<= numServers; i++){
			testParameters.put("process.filename." + i , tmpdir + File.separator + "proccessed-messages-" + i + ".txt");
		}
		testParameters.put("max.messages",MAX_MESSAGES);
		testParameters.put("strict.ordering",queueName.equals(QUEUE_ORDERED));
		return testParameters;
	}
}

/**
 * 
 * @author David Turanski
 *
 */
class KeyGenerator {	
	private Random random;
	private int currentKey;
	private int numRepeats;
	private boolean repeating;
	private int ids[];
	private int maxIds;
	private int repeatKeyPercent;
	private int maxRepeats; 

	public KeyGenerator(int maxIds,int repeatKeyPercent, int maxRepeats){
		this.maxIds = maxIds;
		this.repeatKeyPercent = repeatKeyPercent;
		this.maxRepeats = maxRepeats;
		currentKey = -1;
		random = new Random();
		generateIds();
	}

	/**
	 * Return one of the generated ids using a weighted random algorithm intended to simulate clusters of messages with the same key
	 * as you would expect to see in a typically strict ordering scenario. The current key value is saved and repeated up to MAX_REPEATS 
	 * times if in a repeating state. The probability of a repeated key is determined by REPEAT_KEY_PERCENT.  
	 * @return
	 */

	public int nextKey() {
		// Roll the dice

		int index = random.nextInt(maxIds);

		if (repeating){
			if (currentKey < 0) {
				currentKey = ids[index];
			}
			repeating = (--numRepeats > 0);
			return currentKey;
		}

		int repeat = random.nextInt(100) + 1;
		if (repeat <= repeatKeyPercent){
			numRepeats = random.nextInt(maxRepeats);
			repeating = true;

		} else {
			numRepeats = 0;
			repeating = false;
		}

		currentKey = ids[index];
		return currentKey;
	}

	/**
	 * Generate random ids.
	 * @return
	 */
	private void generateIds() {
		ids = new int[maxIds]; 
		for (int i=0; i< maxIds; i++){
			ids[i] = random.nextInt(1000000) + 1;
		}

	}
}
