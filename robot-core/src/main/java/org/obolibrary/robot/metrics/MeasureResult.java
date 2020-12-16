package org.obolibrary.robot.metrics;

import java.util.*;

public class MeasureResult {

  Map<String, Object> data = new HashMap<>();
  Map<String, List<Object>> dataListvalues = new HashMap<>();
  Map<String, Map<String, Object>> dataMapvalues = new HashMap<>();

  /**
   * add an individual data item
   *
   * @param key the metric
   * @param value the value
   */
  public void put(String key, Object value) {
    data.put(key, value);
  }

  /**
   *
   * @param key the metric
   * @param set the value (a set of Objects)
   */
  public void putSet(String key, Set<? extends Object> set) {
    dataListvalues.put(key, new ArrayList<>(set));
  }

  /**
   *
   * @param key the metric
   * @param data the value (a map of Key-Value pairs)
   */
  public void putMap(String key, Map<String, ? extends Object> data) {
    Map<String, Object> map = new HashMap<>();
    for (String k : data.keySet()) {
      map.put(k, data.get(k));
    }
    dataMapvalues.put(key, map);
  }

  /**
   *
   * @return get the current simple data
   */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   *
   * @return all the data that is in list form
   */
  public Map<String, List<Object>> getListData() {
    return dataListvalues;
  }

  /**
   *
   * @return all the data that is in map form
   */
  public Map<String, Map<String, Object>> getMapData() {
    return dataMapvalues;
  }

  /**
   *
   * @return true if there are no results in this object, otherwise false
   */
  public boolean isEmpty() {
    return data.isEmpty() && dataListvalues.isEmpty() && dataMapvalues.isEmpty();
  }

  /**
   *
   * @param key the metric
   * @return the value recorded for that metric (only simple metrics)
   */
  public Object getSimpleMetricValue(String key) {
    return data.get(key);
  }

  /**
   *
   * @param data to be imported
   */
  public void importMetrics(MeasureResult data) {
    this.data.putAll(data.getData());
    this.dataListvalues.putAll(data.getListData());
    this.dataMapvalues.putAll(data.getMapData());
  }
}
