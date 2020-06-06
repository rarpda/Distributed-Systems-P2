package Node;

import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Handler of client Node information.
 */
public class ClientHandler {
  /* Variables for handling client information*/
  private String multicasterUsername;
  private LinkedList<String> multicastQueue = new LinkedList<>();
  private LinkedList<String> messageQueue = new LinkedList<String>();
  private HashMap<String, Socket> usersLoggedIn = new HashMap<>();
  private HashMap<String, LinkedList<String>> messagingQueue = new HashMap<>();

  /***
   * Function to login user.
   * */
  public void addUser(String username, Socket connection) {
    usersLoggedIn.put(username, connection);
    messagingQueue.put(username, new LinkedList<>());
  }


  /**
   * Function to logout user.
   */
  public void removeUSer(String username) {
    usersLoggedIn.remove(username);
    messagingQueue.remove(username);
  }


  /**
   * Method to get users logged in.
   */
  public HashMap<String, Socket> getUsersLoggedIn() {
    return usersLoggedIn;
  }


  /**
   * Method to get message queue.
   */
  public LinkedList<String> getMessageQueue() {
    return messageQueue;
  }

  public boolean isMessagingQueueEmpty() {
    return messageQueue.isEmpty();
  }


  /**
   * Method to add message to the queue.
   */
  public void addMessageToQueue(String data) {
    /*Process data*/
    messageQueue.add(data);
  }


  public LinkedList<String> getMulticastQueue() {
    return multicastQueue;
  }


  public void setMulticasterUsername(String multicasterUsername) {
    this.multicasterUsername = multicasterUsername;
  }
}
