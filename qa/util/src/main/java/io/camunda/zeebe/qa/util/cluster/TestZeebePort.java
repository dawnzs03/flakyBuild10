/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.qa.util.cluster;

/** Represents the known ports in Zeebe and their default values. */
public enum TestZeebePort {
  /** Port of the command API, i.e. the port used by the gateway to communicate with the broker */
  COMMAND(26501),
  /** Port of the gateway API, i.e. the port used by the client to communicate with any gateway */
  GATEWAY(26500),
  /** Port for internal communication, i.e. what all nodes use to communicate for clustering */
  CLUSTER(26502),
  /** Port for the management server, i.e. actuators, metrics, etc. */
  MONITORING(9600);

  private final int port;

  TestZeebePort(final int port) {
    this.port = port;
  }

  /** Returns the default port number for this port */
  public int port() {
    return port;
  }
}
