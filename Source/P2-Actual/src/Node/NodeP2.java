package Node;

import java.io.*;


/**
 * Class responsible for implementing node functionalities.
 */
public class NodeP2 {

  /*Communication info for this node, successor and coordinator*/
  private Communication coordinatorInfo;
  private Communication communication;
  private Communication successor;
  private ClientHandler clientHandler = new ClientHandler();
  private currentStates currentState;
  private int tokenNumber = 0;
  private Logging loghandler;

  public void logInfo(String message) {
    loghandler.logEntry(message);
  }

  public int getNodeID() {
    return communication.getNodeID();
  }


  public enum currentStates {
    COORDINATOR,
    NODE,
  }

  public int getTokenNumber() {
    return tokenNumber;
  }

  public void setTokenNumber(int tokenNumber) {
    this.tokenNumber = tokenNumber;
  }

  /*Setters and getters*/
  public boolean isCurrentlyCoordinator() {
    return currentState == currentStates.COORDINATOR;
  }


  public ClientHandler getClientHandler() {
    return clientHandler;
  }

  public Communication getCoordinatorInfo() {
    return coordinatorInfo;
  }

  public Communication getSuccessor() {
    return successor;
  }

  public Communication getCommunication() {
    return communication;
  }


  /**
   * Method to store the new coordinator information.
   *
   * @param coordinatorInfo The communication info of the new coordinator.
   */
  public void setCoordinatorInfo(Communication coordinatorInfo) {
    this.coordinatorInfo = coordinatorInfo;
    /* Check if this node is the */
    this.successor = null;
    if (coordinatorInfo.getInfoFormat().equals(communication.getInfoFormat())) {
      /*Update status*/
      currentState = currentStates.COORDINATOR;
      System.out.println("I am coordinating, node ID: " + coordinatorInfo.getNodeID());
    } else {
      System.out.println("My coordinator ID is: " + coordinatorInfo.getNodeID());
      currentState = currentStates.NODE;
    }
  }


  /**
   * Set successor of the node.
   */
  public void setSuccessor(Communication successor) {
    this.successor = successor;
  }


  /**
   * Constructor for initial console line parameters.
   */
  public NodeP2(String[] inputParameters) throws IOException {
    int nodeID = Integer.parseInt(inputParameters[0]); /* Integer version of node Id */
    String endpoint = inputParameters[1];
    String coordinatorEnd = inputParameters[2];
    /*This nodes endpoint info*/
    String[] array = endpoint.split(":");
    String hostName = array[0];
    int portNumber = Integer.parseInt(array[1]);
    /*This Coordinator endpoint info*/
    array = coordinatorEnd.split(":");
    String coordinatorHostName = array[0];
    int coordinatorPortNumber = Integer.parseInt(array[1]);
    int coordinatorID = Integer.parseInt(inputParameters[3]);
    loghandler = new Logging(nodeID);
    /*Create communication structure for both nodes.*/
    communication = new Communication(nodeID, hostName, portNumber);
    /*Check if note is initialised as the coordinator.*/
    setCoordinatorInfo(new Communication(coordinatorID, coordinatorHostName, coordinatorPortNumber));
  }


  /**
   * Main function to run it.
   */
  public static void main(String[] args) throws IOException {
    /*Process inputs*/
    NodeP2 nodeClass = new NodeP2(args);
    Server nodeServer = new Server(nodeClass);
    while (true) {
      nodeServer.runNode();
    }
  }

}
