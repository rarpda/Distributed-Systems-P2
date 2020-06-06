The files have been compiled and are located in:
Source/P2-Actual/out/production/P2-Actual


To run a node class it needs the following parameters in the command line:
java NodeP2.NodeP2 ID ENDPOINT COORDINATOR_ENDPOIT COORDINATOR_ID

For example: java Client.Client 2 localhost:8083 localhost:8085 5
This will run a node with ID 2 in the local host at port 8083 and inialised with coordinator with ID 5 and local host at port 8085.



##################################
To run the client class it  needs the following parameters in the command line:
java Client.Client OPTION OPTION_NUMBER

Here are the available options
java Client.Client SENDER 0
java Client.Client RECEIVER 0
java Client.Client RECEIVER 1
java Client.Client MULTICAST 0


To run the Queue Manager it needs the following parameters in the command line:
java QueueManager.QueueManager 


###########
To run the entire network issue the following commands in separate terminals:
java QueueManager.QueueManager 
java Node.NodeP2 6 localhost:8081 localhost:8085 5
java Node.NodeP2 1 localhost:8080 localhost:8085 5
java Node.NodeP2 4 localhost:8082 localhost:8085 5
java Node.NodeP2 2 localhost:8083 localhost:8085 5
java Node.NodeP2 7 localhost:8084 localhost:8085 5
java Node.NodeP2 5 localhost:8085 localhost:8085 5

#This will trigger a double election.

#Select the client options as wished.
To test CLIENT - SENDER start
java Client.Client SENDER 0
java Client.Client RECEIVER 1

#To check multicast issue this
java Client.Client RECEIVER 0
java Client.Client RECEIVER 1
java Client.Client MULTICAST 0
