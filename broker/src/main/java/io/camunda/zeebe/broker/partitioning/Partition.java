/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.broker.partitioning;

import io.atomix.raft.partition.RaftPartition;
import io.camunda.zeebe.broker.partitioning.startup.PartitionStartupContext;
import io.camunda.zeebe.broker.partitioning.startup.steps.PartitionDirectoryStep;
import io.camunda.zeebe.broker.partitioning.startup.steps.PartitionRegistrationStep;
import io.camunda.zeebe.broker.partitioning.startup.steps.RaftBootstrapStep;
import io.camunda.zeebe.broker.partitioning.startup.steps.RaftJoinStep;
import io.camunda.zeebe.broker.partitioning.startup.steps.SnapshotStoreStep;
import io.camunda.zeebe.broker.partitioning.startup.steps.ZeebePartitionStep;
import io.camunda.zeebe.broker.system.partitions.ZeebePartition;
import io.camunda.zeebe.scheduler.future.ActorFuture;
import io.camunda.zeebe.scheduler.startup.StartupProcess;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the combination of a Raft and Zeebe partition. For now, the only way to construct
 * partitions is through the {@link #bootstrapping(PartitionStartupContext)} method.
 *
 * <pre>
 *   final var context = new PartitionStartupContext(...);
 *   final var partition = Partition.bootstrapping(context);
 *
 *   partition.start().join();
 *   partition.stop().join();
 * </pre>
 */
public final class Partition {
  private static final Logger LOGGER = LoggerFactory.getLogger(Partition.class);

  private final PartitionStartupContext context;
  private final StartupProcess<PartitionStartupContext> startupProcess;

  private Partition(
      final PartitionStartupContext context,
      final StartupProcess<PartitionStartupContext> startupProcess) {
    this.context = context;
    this.startupProcess = startupProcess;
  }

  /**
   * Creates a partition that uses the bootstrapping process when started. Bootstrapping assumes
   * that this broker is already part of the replication group for this partition. If the broker
   * does not have the partition configuration stored locally, the initial configuration is derived
   * from the static broker configuration.
   *
   * @param context a populated context that the partition can use.
   * @return a partition that can be started.
   */
  public static Partition bootstrapping(final PartitionStartupContext context) {
    return new Partition(
        context,
        new StartupProcess<>(
            LOGGER,
            List.of(
                new PartitionDirectoryStep(),
                new SnapshotStoreStep(),
                new RaftBootstrapStep(),
                new ZeebePartitionStep(),
                new PartitionRegistrationStep())));
  }

  public static Partition joining(final PartitionStartupContext context) {
    return new Partition(
        context,
        new StartupProcess<>(
            List.of(
                new PartitionDirectoryStep(),
                new SnapshotStoreStep(),
                new RaftJoinStep(),
                new ZeebePartitionStep(),
                new PartitionRegistrationStep())));
  }

  public ActorFuture<Partition> start() {
    final var concurrencyControl = context.concurrencyControl();
    final var result = concurrencyControl.<Partition>createFuture();
    concurrencyControl.run(
        () -> {
          final var start = startupProcess.startup(concurrencyControl, context);
          concurrencyControl.runOnCompletion(
              start,
              (ok, error) -> {
                if (error != null) {
                  result.completeExceptionally(error);
                } else {
                  result.complete(this);
                }
              });
        });
    return result;
  }

  public ActorFuture<Partition> stop() {
    final var concurrencyControl = context.concurrencyControl();
    final var result = concurrencyControl.<Partition>createFuture();
    concurrencyControl.run(
        () -> {
          final var start = startupProcess.shutdown(concurrencyControl, context);
          concurrencyControl.runOnCompletion(
              start,
              (ok, error) -> {
                if (error != null) {
                  result.completeExceptionally(error);
                } else {
                  result.complete(this);
                }
              });
        });
    return result;
  }

  public ZeebePartition zeebePartition() {
    return context.zeebePartition();
  }

  public RaftPartition raftPartition() {
    return context.raftPartition();
  }

  public int id() {
    return context.partitionMetadata().id().id();
  }
}
