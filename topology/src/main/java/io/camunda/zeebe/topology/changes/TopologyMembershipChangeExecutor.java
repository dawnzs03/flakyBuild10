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

public interface TopologyMembershipChangeExecutor {

  /**
   * The implementation of this method can react to a new node joining the cluster. For example,
   * this can update BrokerInfo so that the gateway can see the new clusterSize.
   *
   * @param memberId id of the broker that is newly added to the cluster
   * @return future when the operation is completed.
   */
  ActorFuture<Void> addBroker(MemberId memberId);

  /**
   * The implementation of this method can react to a node leaving the cluster.
   *
   * @param memberId id of the member that is leaving the cluster
   * @return future when the operation is completed
   */
  ActorFuture<Void> removeBroker(MemberId memberId);
}
