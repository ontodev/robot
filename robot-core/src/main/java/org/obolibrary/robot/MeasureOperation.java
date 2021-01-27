package org.obolibrary.robot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import org.obolibrary.robot.export.Cell;
import org.obolibrary.robot.export.Column;
import org.obolibrary.robot.export.Row;
import org.obolibrary.robot.export.Table;
import org.obolibrary.robot.metrics.MeasureResult;
import org.obolibrary.robot.metrics.OntologyMetrics;
import org.obolibrary.robot.providers.CURIEShortFormProvider;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compute metrics for the ontology.
 *
 * @author <a href="mailto:nicolas.matentzoglu@gmail.com">Nicolas Matentzoglu</a>
 */
public class MeasureOperation {
  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(MeasureOperation.class);

  /** Namespace for error messages. */
  private static final String NS = "measure#";

  /** Error message when metric type is illegal. Expects: metric type. */
  private static final String METRICS_TYPE_ERROR =
      NS + "METRICS TYPE ERROR unknown metrics type: %s";

  /** Error message when format type is illegal. Expects: format. */
  private static final String METRICS_FORMAT_ERROR =
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
  public static boolean maybeWriteResult(MeasureResult result, String format, File output)
      throws IOException {
    if (!result.isEmpty()) {
      writeResult(result, format, output);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Running the measure command
   *
   * @param ontology input ontology
   * @param rf reasoner factory to be used for reasoning metrics
   * @param metricsType The type of metrics that should be generated, like 'essential', 'extended'
   *     or all
   * @param format the name of the file format to write the results to
   * @param output the file to write to
   * @param prefixes prefix map to be used for computing metrics
   * @throws IOException if writing file failed
   */
  public static void measure(
      OWLOntology ontology,
      OWLReasonerFactory rf,
      String metricsType,
      String format,
      File output,
      Map<String, String> prefixes)
      throws IOException {
    MeasureResult metrics = new MeasureResult();
    CURIEShortFormProvider curieShortFormProvider = new CURIEShortFormProvider(prefixes);
    if (metricsType.contains("reasoner")) {
      metrics.importMetrics(getMetrics(ontology, rf, metricsType, curieShortFormProvider));
    } else {
      metrics.importMetrics(getMetrics(ontology, metricsType, curieShortFormProvider));
    }
    boolean wroteData = MeasureOperation.maybeWriteResult(metrics, format, output);
    if (!wroteData) {
      LOGGER.info("No metrics written.");
    }
  }

  /**
   * Compute metrics for a given ontology.
   *
   * @param ontology Ontology to run metrics
   * @param metricsType what kind of metrics to harvest
   * @param curieShortFormProvider Shortformprovider to be used for computation of CURIEs
   * @return Metrics, if successful
   */
  public static MeasureResult getMetrics(
      OWLOntology ontology, String metricsType, CURIEShortFormProvider curieShortFormProvider) {
    OntologyMetrics ontologyMetrics = new OntologyMetrics(ontology, curieShortFormProvider);
    MeasureResult metrics;
    switch (metricsType) {
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
        throw new IllegalArgumentException(String.format(METRICS_TYPE_ERROR, metricsType));
    }
    return metrics;
  }

  /**
   * Run the metrics command using the reasoner factory. Note: when the reasoner factory is passed,
   * it is assumed that reasoner metrics should be harvested. For example: both reasoner-all, and
   * all will collect the same metrics: all metrics, plus the (simple) reasoner metrics.
   *
   * @param ontology Ontology to run metrics
   * @param rf reasoner factory, in case reasoner metrics should be collected
   * @param metricsType what kind of metrics to harvest
   * @param curieShortFormProvider short form provider
   * @return Metrics, if successful
   */
  public static MeasureResult getMetrics(
      OWLOntology ontology,
      OWLReasonerFactory rf,
      String metricsType,
      CURIEShortFormProvider curieShortFormProvider) {
    OntologyMetrics ontologyMetrics = new OntologyMetrics(ontology);
    MeasureResult metrics = new MeasureResult();
    OWLReasoner r = rf.createReasoner(ontology);
    metrics.importMetrics(ontologyMetrics.getSimpleReasonerMetrics(r));
    if (metricsType.contains("reasoner")) {
      switch (metricsType) {
        case "essential-reasoner":
          metrics.importMetrics(getMetrics(ontology, "essential", curieShortFormProvider));
          break;
        case "extended-reasoner":
          metrics.importMetrics(getMetrics(ontology, "extended", curieShortFormProvider));
          break;
        case "all-reasoner":
          metrics.importMetrics(getMetrics(ontology, "all", curieShortFormProvider));
          break;
        default:
          throw new IllegalArgumentException(String.format(METRICS_TYPE_ERROR, metricsType));
      }
      return metrics;
    } else {
      metrics.importMetrics(getMetrics(ontology, metricsType, curieShortFormProvider));
    }
    return metrics;
  }

  /**
   * Write a model to an output stream.
   *
   * @param result results of the metrics operation
   * @param format the language to write in (if null, TTL)
   * @param output the output stream to write to
   * @throws IOException on problem writing to output
   */
  public static void writeResult(MeasureResult result, String format, File output)
      throws IOException {
    switch (format) {
      case "tsv":
        writeTable(result, output, "tsv");
        break;
      case "csv":
        writeTable(result, output, "csv");
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
        throw new IllegalArgumentException(String.format(METRICS_FORMAT_ERROR, format));
    }
  }

  private static JsonElement resultsToJson(MeasureResult result) {
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

    Map<String, Map<String, Object>> dataMap = result.getMapData();
    List<String> keysMap = new ArrayList<>(dataMap.keySet());
    Collections.sort(keysMap);
    for (String key : keysMap) {
      JsonElement element = gson.toJsonTree(dataMap.get(key)).getAsJsonObject();
      metrics.add(key, element);
    }
    return root;
  }

  private static void writeJSON(MeasureResult result, File output) throws IOException {
    JsonElement root = resultsToJson(result);
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    writeStringToFile(gson.toJson(root), output);
  }

  private static void writeStringToFile(String output, File outputPath) throws IOException {
    try (FileWriter fw = new FileWriter(outputPath);
        BufferedWriter bw = new BufferedWriter(fw)) {
      LOGGER.debug("Writing metrics to: " + outputPath);
      bw.write(output);
    }
  }

  private static void writeYAML(MeasureResult result, File outputPath) throws IOException {
    JsonElement root = resultsToJson(result);
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    String output = asYaml(gson.toJson(root));
    writeStringToFile(output, outputPath);
  }

  private static String asYaml(String jsonString) throws IOException {
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    ObjectMapper mapper =
        new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    return mapper.writeValueAsString(jsonNodeTree);
  }

  private static String escapeTSV(String s) {
    return s.replaceAll("\t", " ");
  }

  private static final Column COLUMN_METRIC = new Column("metric");
  private static final Column COLUMN_METRIC_VALUE = new Column("metric_value");
  private static final Column COLUMN_METRIC_TYPE = new Column("metric_type");

  private static void addRowToTable(
      Table table, String metric, String metricValue, String metricType) {
    Row row = new Row();
    row.add(new Cell(COLUMN_METRIC, metric));
    row.add(new Cell(COLUMN_METRIC_VALUE, metricValue));
    row.add(new Cell(COLUMN_METRIC_TYPE, metricType));
    table.addRow(row);
  }

  private static Table resultsToTable(MeasureResult result, String format) {
    Table table = new Table(format);

    COLUMN_METRIC.setSort(2);
    COLUMN_METRIC_TYPE.setSort(1);
    COLUMN_METRIC_VALUE.setSort(0);

    table.addColumn(COLUMN_METRIC);
    table.addColumn(COLUMN_METRIC_VALUE);
    table.addColumn(COLUMN_METRIC_TYPE);
    table.setSortColumns();

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

    for (Map.Entry<String, Map<String, Object>> entry : result.getMapData().entrySet()) {
      String key = escapeTSV(entry.getKey());
      Map<String, Object> v = entry.getValue();
      for (Map.Entry<String, Object> entryMap : v.entrySet()) {
        String key_inner = entryMap.getKey();
        String value_inner = escapeTSV(entryMap.getValue() + "");
        addRowToTable(table, key, key_inner + " " + value_inner, "map_value");
      }
    }

    table.sortRows();
    return table;
  }

  private static void writeTable(MeasureResult result, File output, String format)
      throws IOException {
    Table table = resultsToTable(result, format);
    table.write(output.getPath(), "");
  }

  private static void writeHTML(MeasureResult result, File output) throws IOException {
    Table table = resultsToTable(result, "tsv");
    writeStringToFile(table.toHTML(""), output);
  }
}
