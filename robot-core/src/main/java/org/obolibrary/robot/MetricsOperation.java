package org.obolibrary.robot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.gson.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.obolibrary.robot.export.Cell;
import org.obolibrary.robot.export.Column;
import org.obolibrary.robot.export.Row;
import org.obolibrary.robot.export.Table;
import org.obolibrary.robot.metrics.MetricsResult;
import org.obolibrary.robot.metrics.OntologyMetrics;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compute metrics for the ontology.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MetricsOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MetricsOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "metrics#";

  /** Error message when metric type is illegal. Expects: metric type. */
  private static final String metricsTypeError = NS + "METRICS TYPE ERROR unknown metrics type: %s";

  /** Error message when format type is illegal. Expects: format. */
  private static final String metricsFormatError =
      NS + "METRICS FORMAT ERROR unknown metrics format: %s";

  /**
   * If a result set has results, write to the output stream and return true. Otherwise return
   * false.
   *
   * @param result the results to write
   * @param format the name of the file format to write the results to
   * @param output the file to write to
   * @return true if there were results, false otherwise
   * @throws IOException if writing file failed
   */
  public static boolean maybeWriteResult(MetricsResult result, String format, File output)
      throws IOException {
    if (!result.isEmpty()) {
      writeResult(result, format, output);
      return true;
    } else {
      return false;
    }
  }

  public static void executeMetrics(OWLOntology o, String metrics_type, String format, File output)
      throws IOException {
    MetricsResult metrics = runMetrics(o, metrics_type);
    boolean wroteData = MetricsOperation.maybeWriteResult(metrics, format, output);
    if (!wroteData) {
      logger.info("No metrics written.");
    }
  }

  /**
   * Given a dataset, a query string, a format name, and an output stream, run the SPARQL query over
   * the named graphs and write the output to the stream.
   *
   * @param ontology Ontology to run metrics
   * @param metrics_type what kind of metrics to harvest
   * @return Metrics, if successful
   */
  public static MetricsResult runMetrics(OWLOntology ontology, String metrics_type) {
    OntologyMetrics ontologyMetrics = new OntologyMetrics(ontology);
    MetricsResult metrics;
    switch (metrics_type) {
      case "essential":
        metrics = ontologyMetrics.getEssentialMetrics();
        break;
      case "extended":
        metrics = ontologyMetrics.getExtendedMetrics();
        break;
      case "all":
        metrics = ontologyMetrics.getAllMetrics();
        break;
      default:
        throw new IllegalArgumentException(String.format(metricsTypeError, metrics_type));
    }
    return metrics;
  }

  /**
   * Write a model to an output stream.
   *
   * @param result results of the metrics operation
   * @param format the language to write in (if null, TTL)
   * @param output the output stream to write to
   */
  public static void writeResult(MetricsResult result, String format, File output)
      throws IOException {
    switch (format) {
      case "tsv":
        writeTSV(result, output);
        break;
      case "yaml":
        writeYAML(result, output);
        break;
      case "json":
        writeJSON(result, output);
        break;
      case "html":
        writeHTML(result, output);
        break;
      default:
        throw new IllegalArgumentException(String.format(metricsFormatError, format));
    }
  }

  private static JsonElement resultsToJson(MetricsResult result) {
    JsonObject root = new JsonObject();
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    JsonObject metrics = new JsonObject();
    root.add("metrics", metrics);

    Map<String, Object> data = result.getData();
    List<String> keys = new ArrayList<>(data.keySet());
    Collections.sort(keys);

    for (String key : keys) {
      Object value = data.get(key);
      if (value instanceof Double) {
        metrics.addProperty(key, (Double) value);
      } else if (value instanceof Float) {
        metrics.addProperty(key, (Float) value);
      } else if (value instanceof Long) {
        metrics.addProperty(key, (Long) value);
      } else if (value instanceof Integer) {
        metrics.addProperty(key, (Integer) value);
      } else if (value instanceof Boolean) {
        metrics.addProperty(key, (Boolean) value);
      } else {
        metrics.addProperty(key, value.toString());
      }
    }
    Map<String, List<Object>> dataList = result.getListData();
    List<String> keysList = new ArrayList<>(dataList.keySet());
    Collections.sort(keysList);

    for (String key : keysList) {
      List<String> stringList = new ArrayList<>();
      dataList.get(key).forEach(s -> stringList.add(s.toString()));
      JsonElement element = gson.toJsonTree(stringList).getAsJsonArray();
      metrics.add(key, element);
    }

    Map<String, Map<String, Integer>> dataMap = result.getMapData();
    List<String> keysMap = new ArrayList<>(dataMap.keySet());
    Collections.sort(keysMap);
    for (String key : keysMap) {
      JsonElement element = gson.toJsonTree(dataMap.get(key)).getAsJsonObject();
      metrics.add(key, element);
    }
    return root;
  }

  private static void writeJSON(MetricsResult result, File output) throws IOException {
    JsonElement root = resultsToJson(result);
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    writeStringToFile(gson.toJson(root), output);
  }

  private static void writeStringToFile(String output, File outputPath) throws IOException {
    try (FileWriter fw = new FileWriter(outputPath);
        BufferedWriter bw = new BufferedWriter(fw)) {
      logger.debug("Writing metrics to: " + outputPath);
      bw.write(output);
    }
  }

  private static void writeYAML(MetricsResult result, File outputPath) throws IOException {
    JsonElement root = resultsToJson(result);
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    String output = asYaml(gson.toJson(root));
    writeStringToFile(output, outputPath);
  }

  private static String asYaml(String jsonString) throws IOException {
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    return new YAMLMapper().writeValueAsString(jsonNodeTree);
  }

  private static String escapeTSV(String s) {
    return s.replaceAll("\t", " ");
  }

  private static final Column cl_metric = new Column("metric");
  private static final Column cl_metric_value = new Column("metric_value");
  private static final Column cl_metric_type = new Column("metric_type");

  private static void addRowToTable(
      Table table, String metric, String metric_value, String metric_type) {
    Row row = new Row();
    row.add(new Cell(cl_metric, metric));
    row.add(new Cell(cl_metric_value, metric_value));
    row.add(new Cell(cl_metric_type, metric_type));
    table.addRow(row);
  }

  private static Table resultsToTable(MetricsResult result) {
    Table table = new Table("tsv");

    table.addColumn(cl_metric);
    table.addColumn(cl_metric_value);
    table.addColumn(cl_metric_type);

    // StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
      String key = escapeTSV(entry.getKey());
      String value = escapeTSV(entry.getValue().toString());
      addRowToTable(table, key, value, "single_value");
      // sb.append(key).append("\t").append(value).append("\t").append("single_value").append("\n");
    }

    for (Map.Entry<String, List<Object>> entry : result.getListData().entrySet()) {

      String key = escapeTSV(entry.getKey());
      for (Object v : entry.getValue()) {
        String value = escapeTSV(v.toString());
        addRowToTable(table, key, value, "list_value");
        // sb.append(key).append("\t").append(value).append("\t").append("list_value").append("\n");
      }
    }

    for (Map.Entry<String, Map<String, Integer>> entry : result.getMapData().entrySet()) {
      String key = escapeTSV(entry.getKey());
      Map<String, Integer> v = entry.getValue();
      for (Map.Entry<String, Integer> entryMap : v.entrySet()) {
        String key_inner = entryMap.getKey();
        String value_inner = escapeTSV(entryMap.getValue() + "");
        addRowToTable(table, key, key_inner + " " + value_inner, "list_value");
      }
    }
    return table;
  }

  private static void writeTSV(MetricsResult result, File output) throws IOException {
    Table table = resultsToTable(result);
    IOHelper.writeTable(table.toList(""), output, '\t');
  }

  private static void writeHTML(MetricsResult result, File output) throws IOException {
    Table table = resultsToTable(result);
    writeStringToFile(table.toHTML(""), output);
  }
}
