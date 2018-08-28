package org.obolibrary.robot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enum containing all possible relation types.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public enum RelationType {
  SELF("self"),
  CHILDREN("children"),
  PARENTS("parents"),
  DESCENDANTS("descendants"),
  ANCESTORS("ancestors"),
  EQUIVALENTS("equivalents"),
  TYPES("types"),
  CLASSES("classes"),
  PROPERTIES("properties"),
  INDIVIDUALS("individuals"),
  OBJECT_PROPERTIES("object-properties"),
  ANNOTATION_PROPERTIES("annotation-properties"),
  DATA_PROPERTIES("data-properties");

  private static final Set<RelationType> RELATION_TYPES = new HashSet<>();
  private static final Map<String, RelationType> NAME_RELATION_MAP = new HashMap<>();

  static {
    RELATION_TYPES.add(SELF);
    RELATION_TYPES.add(CHILDREN);
    RELATION_TYPES.add(PARENTS);
    RELATION_TYPES.add(DESCENDANTS);
    RELATION_TYPES.add(ANCESTORS);
    RELATION_TYPES.add(EQUIVALENTS);
    RELATION_TYPES.add(TYPES);
    RELATION_TYPES.add(CLASSES);
    RELATION_TYPES.add(PROPERTIES);
    RELATION_TYPES.add(INDIVIDUALS);
    RELATION_TYPES.add(OBJECT_PROPERTIES);
    RELATION_TYPES.add(ANNOTATION_PROPERTIES);
    RELATION_TYPES.add(DATA_PROPERTIES);
    for (RelationType rt : RELATION_TYPES) {
      NAME_RELATION_MAP.put(rt.toString(), rt);
    }
  }

  private final String name;

  /**
   * Constructor for a RelationType.
   *
   * @param name command line name option
   */
  RelationType(String name) {
    this.name = name;
  }

  /**
   * Return the name of the RelationType.
   *
   * @return string name
   */
  @Override
  public String toString() {
    return name;
  }

  public static RelationType getRelationType(String name) {
    RelationType rt = NAME_RELATION_MAP.get(name);
    if (rt == null) {
      // TODO
      throw new IllegalArgumentException("");
    }
    return rt;
  }

  public static boolean isRelationType(String name) {
    return NAME_RELATION_MAP.get(name) != null;
  }
}
