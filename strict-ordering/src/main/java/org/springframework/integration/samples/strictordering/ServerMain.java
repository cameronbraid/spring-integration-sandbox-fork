package org.springframework.integration.samples.strictordering;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerMain {
  public static void main (String[] args){
	    String serverName = null;
	    if (args.length > 0){
	      serverName = args[0];
	    } else {
	    	System.out.println("usage: ServerMain serverName");
	    	System.exit(0);
	    }
		new ClassPathXmlApplicationContext(
				"META-INF/spring/jms-infrastructure-config.xml",
				"META-INF/spring/"+serverName+"-integration-config.xml",
				"META-INF/spring/cache-config.xml"
				);
  }
}
