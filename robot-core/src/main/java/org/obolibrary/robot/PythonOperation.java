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
    run(null);
  }

  /**
   * Run a Gateway Server.
   *
   * @param port port to run JVM on, or null
   */
  public static void run(Integer port) {
    GatewayServer gs;
    if (port != null) {
      gs = new GatewayServer(null, port);
    } else {
      gs = new GatewayServer(null);
    }
    try {
      gs.start();
      port = gs.getPort();
      logger.debug(String.format("JVM started on port %d", port));
    } finally {
      gs.shutdown();
    }
  }
}
