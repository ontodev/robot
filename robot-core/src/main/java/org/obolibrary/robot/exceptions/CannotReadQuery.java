package org.obolibrary.robot.exceptions;

/** Created by edouglass on 8/9/17. */
public class CannotReadQuery extends RuntimeException {
  public CannotReadQuery(String s, Exception e) {
    super(s, e);
  }
}
