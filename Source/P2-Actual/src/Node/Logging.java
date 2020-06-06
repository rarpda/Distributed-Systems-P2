package Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * This file contains functions related to the logging of the server activity.
 */
public class Logging {

  /*Variables.*/
  private boolean logginEnabled = true; /*Enable this flag to log data.*/
  private int nodeID;
  private FileWriter myFileWriter = null;
  private BufferedWriter logWriter = null;

  public Logging(int nodeID) throws IOException {
    this.nodeID = nodeID;
    if (logginEnabled) {
      /*Create new file if does not exist. Else delete*/
      File logFile = new File("./Log" + nodeID + ".txt");
      if (logFile.exists()) {
        logFile.delete();
      }
      /*Open up writers.*/
      myFileWriter = new FileWriter(logFile.getPath(), true);
      logWriter = new BufferedWriter(myFileWriter);
    }

  }

  /**
   * Function used for logging server data into a text file.
   *
   * @param logMessage The client's request message.
   */
  public void logEntry(String logMessage) {
    /*With 6 nodes running this is too much logging/space taken!!!!*/
    if (logginEnabled) {
      /*open the text file add log entry to it.*/
      try {
        logWriter.write(logMessage + "\r\n"); /* Write data into file once it has been fetched. */
      } catch (Exception e) {
        System.out.println("Logging:logEntry" + e.getMessage());
        try {
          /*Close in case of error.*/
          myFileWriter.close();
          logWriter.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
  }
}


