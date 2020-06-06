package Node;

import QueueManager.QueueManager;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;


/**
 * This class deals requests from the server socket.
 */
class Server {

  /*Variables for server socket*/
  private Coordinator coordinatorClass = null;
  private final int maxPoolSize = 10; /* Maximum number of threads allowed */
  private ServerSocket serverSocket;
  private NodeP2 nodeConnecting;
  private boolean runningElection = false;
  private boolean startedElection = false;
  private int electionCount = 0;
  private int coordinationCount = 0;
  private int clientCount = 0;
  private ConnectionHandler ch;
  private ExecutorService threadPool = null;


  /**
   * Method to construct the server class
   **/
  public Server(NodeP2 nodeConnecting) throws IOException {
    /*Create server socket*/
    serverSocket = new ServerSocket(nodeConnecting.getCommunication().getPortNumber(), 10, InetAddress.getByName(nodeConnecting.getCommunication().getHostname()));
    this.nodeConnecting = nodeConnecting;
    if (nodeConnecting.isCurrentlyCoordinator()) {
      /*Initialise coordinator class if coordinator.*/
      coordinatorClass = new Coordinator(nodeConnecting);
    }
  }


  /**
   * Method to run the node in a threaded way.
   */
  public void runNode() {
    while (true) {
      /* Scheduler used to thread the server handling. */
      //https://stackoverflow.com/questions/34324082/how-do-i-schedule-a-task-to-run-once
      ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      /*Schedule server routine.*/
      ScheduledFuture<?> coordServer = scheduler.schedule(() -> runServer(), 0, TimeUnit.SECONDS);
      while (!coordServer.isDone()) {
        try {
          /* Wait for 20 seconds if its coordinator if election not running. */
          if (nodeConnecting.isCurrentlyCoordinator() && !runningElection) {
            coordinatorClass.runCoordinatorRoutine();
            Thread.sleep(20000);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      scheduler.shutdown();
    }
  }


  /**
   * Method that holds server request operations.
   */
  private void runServer() {
    try {
      System.out.println("Server started. Listening to port " + nodeConnecting.getCommunication().getPortNumber());
      threadPool = Executors.newFixedThreadPool(maxPoolSize); /* Create a new Thread pool manager */
      while (true) {
        /*Wait for connection and add it to the threaded pool.*/
        Socket conn = serverSocket.accept();
        ch = new ConnectionHandler(conn);
        threadPool.execute(ch); /* Execute thread. */
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    } finally {
      if (threadPool != null) {
        threadPool.shutdown(); /* Shutdown the thread pool in case of Error. */
      }
    }
  }

  /**
   * Class created to manage the connection of the server to the clients.
   * Responsible for communication as well as logging of relevant information.]
   * Extends Thread.
   **/
  public class ConnectionHandler extends Thread {

    private Socket connection; /* socket representing TCP/IP connection to Client.Client */
    private InputStream inStream; /* get data from client on this input stream */
    private OutputStream outStream; /* can send data back to the client on this output stream */
    private BufferedReader clientReader; /* use buffered reader to read client data */
    private int nodeID; /*Id of the node*/
    private String username; /*Username */
    private boolean clientConnected; /*True if connection is a client. */

    /**
     * Class constructor
     *
     * @param connection The socket representing the connection the client
     */
    public ConnectionHandler(Socket connection) {
      clientConnected = false; /*Set to false by default.*/
      nodeID = nodeConnecting.getNodeID(); /*Set id*/
      this.connection = connection;
      try {
        /*Try to open streams.*/
        inStream = connection.getInputStream(); /* get data from client on this input stream */
        outStream = connection.getOutputStream(); /* to send data back to the client on this stream */
        clientReader = new BufferedReader(new InputStreamReader(inStream)); // use buffered reader to read client
      } catch (IOException ioe) {
        System.out.println("ConnectionHandler: " + ioe.getMessage());
      }
    }


    /**
     * Run method inStream invoked when the Thread's start method. Is responsible
     * for servicing the clients' requests and its own disposal.
     **/
    public void run() {
      try {
        /*Run client servicing. */
        serviceClient();
        /*If too many clietns are logged in then disconnect. Otherwise keep sockets open.*/
        if (clientConnected && (clientCount <= 2)) {
          /*Always service if client. */
          while (true) {
            serviceClient();
          }
        }
      } catch (IOException e) {
        System.out.println("ConnectionHandler:Error occurred. " + e.getMessage());
      } finally {
        /*Remove user in case of disconnection*/
        if (clientConnected) {
          clientConnected = false;
          nodeConnecting.getClientHandler().removeUSer(username);
          clientCount--;
        }
        disposeThread(); /* Dispose of all unamanaged resources (i.e. streams) */
      }
    }


    /**
     * Function used to service the clients' requests. Processes the input, replies
     * to the client and also logs the relevant data.
     */
    private void serviceClient() throws IOException {
      String clientMessage = clientReader.readLine(); /* Read client's request. */
      /*Process node request. */
      /*If null message stop.*/
      if (clientMessage == null) {
        return;
      }
      /*Process message received*/
      Comms_Protocol messageReceived = new Comms_Protocol(clientMessage);
      /*Check if client connection.*/
      if (messageReceived.getType() == Comms_Protocol.CONNECTION_TYPE.CLIENT) {
        clientCount++;
        clientConnected = true;
        if (clientCount > 2) {
          /*Disconnect client if too many*/
          String disconnectionMessage = "Your connection has been declined!";
          outStream.write(disconnectionMessage.getBytes());
          return;
        }
      } else {
        clientConnected = false;
      }
      /*Prepare message for logging. */
      Timestamp ts = new Timestamp(System.currentTimeMillis());
      String outputLogMessage = "[" + ts.toString() + " | " + nodeID + "] Receive from " + messageReceived.getSenderID() + ": ";
      outputLogMessage += messageReceived.getCommand() + " ";

      /*Get payload*/
      String payload = messageReceived.getPayload();
      if (payload != null) {
        outputLogMessage += payload;
      }
      /*Get node's client handler.*/
      ClientHandler nodeClient = nodeConnecting.getClientHandler();
      /*Log information. */
      nodeConnecting.logInfo(outputLogMessage);
      System.out.println(outputLogMessage);
      /*Process command accordingly. */
      switch (messageReceived.getCommand()) {
        case PING:
          /*Checking node is alive.*/
          break;
        case INFORM:
          /*Set successor*/
          nodeConnecting.setSuccessor(new Communication(payload));
          nodeConnecting.setTokenNumber(messageReceived.getTokenCount());
          break;
        case ELECTION:
          /*Run election*/
          runElection(payload, nodeConnecting);
          break;
        case COORDINATOR:
          /*Update coordinator info*/
          handleCoordinationRequest(messageReceived);
          break;
        case TOKEN:
          /*Run token operations*/
          tokenRoutine(messageReceived);
          break;
        case LOGIN:
          /*Log user*/
          username = messageReceived.getUserName();
          /*Add it to list of registered users.*/
          nodeClient.addUser(messageReceived.getUserName(), connection);
          break;
        case ADD:
          /*Add message*/
          username = messageReceived.getUserName();
          nodeClient.addUser(messageReceived.getUserName(), connection);
          nodeClient.addMessageToQueue(messageReceived.getData());
          break;
        case ALL:
          /*Multicast message.*/
          nodeClient.addUser(messageReceived.getUserName(), connection);
          username = messageReceived.getUserName();
          nodeClient.getMulticastQueue().add(messageReceived.getData());
          nodeClient.setMulticasterUsername(username);
          System.out.println("ALL" + username);
          break;
        default:
          /*Unknown command.*/
          break;
      }

    }

    /**
     * Method to run the token routine.
     * Uses the token if needed and passes it.
     */
    private void tokenRoutine(Comms_Protocol messageReceived) {
      /*Token validation check*/
      if (nodeConnecting.getTokenNumber() != messageReceived.getTokenCount()) {
        System.out.println("Old token");
        return;
      }
      /* Run Q access checks*/
      handleQueueManager(nodeConnecting);
      /*Check if elections are needed when token received.*/
      /*If interim node, start election. */
      if ((nodeID > nodeConnecting.getCoordinatorInfo().getNodeID()) && !startedElection || (nodeConnecting.isCurrentlyCoordinator() && coordinatorClass.isInterim())) {
        startedElection = true;
        /*Prepare and send election message.*/
        String message = "{" + nodeConnecting.getCommunication().getInfoFormat() + "}";
        sendNodeMessage(nodeID, nodeConnecting.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.ELECTION, message);
      }
      /*Pass token along.*/
      String valueOf = String.valueOf(messageReceived.getTokenCount());
      sendNodeMessage(nodeID, nodeConnecting.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.TOKEN, valueOf);
    }

    /**
     * Method to handle coordination messages. COORDINATOR
     */
    private void handleCoordinationRequest(Comms_Protocol messageReceived) {
      /*Increment coordination message count*/
      coordinationCount++;
      /*If coordiantion messages equal election messages it is finished.*/
      if (electionCount == coordinationCount) {
        /*Election is over.*/
        if (!startedElection) {
          /*Pass if did not start*/
          sendNodeMessage(nodeID, nodeConnecting.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.COORDINATOR, messageReceived.getPayload());
        }
        /*Stop sending otherwise. Finish election by setting new coordinator. */
        finishElection(messageReceived.getPayload());
      } else {
        /*Send coordinator message to successor.*/
        sendNodeMessage(nodeID, nodeConnecting.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.COORDINATOR, messageReceived.getPayload());
      }
    }

    /**
     * Function to finish and reset election variables.
     * Processes the payload to get the new coordinator's info.
     */
    private void finishElection(String payload) {
      /*Clear variables*/
      electionCount = 0;
      coordinationCount = 0;
      startedElection = false;
      /*Process payload.*/
      Communication newCoordinator = new Communication(payload);
      /*Initialise coordinator class if new coordinator*/
      if (newCoordinator.getNodeID() == nodeConnecting.getCommunication().getNodeID()) {
        coordinatorClass = new Coordinator(nodeConnecting);
      }
      /*Update status of coordination and reset running election*/
      nodeConnecting.setCoordinatorInfo(newCoordinator);
      runningElection = false;
      /*It will now run coordination routines straight away.*/
    }


    /**
     * Function used to dispose of the thread. Released all unmanaged resources.
     */
    private void disposeThread() {
      try {
        /* Close all input/outputs streams */
        outStream.close();
        clientReader.close();
        inStream.close();
        if (!connection.isClosed()) {
          connection.close();
        }
      } catch (IOException ioe) {
        System.out.println("ConnectionHandler: DisposeThread " + ioe.getMessage());
      }
    }


    /**
     * Method use to handle election messages.
     */
    private void runElection(String payload, NodeP2 thisNode) {
      runningElection = true;
      /*Set status of election to running*/
      /*Increment election message counter*/
      electionCount = electionCount + 1;
      int index = payload.indexOf(",");
      /*Check if it is this node's election message*/
      int firstId = Integer.parseInt(payload.substring(1, index));
      if (firstId == nodeID) {
        /*Decide and send coordinator message*/
        ArrayList<Communication> nodeIdSet = Election.processElectionMessage(payload);
        /*Sort descing and get first.*/
        Collections.sort(nodeIdSet, (o1, o2) -> {
          if (o1.getNodeID() > o2.getNodeID()) {
            return -1;
          } else {
            return 1;
          }
        });
        Communication highestNodeInfo = nodeIdSet.get(0);
        /*Send the endpoint information to other nodes. */
        sendNodeMessage(nodeID, thisNode.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.COORDINATOR, highestNodeInfo.getInfoFormat());
      } else {
        /*Append node's endpoint and id and pass along.*/
        String message = payload.replace("}", ";");
        message += thisNode.getCommunication().getInfoFormat() + "}";
        sendNodeMessage(nodeID, thisNode.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.ELECTION, message);
      }
    }

    /**
     * Method used to manage Q access.
     * Uses Multicast(ALL), PULL and ADD methods to communicate with it.
     */
    private void handleQueueManager(NodeP2 myNode) {
      ClientHandler nodeClientHander = myNode.getClientHandler();
      HashMap<String, Socket> userConnected = nodeClientHander.getUsersLoggedIn();
      /*Do not use if clients are not connected*/
      if (userConnected.isEmpty()) {
        return;
      }

      /*Iterate through clients logged in.*/
      for (String username : userConnected.keySet()) {
        /*Check messages*/
        String message = nodeID + ":PULL:" + username;
        /*Send request to Q*/
        String messageBack = sendQueueManagerMessage(message);
        if (messageBack.equals("OK") || messageBack.equals("FAILED")) {
          /*Empty, check next client.*/
        } else {
          System.out.println("Message received: " + messageBack);
          //TODO check if message is MULTICAST. MUST STORE IF MORE CLIENTS ARE USING THE NETWORK.
          /*Send to client*/
          Socket clientConnection = userConnected.get(username);
          sendClientMessage(nodeID, username, clientConnection, messageBack);
          return;
        }
      }

      /*Adding multicast message if queue is not empty -ALL*/
      if (!nodeClientHander.getMulticastQueue().isEmpty()) {
        String message = nodeClientHander.getMulticastQueue().removeFirst();
        String queueMessage = nodeID + ":ALL:";
        /*Send message to queue*/
        String messageBack = sendQueueManagerMessage(queueMessage + message);
        if (messageBack.equals("OK") || messageBack.equals("FAILED")) {
        } else {
          /* Message added.*/
          System.out.println("Message Added: " + messageBack);
          return;
        }
      }

      /* Post message via ADD */
      if (!nodeClientHander.isMessagingQueueEmpty()) {
        /*There's data*/
        String message = nodeClientHander.getMessageQueue().getFirst();
        String queueMessage = nodeID + ":ADD:" + message;
        /*Add data*/
        String response = sendQueueManagerMessage(queueMessage);
        if (response.equals("OK")) {
          /*REmove first and send.*/
          nodeClientHander.getMessageQueue().removeFirst();
        }
      }
      return;
    }


    /**
     * Method to communicate with Q resource. (Server).
     */
    private String sendQueueManagerMessage(String message) {
      /*Information for logging. */
      Timestamp ts = new Timestamp(System.currentTimeMillis());
      String outputLogMessage = "[" + ts.toString() + " | " + nodeID + "] Send to Q: " + message;
      String output = null;
      /*Open socket and send to Q*/
      try (Socket echoSocket = new Socket(QueueManager.HOSTNAME, QueueManager.PORT_NUMBER);
           PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
           BufferedReader reader = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))) {
        out.println(message);
        out.flush();
        output = reader.readLine();
      } catch (UnknownHostException e) {
        System.err.println("Unknown host: ");
      } catch (IOException e) {
        System.err.println("Queue Manager is not alive");
        //TODO - Look at potentially informing the coordinator that the resource is down.
      }
      /*Log information*/
      nodeConnecting.logInfo(outputLogMessage);
      System.out.println(outputLogMessage);
      return output;
    }
  }


  /**
   * Method to communicate with client node.
   */
  public void sendClientMessage(int nodeID, String username, Socket client, String data) {
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    String outputLogMessage = "[" + ts.toString() + " | " + nodeID + "] Send to " + username + ":" + data;
    /*Send to current info of next*/
    try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
      out.println(data);
      out.flush();
    } catch (UnknownHostException e) {
      System.err.println("Unknown host: " + username);
    } catch (IOException e) {
      System.err.println("Client is not alive");
    }
    /*Log data*/
    nodeConnecting.logInfo(outputLogMessage);
    System.out.println(outputLogMessage);
  }


  /**
   * Method to send message to successor.
   **/
  public void sendNodeMessage(int nodeID, Communication receiver, Comms_Protocol.MESSAGE_TYPE
          message_type, String payload) {
    /*Network has been reformed. Wait until sucessor is set.*/
    if (receiver == null) {
      System.out.println("Network reforming. Wait for coordinator.");
      return;
    }
    /*Information for logging.*/
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    String outputLogMessage = "[" + ts.toString() + " | " + nodeID + "] Send to " + receiver.getNodeID() + ":";
    outputLogMessage += message_type + " " + payload;
    /*Prepare outgoing message*/
    Comms_Protocol message = new Comms_Protocol(nodeID, message_type, payload);
    /*Open connection and send.*/
    try (Socket echoSocket = new Socket(receiver.getHostname(), receiver.getPortNumber());
         PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);) {
      out.write(message.toString());
      out.flush();
    } catch (UnknownHostException e) {
      System.err.println("Unknown host: " + receiver.getHostname());
    } catch (IOException e) {
      System.err.println("Node is not alive. Check Coordinator.,");
      /*Try to inform the coordinator.*/
      //TODO inform coordinator that the successor is dead.
      /*Check if next node is coordinator*/
      /*Become interim coordinator if true;*/
      if (receiver.getInfoFormat().equals(nodeConnecting.getCoordinatorInfo().getInfoFormat())) {
        System.out.println("Coordinator is dead");
        coordinatorClass = new Coordinator(nodeConnecting, true);
        nodeConnecting.setCoordinatorInfo(nodeConnecting.getCommunication());
      }
    }
    /*Log data.*/
    nodeConnecting.logInfo(outputLogMessage);
    System.out.println(outputLogMessage);

  }
}
