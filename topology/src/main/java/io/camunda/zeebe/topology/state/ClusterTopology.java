/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.topology.state;

import com.google.common.collect.ImmutableMap;
import io.atomix.cluster.MemberId;
import io.camunda.zeebe.topology.state.MemberState.State;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents the cluster topology which describes the current active, joining or leaving brokers
 * and the partitions that each broker replicates.
 *
 * <p>version - represents the current version of the topology. It is incremented only by the
 * coordinator when a new configuration change is triggered.
 *
 * <p>members - represents the state of each member
 *
 * <p>changes - keeps track of the ongoing configuration changes
 *
 * <p>This class is immutable. Each mutable methods returns a new instance with the updated state.
 */
public record ClusterTopology(
    long version, Map<MemberId, MemberState> members, ClusterChangePlan changes) {

  private static final int UNINITIALIZED_VERSION = -1;

  public static ClusterTopology uninitialized() {
    return new ClusterTopology(UNINITIALIZED_VERSION, Map.of(), ClusterChangePlan.empty());
  }

  public boolean isUninitialized() {
    return version == UNINITIALIZED_VERSION;
  }

  public static ClusterTopology init() {
    return new ClusterTopology(0, Map.of(), ClusterChangePlan.empty());
  }

  public ClusterTopology addMember(final MemberId memberId, final MemberState state) {
    if (members.containsKey(memberId)) {
      throw new IllegalStateException(
          String.format(
              "Expected add a new member, but member %s already exists with state %s",
              memberId.id(), members.get(memberId)));
    }

    final var newMembers =
        ImmutableMap.<MemberId, MemberState>builder().putAll(members).put(memberId, state).build();
    return new ClusterTopology(version, newMembers, changes);
  }

  /**
   * Adds or updates a member in the topology.
   *
   * <p>memberStateUpdater is invoked with the current state of the member. If the member does not
   * exist, and memberStateUpdater returns a non-null value, then the member is added to the
   * topology. If the member exists, and the memberStateUpdater returns a null value, then the
   * member is removed.
   *
   * @param memberId id of the member to be updated
   * @param memberStateUpdater transforms the current state of the member to the new state
   * @return the updated ClusterTopology
   */
  public ClusterTopology updateMember(
      final MemberId memberId, final UnaryOperator<MemberState> memberStateUpdater) {
    final MemberState currentState = members.get(memberId);
    final var updateMemberState = memberStateUpdater.apply(currentState);

    if (Objects.equals(currentState, updateMemberState)) {
      return this;
    }

    final var mapBuilder = ImmutableMap.<MemberId, MemberState>builder();

    if (updateMemberState != null) {
      // Add/Update the member
      mapBuilder.putAll(members).put(memberId, updateMemberState);
    } else {
      // remove memberId from the map
      mapBuilder.putAll(
          members.entrySet().stream()
              .filter(entry -> !entry.getKey().equals(memberId))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    final var newMembers = mapBuilder.buildKeepingLast();
    return new ClusterTopology(version, newMembers, changes);
  }

  public ClusterTopology startTopologyChange(final List<TopologyChangeOperation> operations) {
    if (hasPendingChanges()) {
      throw new IllegalArgumentException(
          "Expected to start new topology change, but there is a topology change in progress "
              + changes);
    } else if (operations.isEmpty()) {
      throw new IllegalArgumentException(
          "Expected to start new topology change, but there is no operation");
    } else {
      return new ClusterTopology(version + 1, members, ClusterChangePlan.init(operations));
    }
  }

  /**
   * Returns a new ClusterTopology after merging this and other. This doesn't overwrite this or
   * other. If this.version == other.version then the new ClusterTopology contains merged members
   * and changes. Otherwise, it returns the one with the highest version.
   *
   * @param other ClusterTopology to merge
   * @return merged ClusterTopology
   */
  public ClusterTopology merge(final ClusterTopology other) {
    if (version > other.version) {
      return this;
    } else if (other.version > version) {
      return other;
    } else {
      final var mergedMembers =
          Stream.concat(members.entrySet().stream(), other.members().entrySet().stream())
              .collect(
                  Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MemberState::merge));

      final var mergedChanges = changes.merge(other.changes);
      return new ClusterTopology(version, ImmutableMap.copyOf(mergedMembers), mergedChanges);
    }
  }

  /**
   * @return true if the next operation in pending changes is applicable for the given memberId,
   *     otherwise returns false.
   */
  private boolean hasPendingChangesFor(final MemberId memberId) {
    return !changes.pendingOperations().isEmpty()
        && changes.pendingOperations().get(0).memberId().equals(memberId);
  }

  /**
   * Returns the next pending operation for the given memberId. If there is no pending operation for
   * this member, then returns an empty optional.
   *
   * @param memberId id of the member
   * @return the next pending operation for the given memberId.
   */
  public Optional<TopologyChangeOperation> pendingChangesFor(final MemberId memberId) {
    if (!hasPendingChangesFor(memberId)) {
      return Optional.empty();
    }
    return Optional.of(changes.pendingOperations().get(0));
  }

  /**
   * When the operation returned by {@link #pendingChangesFor(MemberId)} is completed, the changes
   * should be reflected in ClusterTopology by invoking this method. This removes the completed
   * operation from the pending changes and update the member state using the given updater.
   *
   * @param memberId id of the member which completed the operation
   * @param memberStateUpdater the method to update the member state
   * @return the updated ClusterTopology
   */
  public ClusterTopology advanceTopologyChange(
      final MemberId memberId, final UnaryOperator<MemberState> memberStateUpdater) {
    return updateMember(memberId, memberStateUpdater).advance();
  }

  private ClusterTopology advance() {
    if (!hasPendingChanges()) {
      throw new IllegalStateException(
          "Expected to advance the topology change, but there is no pending change");
    }
    final ClusterTopology result = new ClusterTopology(version, members, changes.advance());

    if (!result.hasPendingChanges()) {
      // The last change has been applied. Clean up the members that are marked as LEFT in the
      // topology. This operation will be executed in the member that executes the last operation.
      // This is ok because it is guaranteed that no other concurrent modification will be applied
      // to the topology. This is because all the operations are applied sequentially, and no
      // topology update will be done without adding a ClusterChangePlan.
      final var currentMembers =
          result.members().entrySet().stream()
              // remove the members that are marked as LEFT
              .filter(entry -> entry.getValue().state() != State.LEFT)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Increment the version so that other members can merge by overwriting their local topology.
      return new ClusterTopology(result.version() + 1, currentMembers, ClusterChangePlan.empty());
    }

    return result;
  }

  public boolean hasMember(final MemberId memberId) {
    return members().containsKey(memberId);
  }

  public MemberState getMember(final MemberId memberId) {
    return members().get(memberId);
  }

  public boolean hasPendingChanges() {
    return !changes.pendingOperations().isEmpty();
  }

  public int clusterSize() {
    return (int)
        members.entrySet().stream()
            .filter(
                entry ->
                    entry.getValue().state() != State.LEFT
                        && entry.getValue().state() != State.UNINITIALIZED)
            .count();
  }

  public int partitionCount() {
    return (int)
        members.values().stream().flatMap(m -> m.partitions().keySet().stream()).distinct().count();
  }
}
