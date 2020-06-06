package Node;

import java.util.ArrayList;

/**
 * Class to implement election functions.
 */
public class Election {

  /**
   * Process election message received.
   * */
  public static ArrayList<Communication> processElectionMessage(String electionMessage) {
    ArrayList<Communication> outputSet = new ArrayList<>();
    /*Remove { and }*/
    String clean = electionMessage.replace("{", "");
    clean = clean.replace("}", "");
    /*Split into each individual communication end.*/
    String[] test = clean.split(";");
    /* Process into communication information endpoints. */
    for (String nodeInfo : test) {
      outputSet.add(new Communication((nodeInfo)));
    }
    return outputSet;
  }


}
