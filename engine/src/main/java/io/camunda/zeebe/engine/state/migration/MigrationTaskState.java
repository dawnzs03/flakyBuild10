/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.migration;

import io.camunda.zeebe.db.DbValue;
import io.camunda.zeebe.msgpack.UnpackedObject;
import io.camunda.zeebe.msgpack.property.EnumProperty;

public final class MigrationTaskState extends UnpackedObject implements DbValue {

  private final EnumProperty<State> stateProp =
      new EnumProperty<>("state", State.class, State.NOT_STARTED);

  public MigrationTaskState() {
    declareProperty(stateProp);
  }

  public State getState() {
    return stateProp.getValue();
  }

  public MigrationTaskState setState(final State state) {
    stateProp.setValue(state);
    return this;
  }

  public enum State {
    NOT_STARTED(0),
    FINISHED(1);

    byte value;

    State(final int value) {
      this.value = (byte) value;
    }
  }
}
