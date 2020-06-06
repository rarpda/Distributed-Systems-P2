package Node;


/**
 * Class that handles all aspects regarding communication between nodes.
 */
public class Communication {

  /*Variables for communication*/
  private int nodeID; /*ID of the node*/
  private String hostname; /*Host name*/
  private int portNumber;
  private String infoFormat /*Format to send to other nodes.*/;

  /**
   * Constructor  for initial information being loaded.
   */
  public Communication(int nodeID, String hostname, int portNumber) {
    this.nodeID = nodeID;
    this.hostname = hostname;
    this.portNumber = portNumber;
    this.infoFormat = nodeID + "," + hostname + "," + portNumber;
  }

  /**
   * Constructor that takes inputs from messages
   */
  public Communication(String fileLine) {
    String[] lineSeparator = fileLine.split(",");
    this.nodeID = Integer.parseInt(lineSeparator[0]);
    this.hostname = lineSeparator[1];
    this.portNumber = Integer.parseInt(lineSeparator[2]);
    this.infoFormat = fileLine;
  }


  /*Getters and Setters*/
  public int getNodeID() {
    return nodeID;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public String getInfoFormat() {
    return infoFormat;
  }


}