package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Class to implment client node.
 */
public class Client {
  /*Predefined  testing settings.*/
  private int port;
  private String serverHostname;
  private String username;
  private final String PREDEFINED_SENDER = "ALPHA";
  private final String PREDEFINED_RECEIVER = "BETA";
  private final String PREDEFINED_MULTICAST = "GAMMA";
  private final String PREDEFINED_RECEIVER_1 = "OMEGA";
  private final String PREDEFINED_MESSAGE = "Hello";
  /*Connections to nodes.*/
  private final int PREDEFINED_NODE_0_PORTNUMBER = 8081;
  private final int PREDEFINED_NODE_1_PORTNUMBER = 8080;
  private final int PREDEFINED_NODE_2_PORTNUMBER = 8082;
  private final int PREDEFINED_NODE_3_PORTNUMBER = 8085;
  private final String PREDEFINED_NODE_A_HOSTNAME = "localhost";

  /**
   * Method to initialise client class.
   */
  public Client(String option, int profileOption) throws IOException {
    boolean multicast = false;
    boolean sender = false;
    serverHostname = PREDEFINED_NODE_A_HOSTNAME;
    /*Load predefined settings*/
    if (option.equals("SENDER")) {
      username = PREDEFINED_SENDER;
      port = PREDEFINED_NODE_0_PORTNUMBER;
      sender = true;
    } else if (option.equals("RECEIVER")) {
      if (profileOption == 1) {
        username = PREDEFINED_RECEIVER;
        port = PREDEFINED_NODE_1_PORTNUMBER;
      } else {
        username = PREDEFINED_RECEIVER_1;
        port = PREDEFINED_NODE_2_PORTNUMBER;
      }
    } else if (option.equals("MULTICAST")) {
      multicast = true;
      username = PREDEFINED_MULTICAST;
      port = PREDEFINED_NODE_3_PORTNUMBER;
    }


    System.out.println("Initialising as: " + username);
    /*Open socket for communications.*/
    try (Socket echoSocket = new Socket(serverHostname, port);
         PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))) {
      out.flush();
      while (true) {
        if (multicast) {
          /*If multicasting send an ALL message.*/
          String message = username + ":ALL:" + username + "," + "Multicasting!!";
          System.out.println("Sending: " + message);
          out.println(message);
          out.flush();
          /*Wait for reply. It will wait endlessly.*/
          System.out.println("Wait for reply");
          String reply = in.readLine();
          System.out.println(reply);
        } else {
          /*Initialised as sender.*/
          if (sender) {
            /*Write message*/
            String message = username + ":ADD:" + PREDEFINED_RECEIVER_1 + "," + PREDEFINED_SENDER + "," + PREDEFINED_MESSAGE;
            System.out.println("Sending: " + message);
            out.println(message);
            out.flush();
          } else {
            /*Send loging details*/
            System.out.println("Send login details");
            out.println(username + ":LOGIN");
            out.flush();
            /*Wait for reply*/
            System.out.println("Wait for reply");
            String reply = in.readLine();
            String message[] = reply.split(":");
            if (message.length == 3) {
              /*Process reply.*/
              String finalMessage = message[1] + " says: " + message[2];
              System.out.println(finalMessage);
            } else {
              System.out.println(reply);
            }
          }
        }
        break;
      }
    } catch (UnknownHostException e) {
      System.err.println("Unknown host: " + serverHostname);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Unable to get streams from server");
      System.exit(1);
    } finally {
    }
  }

  /**
   * Method to run client.
   * First input is String option to select Mode]
   * Second input is integer to change receiving clietn.
   */
  public static void main(String[] args) throws IOException {
    if (args.length > 0) {
      new Client(args[0], Integer.parseInt(args[1]));
    }

  }
}
