package org.obolibrary.robot;

import py4j.GatewayServer;

/**
 * Starts a gateway server for Py4J to execute ROBOT operations via Python. This class should only
 * be accessed through Py4J launch_gateway method.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class PythonOperation {

  /**
   * Run a Gateway Server.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    GatewayServer gs = new GatewayServer(null);
    try {
      gs.start();
    } finally {
      gs.shutdown();
    }
  }
}
