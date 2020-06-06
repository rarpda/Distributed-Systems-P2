package Node;

/**
 * Class to handle comms protocol related information.
 */
public class Comms_Protocol {


  /*Variables*/
  private int senderID;
  private MESSAGE_TYPE command;
  private String payload;
  private String userName;
  private String data;
  private int tokenCount = 0;
  private CONNECTION_TYPE type;

  enum CONNECTION_TYPE {
    COORDINATOR,
    CLIENT,
    NODE,
    RESOURCE_MANAGER
  }

  public enum MESSAGE_TYPE {
    PING,
    INFORM,
    TOKEN,
    ELECTION,
    COORDINATOR,
    LOGIN,
    ADD,
    ALL,
  }


  /*Getters and setters */
  public int getSenderID() {
    return senderID;
  }

  public MESSAGE_TYPE getCommand() {
    return command;
  }

  public String getPayload() {
    return payload;
  }

  public String getData() {
    return data;
  }

  public String getUserName() {
    return userName;
  }


  public int getTokenCount() {
    return tokenCount;
  }


  public CONNECTION_TYPE getType() {
    return type;
  }


  /**
   * Constructor to process inbound message.
   */
  public Comms_Protocol(String stringMessage) {
    /*Split message and get command*/
    String[] breakMessage = stringMessage.split(":");
    command = MESSAGE_TYPE.valueOf(breakMessage[1]);
    switch (command) {
      case PING:
        type = CONNECTION_TYPE.COORDINATOR;
        senderID = Integer.parseInt(breakMessage[0]);
        break;
      case INFORM:
        type = CONNECTION_TYPE.COORDINATOR;
        payload = breakMessage[2];
        tokenCount = Integer.parseInt(breakMessage[3]);
        senderID = Integer.parseInt(breakMessage[0]);
        break;
      case TOKEN:
        type = CONNECTION_TYPE.NODE;
        senderID = Integer.parseInt(breakMessage[0]);
        tokenCount = Integer.parseInt(breakMessage[2]);
        break;
      case ELECTION:
        type = CONNECTION_TYPE.NODE;
        senderID = Integer.parseInt(breakMessage[0]);
        payload = breakMessage[2];
        break;
      case COORDINATOR:
        type = CONNECTION_TYPE.NODE;
        senderID = Integer.parseInt(breakMessage[0]);
        payload = breakMessage[2];
        break;
      case LOGIN:
        type = CONNECTION_TYPE.CLIENT;
        userName = breakMessage[0];
        break;
      case ADD:
        type = CONNECTION_TYPE.CLIENT;
        userName = breakMessage[0];
        this.data = breakMessage[2].replace(",", ":");
        break;
      case ALL:
        type = CONNECTION_TYPE.CLIENT;
        userName = breakMessage[0];
        this.data = breakMessage[2].replace(",", ":");
        break;
    }
  }

  /*Create message*/
  public Comms_Protocol(int senderID, MESSAGE_TYPE command, String payload) {
    this.senderID = senderID;
    this.command = command;
    this.payload = payload;

  }

  /*Override to send it as outbound message*/
  @Override
  public String toString() {
    return senderID + ":" + command + ":" + payload;
  }
}
