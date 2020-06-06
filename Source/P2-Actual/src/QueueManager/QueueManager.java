package QueueManager;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Class used to implement resource Q.
 */
public class QueueManager {

  /**
   * Class implement message structure.
   */
  public class MessageClass {
    String sender;
    String receiver;
    String data;
    int nodeID;
  }

  /*Variables */
  private LinkedList<MessageClass> multicastQueue = new LinkedList<MessageClass>();
  private LinkedList<MessageClass> messageQueue = new LinkedList<MessageClass>();
  private HashMap<String, Integer> clientWithData = new HashMap<>();/* Username is key, integer is number of messages.*/
  private ServerSocket serverSocket;
  /*Default location*/
  public final static String HOSTNAME = "localhost";
  public final static int PORT_NUMBER = 8090;

  enum OPTIONS {
    PULL,
    ADD,
    ALL,
  }

  /**
   * Class to create queue manager and service requests from server nodes.
   */
  public QueueManager() throws IOException {
    /*Open up server socket. */
    serverSocket = new ServerSocket(PORT_NUMBER, 5, InetAddress.getByName(HOSTNAME));
    /*Single Threaded*/
    System.out.println("Creating resource manager.");
    while (true) {
      /*Print queue sizes. */
      System.err.println("Multicast queue is:" + multicastQueue.size());
      System.err.println("Current queue size is:" + messageQueue.size());
      System.out.println("Waiting for clients request");
      /*Wait for connection*/
      try (Socket connection = serverSocket.accept(); /*Wait for connection*/
           BufferedReader clientReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
           PrintWriter outputStream = new PrintWriter(connection.getOutputStream());) {
        System.out.println("Connection opened");
        String clientMessage = clientReader.readLine(); /* Read client's request. */
        /*Service request*/
        System.out.println(clientMessage);
        String outputMessage = ServiceRequest(clientMessage);
        System.out.println("Send reply:" + outputMessage);
        outputStream.println(outputMessage);
        outputStream.flush();
        System.out.println("Reply Sent.");
      } catch (UnknownHostException e) {
        System.err.println("Unknown host:");
      } catch (IOException e) {
        System.err.println("Node is not alive");
        /*Try to inform the coordinator. */
      }
    }
  }

  public static void main(String args[]) {
    /*Run queue manager*/
    try {
      new QueueManager();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * /*Method to process message received.
   */
  public class Protocol {
    public OPTIONS getRequest() {
      return request;
    }

    private OPTIONS request;

    public MessageClass getMessageReceived() {
      return messageReceived;
    }

    private MessageClass messageReceived = new MessageClass();

    public Protocol(String payload) {
      String[] splitData = payload.split(":");
      messageReceived.nodeID = Integer.parseInt(splitData[0]);
      this.request = OPTIONS.valueOf(splitData[1]);
      /*Process message received by command*/
      switch (request) {
        case PULL:
          messageReceived.receiver = splitData[2];
          break;
        case ADD:
          messageReceived.receiver = splitData[2];
          messageReceived.sender = splitData[3];
          messageReceived.data = splitData[4];
          break;
        case ALL:
          messageReceived.sender = splitData[2];
          messageReceived.data = splitData[3];
          messageReceived.receiver = messageReceived.sender;
          break;
      }
    }

  }


  /**
   * Method to service the request sent by the node.
   */
  private String ServiceRequest(String message) {
    Protocol dataReceived = new Protocol(message);
    String messageOut;
    System.out.println("Message received: " + message);
    /*Routine for each command.*/
    switch (dataReceived.getRequest()) {
      case PULL:
        /*Multicast queu check*/
        if (multicastQueue.isEmpty()) {
          /*target message can be sent*/
          messageOut = getUserMessages(dataReceived);
        } else {
          /*Check multicast queue message*/
          MessageClass messageMulti = multicastQueue.peekFirst();
          /*Check node*/
          String currentUser = dataReceived.getMessageReceived().receiver;
          /*Wait until node that has inserted it removes it.*/
          if (messageMulti.nodeID == dataReceived.messageReceived.nodeID) {
            /*Remove*/
            multicastQueue.removeFirst();
            messageOut = getUserMessages(dataReceived);
          } else {
            /*Send otherwise*/
            messageOut = currentUser + ":";
            messageOut += messageMulti.sender + ":";
            messageOut += messageMulti.data;
          }
        }
        break;
      case ADD:
        /*Add request. Put it into queue and update the message counter for the user.*/
        messageQueue.add(dataReceived.getMessageReceived());
        /*Update count */
        String addReceiver = dataReceived.getMessageReceived().receiver;
        int numberMessages = 0;
        if (clientWithData.containsKey(addReceiver)) {
          numberMessages = clientWithData.get(addReceiver);
        }
        clientWithData.put(addReceiver, numberMessages + 1);
        messageOut = "OK";
        break;
      case ALL:
        /*Multicast request. Add to multicast queue. */
        multicastQueue.add(dataReceived.getMessageReceived());
        messageOut = "OK";
        break;
      default:
        messageOut = "FAILED";
        break;

    }
    return messageOut;
  }


  /**
   * Method to get the user messages.
   */
  private String getUserMessages(Protocol dataReceived) {
    String messageOut;
    String receiver = dataReceived.getMessageReceived().receiver;
    /* Check if user has messages*/
    if (clientWithData.containsKey(receiver)) {
      /*get number of messages*/
      int messagesAvailable = clientWithData.get(receiver);
      /*Data available read message*/
      Iterator<MessageClass> listIterator = messageQueue.listIterator();
      MessageClass storedMessage = null;
      while (listIterator.hasNext()) {
        storedMessage = listIterator.next();
        if (storedMessage.receiver.equals(receiver)) {
          /*Update map*/
          messagesAvailable--;
          if (messagesAvailable <= 0) {
            /*Remove from list*/
            clientWithData.remove(receiver);
          }
          /*Delete from list*/
          listIterator.remove();
        }
      }
      /*Return message in correct format*/
      if (storedMessage != null) {
        messageOut = dataReceived.getMessageReceived().receiver + ":";
        messageOut += storedMessage.sender + ":";
        messageOut += storedMessage.data;
      } else {
        messageOut = "FAILED";
      }
    } else {
      messageOut = "OK";
    }
    return messageOut;
  }
}
