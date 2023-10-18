/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.topology.changes;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.future.CompletableActorFuture;

public class NoopTopologyMembershipChangeExecutor implements TopologyMembershipChangeExecutor {

  @Override
  public ActorFuture<Void> addBroker(final MemberId memberId) {
    return CompletableActorFuture.completed(null);
  }

  @Override
  public ActorFuture<Void> removeBroker(final MemberId memberId) {
    return CompletableActorFuture.completed(null);
  }
}
