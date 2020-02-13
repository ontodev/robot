package org.obolibrary.robot;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * MOJO to update OBO context file.
 *
 * @author <a href="mailto:rbca.jackson@gmail.com">Becky Jackson</a>
 */
@Mojo(name = "UpdateContext")
public class UpdateContextMojo extends AbstractMojo {

  /** Execute updating the OBO context file. */
  public void execute() {
    getLog().info("Updating OBO context...");
    File robotMvn = new File(System.getProperty("user.dir"));
    File robot;
    if (!robotMvn.getAbsolutePath().endsWith("/robot")) {
      robot = robotMvn.getParentFile();
    } else {
      robot = robotMvn;
    }
    String contextPath = robot + "/robot-core/src/main/resources/obo_context.jsonld";
    File contextFile = new File(contextPath);

    // Download current OBO Context
    String oboContext = "";
    try {
      oboContext =
          new Scanner(
                  new URL(
                          "https://raw.githubusercontent.com/OBOFoundry/OBOFoundry.github.io/master/registry/obo_context.jsonld")
                      .openStream(),
                  "UTF-8")
              .useDelimiter("\\A")
              .next();
    } catch (IOException e) {
      getLog().error("Unable to download OBO Context!");
      e.printStackTrace();
      System.exit(1);
    }

    // Remove first two lines (opening brackets)
    List<String> oboLines = new ArrayList<>(Arrays.asList(oboContext.split("\n")));
    List<String> addContext = oboLines.subList(2, oboLines.size());

    List<String> fixed = new ArrayList<>();

    // Fix tab size
    for (String a : addContext) {
      fixed.add(a.replace("    ", "  "));
    }

    // Read in current ROBOT OBO Context
    List<String> robotLines = new ArrayList<>();
    try {
      robotLines = FileUtils.readLines(contextFile, Charset.defaultCharset());
    } catch (IOException e) {
      getLog().error("Unable to read current context!");
      e.printStackTrace();
      System.exit(1);
    }

    List<String> newContext = robotLines.subList(0, 21);
    newContext.addAll(fixed);

    try {
      FileUtils.writeLines(contextFile, newContext);
    } catch (IOException e) {
      getLog().error("Unable to write new context!");
      e.printStackTrace();
      System.exit(1);
    }

    getLog().info("OBO context updated!");
  }
}
