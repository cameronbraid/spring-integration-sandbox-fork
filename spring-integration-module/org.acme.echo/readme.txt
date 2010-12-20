Currently assumed conventions:

Channels
========
	start 	- <module-name>StartChannel (e.g., echoStartChannel)
	end 	- <module-name>EndChannel (e.g., echoEndChannel)

The existence of the 'endChannel' signifies that this module is by-directional - outbound-gateway. 
Otherwise if only 'startChannel' is present then it would be an 'outbound-adapter'.	

Namespace artifacts
===================
	The packaging structure for the namespace artifacts is the same as in Spring, which means the following 
	packages will be generated with the following classes with constant names:
	
		org.acme.echo.module - contains endpoint specific implementation such as 
						ModuleGatewayMessageHandler, 
						ModuleGatewayRequestExchanger etc.

		org.acme.echo.module.config - contains parser and namespace handler implementations.
	
	META-INF - will contain spring.handlers and spring.schemas files
	
	META-INF/spring/module - will contain the module configuration file which is a single file from 
							 which the module is going to be bootstrapped. This file will typically 
							 contain only an 'import' to include the actual config where module 
							 integration flow are being configured 
							 (e.g., <import resource="classpath:META-INF/spring/integration/*.xml"/> ).
							 We need that to support non-roo based existing projects where the 
							 actual integration configurations might not be following the 
							 META-INF/spring/integration structure
							 
The actual application config
=============================
	Following spring conventions they are located in META-INF/spring/integration directory							


					




