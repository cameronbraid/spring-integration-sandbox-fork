Spring Integration Strict Ordering Prototype 
--------------------------------------------
This demo is set up as a distributed SI application and requires running the following as separate processes:

- Client - Configures the input parameters, starts an embedded Active MQ broker and generates and sends input messages over JMS.
The client can be re-run multiple times, but changing parameters requires a restart. 

- DispatcherMain - Runs the Dispatcher in a single threaded JMSListenerContainer. The Dispatcher receives input messages and 
enforces strict ordering of message processing.

- ServerMain - Each instance runs a multi-threaded JMSListenerContainer. Each instance receives the same messages. Each is processed
in a separate thread with a random delay to change the processing order. Messages are written to a file in the order processed. Files
are created in the directory configured as System.getProperty("java.io.tmpdir").  The processing order should be different in each
instance, and different from the input order. However, any messages with the same entity key must be processed in the order received.

The prototype uses a distributed lock implemented with GemFire. Dependencies include:
> Spring Gemfire
> Spring Integration Gemfire
> Spring Integration
> Active MQ

It's probably easiest to import this project as an existing Maven project into Eclipse and create Run Configurations to launch 
a Run Configuration for each process. Alternately, you can run these from maven using profiles, although I haven't figured out how
to run the verification test this way while excluding it from the main build. 

1. Start DispatcherMain
Launch DispatcherMain from Eclipse or open a command shell in the project directory
> mvn test -Pdispatcher

2. Start 3 server instances
ServerMain uses a command line argument to resolve its configuration. The value can be server1, server2, or server3.  
Start one instance each. Create a 3 Eclipse Run Configurations, for ServerMain and assign the corresponding argument to each, 
or open three command shells:
>mvn test -Pserver -DserverName=<serverName> in each.

3. Start the Client. Two command line arguments are required. The destination queue name and the number of servers. e.g., 

Client queue.ordered 3, or

>mvn test -Pclient -Dqueue=<queue> -DnumServers=<numServers>  

NOTE:You can run with queue.unordered to bypass the Dispatcher and observe messages being shuffled. Changing the number of
servers will require a configuration change. 

4. Run ServerTest as a Junit test. (Doesn't currently run from maven, unless you remove the <exclusions> in the surefire
plugin configuration).    


