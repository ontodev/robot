package org.obolibrary.robot.metrics;

import java.util.*;

public class MetricsResult {

  Map<String, Object> data = new HashMap<>();
  Map<String, List<Object>> data_listvalues = new HashMap<>();
  Map<String, Map<String, Object>> data_mapvalues = new HashMap<>();

  void put(String key, Object value) {
    data.put(key, value);
  }

  void addData(Map<String, String> data) {
    this.data.putAll(data);
  }

  public Map<String, Object> getData() {
    return data;
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public void putSet(String key, Set<? extends Object> set) {
    data_listvalues.put(key, new ArrayList<>(set));
  }

  public void putMap(String key, Map<String, ? extends Object> data) {
    Map<String, Object> map = new HashMap<>();
    for (String k : data.keySet()) {
      map.put(k, data.get(k));
    }
    data_mapvalues.put(key, map);
  }

  public Map<String, List<Object>> getListData() {
    return data_listvalues;
  }

  public Map<String, Map<String, Object>> getMapData() {
    return data_mapvalues;
  }

  public Object getSimpleMetricValue(String key) {
    return data.get(key);
  }

  public void importMetrics(MetricsResult data) {
    this.data.putAll(data.getData());
    this.data_listvalues.putAll(data.getListData());
    this.data_mapvalues.putAll(data.getMapData());
  }
}
