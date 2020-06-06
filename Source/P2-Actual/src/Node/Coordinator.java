package Node;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;


/**
 * This class is responsible for the handling all the information regarding coordinating activities.
 */
public class Coordinator {

  /*Variables*/
  private NodeP2 nodeInfo;
  private ArrayList<Communication> listOfNodes;
  private ArrayList<Communication> listOfLiveNodes = new ArrayList<>();
  private boolean interimCoordinator = false;
  private boolean invert = false;
  private int tokenCount = 0;

  /**
   * Constructor of coordinator.
   */
  public Coordinator(NodeP2 nodeInfo) {
    this.nodeInfo = nodeInfo;
    this.listOfNodes = readResourceP();
    listOfNodes.removeIf((node) -> node.getNodeID() == nodeInfo.getCommunication().getNodeID());
  }

  private enum STATE {
    LIVENESS_CHECK,
    FORM_RING,
    TOKEN_CREATION,
    FINISHED,
  }


  /**
   * Constructor of interim coordinator.
   **/
  public Coordinator(NodeP2 nodeInfo, boolean interimCoordinator) {
    this.nodeInfo = nodeInfo;
    this.interimCoordinator = interimCoordinator;
    this.listOfNodes = readResourceP();
    listOfNodes.removeIf((node) -> node.getNodeID() == nodeInfo.getCommunication().getNodeID());
  }


  /**
   * Method to check if the coordinator is temporary or full time.
   */
  public boolean isInterim() {
    return interimCoordinator;
  }


  /**
   * Method to check if the nodes are alive.
   */
  public STATE checkNodesLive() {
    STATE newState;
    /*List of current live nodes detected*/
    ArrayList<Communication> currentLiveNodes = new ArrayList<>();
    /*Iterate through node list and check them*/
    listOfNodes.forEach((node) -> {
      if (sendNodeMessage(node, Comms_Protocol.MESSAGE_TYPE.PING, "")) {
        currentLiveNodes.add(node); /*Add to list if live*/
      }
    });

    /* Sleep if no nodes around found. */
    if (currentLiveNodes.size() == 0) {
      newState = STATE.LIVENESS_CHECK;
      System.out.println("No nodes found. Retrying in 2 seconds");
      try {
        /*Sleep for 2 seconds*/
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } else {
      /*Compare nodes to check if reforming ring is reqresourceFilepathuired.*/
      if (currentLiveNodes.equals(listOfLiveNodes)) {
        /*No need to reform the ring*/
        newState = STATE.FINISHED;
      } else {
        /*Form ring*/
        newState = STATE.FORM_RING;
      }
    }
    /*Update the global holder*/
    listOfLiveNodes = currentLiveNodes;
    return newState;
  }


  /**
   * Method to run the coordinator routines.
   */
  public void runCoordinatorRoutine() {
    STATE state = STATE.LIVENESS_CHECK;
    while (state != STATE.FINISHED) {
      switch (state) {
        case LIVENESS_CHECK:
          /*Check nodes state*/
          state = checkNodesLive();
          break;
        case FORM_RING:
          /* create token number and form ring*/
          tokenCount = (int) (100.0 * Math.random());
          nodeInfo.setTokenNumber(tokenCount);
          createRing();
          state = STATE.TOKEN_CREATION;
          break;
        case TOKEN_CREATION:
          /*Wait and send token*/
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          /*Coordinator connecting, send token.*/
          sendNodeMessage(nodeInfo.getSuccessor(), Comms_Protocol.MESSAGE_TYPE.TOKEN, String.valueOf(tokenCount));
          state = STATE.FINISHED;
          break;
        case FINISHED:
          state = STATE.LIVENESS_CHECK;
          break;
        default:
          break;
      }
    }
  }


  /**
   * Method to send message to node.
   */
  public boolean sendNodeMessage(Communication receiver, Comms_Protocol.MESSAGE_TYPE message_type, String payload) {
    boolean messagedSent = false;
    /*Get comms info*/
    Comms_Protocol message = new Comms_Protocol(nodeInfo.getCommunication().getNodeID(), message_type, payload);
    /*Use information for logging.*/
    Timestamp ts = new Timestamp(System.currentTimeMillis());
    String outputLogMessage = "[" + ts.toString() + " | " + nodeInfo.getNodeID() + "] Send to " + receiver.getNodeID() + ":";
    outputLogMessage += message_type + " " + payload;
    System.out.println(outputLogMessage);
    /*Open and close socket and send*/
    try (Socket echoSocket = new Socket(receiver.getHostname(), receiver.getPortNumber());
         PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true)) {
      out.write(message.toString());
      out.flush();
      messagedSent = true;
    } catch (UnknownHostException e) {
      System.err.println("Unknown host: " + receiver.getHostname());
    } catch (IOException e) {
      System.err.println(": Node is not alive");
    }
    nodeInfo.logInfo(outputLogMessage);
    return messagedSent;
  }


  /**
   * Method used to create the ring network.
   */
  private void createRing() {
    System.err.println("Forming Ring");
    /* Invert the ring everytime its reformed.*/
    if (invert) {
      Collections.reverse(listOfLiveNodes);
      invert = false;
    } else {
      invert = true;
    }

    /*Iterate through node list and set successor*/
    for (int index = 0; index < listOfLiveNodes.size(); index++) {
      /*Node*/
      Communication currentNodeInfo;
      /*Sucessor*/
      Communication nextNodeInfo;
      if (index == (listOfLiveNodes.size() - 1)) {
        /*Set the last to the coordinator.*/
        currentNodeInfo = listOfLiveNodes.get(index);
        nextNodeInfo = nodeInfo.getCommunication();
      } else {
        currentNodeInfo = listOfLiveNodes.get(index);
        nextNodeInfo = listOfLiveNodes.get(index + 1);
      }
      /*Inform nodes of successor.*/
      sendNodeMessage(currentNodeInfo, Comms_Protocol.MESSAGE_TYPE.INFORM, nextNodeInfo.getInfoFormat() + ":" + tokenCount);
    }
    /*Store successor of the coordinator*/
    nodeInfo.setSuccessor(listOfLiveNodes.get(0));
    System.out.println("Linking myself(" + nodeInfo.getCommunication().getNodeID() + ") to " + listOfLiveNodes.get(0).getNodeID());
  }


  /**
   * Method to read resource P.
   * Stored locally.
   */
  private ArrayList<Communication> readResourceP() {
    /* Read file*/
    try (FileInputStream is = new FileInputStream("P.txt");
         InputStreamReader isr = new InputStreamReader(is, Charset.defaultCharset());
         BufferedReader reader = new BufferedReader(isr)) {
      String line;
      ArrayList<Communication> outputList = new ArrayList<Communication>(); /*Temporary holder for data*/
      while ((line = reader.readLine()) != null) {
        /* Process communication information.*/
        Communication commPoint = new Communication(line);
        /*Add all words to a list.*/
        outputList.add(commPoint);
      }
      /*Copy to array and return it.*/
      return outputList;
    } catch (FileNotFoundException e) {
      System.out.println("P was not found.");
    } catch (IOException e) {
      System.out.println("Error loading the file.");
    }
    return null;
  }
}
