package org.obolibrary.robot;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pluggable commands loader.
 *
 * @author <a href="mailto:dgouttegattat@incenp.org">Damien Goutte-Gattat</a>
 */
public class PluginManager {

  private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

  private HashMap<String, URL> jars = null;

  /**
   * Find pluggable commands and add them to a CommandManager.
   *
   * @param cm the command manager to add commands to
   */
  public void addPluggableCommands(CommandManager cm) {
    if (jars == null) {
      findPlugins();
    }

    loadPlugin(cm, null, "");
    for (String pluginBasename : jars.keySet()) {
      loadPlugin(cm, jars.get(pluginBasename), pluginBasename + ":");
    }
  }

  /**
   * Load pluggable commands from a Jar file.
   *
   * @param cm the command manager to add commands to
   * @param jarFile the Jar file to load commands from; if null, will attempt to find pluggable
   *     commands in the system class path
   * @param prefix a string to prepend to the name of each pluggable command when adding them to the
   *     command manager
   */
  private void loadPlugin(CommandManager cm, URL jarFile, String prefix) {
    ClassLoader classLoader =
        jarFile != null
            ? URLClassLoader.newInstance(new URL[] {jarFile})
            : URLClassLoader.getSystemClassLoader();

    try {
      ServiceLoader<Command> serviceLoader = ServiceLoader.load(Command.class, classLoader);
      for (Command pluggableCommand : serviceLoader) {
        cm.addCommand(prefix + pluggableCommand.getName(), pluggableCommand);
      }
    } catch (ServiceConfigurationError e) {
      logger.warn("Invalid configuration in plugin %s, ignoring plugin", jarFile);
    }
  }

  /**
   * Detect Jar files in a set of directories. If a Jar file with the same basename is found in more
   * than one directory, the last one found takes precedence.
   */
  private void findPlugins() {
    String[] pluginsDirectories = {
      System.getProperty("robot.pluginsdir"),
      System.getenv("ROBOT_PLUGINS_DIRECTORY"),
      new File(System.getProperty("user.home"), ".robot/plugins").getPath()
    };
    FilenameFilter jarFilter =
        new FilenameFilter() {
          @Override
          public boolean accept(File file, String name) {
            return name.endsWith(".jar");
          }
        };

    jars = new HashMap<String, URL>();

    for (String directoryName : pluginsDirectories) {
      if (directoryName == null || directoryName.length() == 0) {
        continue;
      }

      File directory = new File(directoryName);
      if (directory.isDirectory()) {
        for (File jarFile : directory.listFiles(jarFilter)) {
          try {
            String basename = jarFile.getName();
            basename = basename.substring(0, basename.length() - 4);
            jars.put(basename, jarFile.toURI().toURL());
          } catch (MalformedURLException e) {
            // This should never happen: the URL is constructed by the Java Class Library
            // from a real filename, it should never be malformed.
          }
        }
      }
    }
  }
}
