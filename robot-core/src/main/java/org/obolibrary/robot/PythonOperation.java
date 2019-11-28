package org.obolibrary.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py4j.GatewayServer;

/**
 * Starts a gateway server for Py4J to execute ROBOT operations via Python. This class can be used
 * to start the JVM directly from python using Py4J's `launch_gateway`.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
public class PythonOperation {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(PythonOperation.class);

  /**
   * Run a Gateway Server.
   *
   * @param args strings to use as arguments
   */
  public void main(String[] args) {
    GatewayServer gs = null;
    try {
      gs = run(null);
    } finally {
      if (gs != null) {
        gs.shutdown();
      }
    }
  }

  /**
   * Run a Gateway Server.
   *
   * @param port port to run JVM on, or null
   * @return a GatewayServer to connect to
   */
  public static GatewayServer run(Integer port) {
    GatewayServer gs;
    if (port != null) {
      gs = new GatewayServer(null, port);
    } else {
      gs = new GatewayServer(null);
    }
    gs.start();
    port = gs.getPort();
    System.out.println(
        String.format("ROBOT JVM listening for Py4j on port %d, press Ctrl-C to exit", port));
    logger.debug(String.format("ROBOT JVM started on port %d", port));
    return gs;
  }
}
