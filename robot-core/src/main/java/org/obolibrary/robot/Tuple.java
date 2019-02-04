package org.obolibrary.robot;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nonnull;

/**
 * Created by edouglass on 8/14/17.
 *
 * <p>Generic Tuple
 */
public class Tuple<A, B> implements Collection {

  private A left;

  private B right;

  Tuple(A left, B right) {
    this.left = left;
    this.right = right;
  }

  public A left() {
    return left;
  }

  public B right() {
    return right;
  }

  @Override
  public int size() {
    return 2;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return o.equals(left) || o.equals(right);
  }

  @Nonnull
  @Override
  public Iterator iterator() {
    return Lists.newArrayList(left, right).iterator();
  }

  @Nonnull
  @Override
  public Object[] toArray() {
    return new Object[] {left, right};
  }

  @Override
  public boolean add(Object o) {
    return false;
  }

  @Override
  public boolean remove(Object o) {
    return false;
  }

  @Override
  public boolean addAll(@Nonnull Collection c) {
    return false;
  }

  @Override
  public void clear() {
    left = null;
    right = null;
  }

  @Override
  public boolean retainAll(@Nonnull Collection c) {
    return Lists.newArrayList(left, right).retainAll(c);
  }

  @Override
  public boolean removeAll(@Nonnull Collection c) {
    return Lists.newArrayList(left, right).removeAll(c);
  }

  @Override
  public boolean containsAll(@Nonnull Collection c) {
    return Lists.newArrayList(left, right).containsAll(c);
  }

  @Nonnull
  @Override
  public Object[] toArray(@Nonnull Object[] a) {
    if (a.length < 2) {
      return new Object[] {left, right};
    } else {
      a[0] = left;
      a[1] = right;
      return a;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Tuple<?, ?> tuple = (Tuple<?, ?>) o;

    if (!left.equals(tuple.left)) return false;
    return right.equals(tuple.right);
  }

  @Override
  public int hashCode() {
    int result = left.hashCode();
    result = 211 * result + right.hashCode();
    return result;
  }
}
