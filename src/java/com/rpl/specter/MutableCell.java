package com.rpl.specter;

public class MutableCell {
  private Object o;

  public MutableCell(Object o) {
    this.o = o;
  }

  public Object get() {
    return o;
  }

  public void set(Object o) {
    this.o = o;
  }
}
