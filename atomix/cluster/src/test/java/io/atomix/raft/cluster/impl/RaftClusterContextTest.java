/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.atomix.raft.cluster.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.atomix.cluster.MemberId;
import io.atomix.raft.cluster.RaftMember;
import io.atomix.raft.cluster.RaftMember.Type;
import io.atomix.raft.impl.RaftContext;
import io.atomix.raft.storage.system.Configuration;
import io.atomix.raft.storage.system.MetaStore;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class RaftClusterContextTest {

  @Test
  void shouldConfigureFromStored() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var remoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var members = Stream.concat(Stream.of(localMember), remoteMembers.stream()).toList();

    final var configuration = new Configuration(1, 1, Instant.now().toEpochMilli(), members);
    final var raft = raftWithStoredConfiguration(configuration);
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // then
    assertThat(context.getLocalMember().memberId()).isEqualTo(MemberId.from("1"));
    assertThat(context.inJointConsensus()).isFalse();
    assertThat(context.getMembers()).containsAll(members);
    assertThat(context.isSingleMemberCluster()).isFalse();
    assertThat(context.getVotingMembers()).containsAll(remoteMembers);
    assertThat(
            context.getReplicationTargets().stream()
                .map(RaftMemberContext::getMember)
                .map(RaftMember.class::cast))
        .containsAll(remoteMembers);
    assertThat(members)
        .allMatch(member -> context.isMember(member.memberId()))
        .allSatisfy(member -> assertThat(context.getMember(member.memberId())).isNotNull());
    assertThat(remoteMembers)
        .allSatisfy(member -> assertThat(context.getMemberContext(member.memberId())).isNotNull());
  }

  @Test
  void shouldReconfigureOverStored() {
    // given -- stored configuration that only contains the local member
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var remoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var members = Stream.concat(Stream.of(localMember), remoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), List.of(localMember)));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when -- reconfigure with a new configuration that contains all members
    context.configure(new Configuration(2, 1, Instant.now().toEpochMilli(), members));

    // then -- new configuration is used
    assertThat(context.getLocalMember().memberId()).isEqualTo(MemberId.from("1"));
    assertThat(context.inJointConsensus()).isFalse();
    assertThat(context.getMembers()).containsAll(members);
    assertThat(context.isSingleMemberCluster()).isFalse();
    assertThat(context.getVotingMembers()).containsAll(remoteMembers);
    assertThat(
            context.getReplicationTargets().stream()
                .map(RaftMemberContext::getMember)
                .map(RaftMember.class::cast))
        .containsAll(remoteMembers);
    assertThat(members)
        .allMatch(member -> context.isMember(member.memberId()))
        .allSatisfy(member -> assertThat(context.getMember(member.memberId())).isNotNull());
    assertThat(remoteMembers)
        .allSatisfy(member -> assertThat(context.getMemberContext(member.memberId())).isNotNull());
  }

  @Test
  void shouldRemoveContextsOnReconfiguration() {
    // given -- stored configuration that contains all members
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var remoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var members = Stream.concat(Stream.of(localMember), remoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(new Configuration(1, 1, Instant.now().toEpochMilli(), members));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when -- reconfigure with a new configuration only contains the local member
    context.configure(new Configuration(2, 1, Instant.now().toEpochMilli(), List.of(localMember)));

    // then -- new configuration is used
    assertThat(context.getLocalMember().memberId()).isEqualTo(MemberId.from("1"));
    assertThat(context.inJointConsensus()).isFalse();
    assertThat(context.getMembers()).containsExactly(localMember);
    assertThat(context.isSingleMemberCluster()).isTrue();
    assertThat(context.getVotingMembers()).isEmpty();
    assertThat(context.getReplicationTargets()).isEmpty();
    assertThat(remoteMembers)
        .noneMatch(member -> context.isMember(member.memberId()))
        .allSatisfy(member -> assertThat(context.getMember(member.memberId())).isNull())
        .allSatisfy(member -> assertThat(context.getMemberContext(member.memberId())).isNull());
  }

  @Test
  void shouldUpdateMemberType() {
    // given -- stored configuration that contains all members
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var oldRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var oldMembers =
        Stream.concat(Stream.of(localMember), oldRemoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when -- reconfigure with a new configuration only contains the local member
    final var newLocalMember =
        new DefaultRaftMember(new MemberId("1"), Type.PASSIVE, Instant.now());
    final var newRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.PASSIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.PASSIVE, Instant.now()));
    final var newMembers =
        Stream.concat(Stream.of(newLocalMember), newRemoteMembers.stream()).toList();

    context.configure(
        new Configuration(2, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));

    // then -- new configuration is used
    assertThat(context.getLocalMember().memberId()).isEqualTo(MemberId.from("1"));
    assertThat(context.getLocalMember().getType()).isEqualTo(Type.PASSIVE);
    assertThat(context.isSingleMemberCluster()).isFalse();
    assertThat(context.inJointConsensus()).isTrue();
    assertThat(context.getMembers()).containsExactlyInAnyOrderElementsOf(newMembers);
    assertThat(newMembers)
        .allMatch(member -> context.isMember(member.memberId()))
        .allSatisfy(
            member ->
                assertThat(context.getMember(member.memberId()).getType()).isEqualTo(Type.PASSIVE));
  }

  @Test
  void shouldCountVoteFromLocalMember() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var remoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var members = Stream.concat(Stream.of(localMember), remoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(new Configuration(1, 1, Instant.now().toEpochMilli(), members));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when
    final Consumer<Boolean> callback = mock();
    final var quorum = context.getVoteQuorum(callback);

    quorum.succeed(new MemberId("2"));

    // then
    verify(callback).accept(true);
  }

  @Test
  void shouldRequireJointConsensusVotes() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var oldRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var oldMembers =
        Stream.concat(Stream.of(localMember), oldRemoteMembers.stream()).toList();
    final var newRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("4"), Type.ACTIVE, Instant.now()));
    final var newMembers =
        Stream.concat(Stream.of(localMember), newRemoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when
    final Consumer<Boolean> callback = mock();
    final var quorum = context.getVoteQuorum(callback);

    // then
    quorum.succeed(new MemberId("4"));
    verifyNoInteractions(callback);

    quorum.succeed(new MemberId("2"));
    verify(callback).accept(true);
  }

  @Test
  void shouldCalculateQuorum() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var remoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("4"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("5"), Type.ACTIVE, Instant.now()));
    final var members = Stream.concat(Stream.of(localMember), remoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(new Configuration(1, 1, Instant.now().toEpochMilli(), members));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when
    context.getMemberContext(new MemberId("2")).setMatchIndex(2);
    context.getMemberContext(new MemberId("3")).setMatchIndex(3);
    context.getMemberContext(new MemberId("4")).setMatchIndex(4);
    context.getMemberContext(new MemberId("5")).setMatchIndex(5);

    // then
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(4L);
  }

  @Test
  void quorumWhenNewMembersAreAhead() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var oldRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var oldMembers =
        Stream.concat(Stream.of(localMember), oldRemoteMembers.stream()).toList();
    final var newRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("4"), Type.ACTIVE, Instant.now()));
    final var newMembers =
        Stream.concat(Stream.of(localMember), newRemoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when
    for (final var member : newRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(5);
    }

    for (final var member : oldRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(2);
    }

    // then
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(2L);
  }

  @Test
  void quorumWhenOldMembersAreAhead() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var oldRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var oldMembers =
        Stream.concat(Stream.of(localMember), oldRemoteMembers.stream()).toList();
    final var newRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("4"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("5"), Type.ACTIVE, Instant.now()));
    final var newMembers =
        Stream.concat(Stream.of(localMember), newRemoteMembers.stream()).toList();

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when
    for (final var member : newRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(2);
    }

    for (final var member : oldRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(5);
    }

    // then
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(2L);
  }

  @Test
  void quorumWhenClusterBecomesSingleMember() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var oldRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var oldMembers =
        Stream.concat(Stream.of(localMember), oldRemoteMembers.stream()).toList();
    final var newMembers = List.<RaftMember>of(localMember);

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when

    for (final var member : oldRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(5);
    }

    // then
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(5L);
  }

  @Test
  void quorumWhenClusterWasSingleMember() {
    // given
    final var localMember = new DefaultRaftMember(new MemberId("1"), Type.ACTIVE, Instant.now());
    final var newRemoteMembers =
        List.<RaftMember>of(
            new DefaultRaftMember(new MemberId("2"), Type.ACTIVE, Instant.now()),
            new DefaultRaftMember(new MemberId("3"), Type.ACTIVE, Instant.now()));
    final var newMembers =
        Stream.concat(Stream.of(localMember), newRemoteMembers.stream()).toList();
    final var oldMembers = List.<RaftMember>of(localMember);

    final var raft =
        raftWithStoredConfiguration(
            new Configuration(1, 1, Instant.now().toEpochMilli(), newMembers, oldMembers));
    final var context = new RaftClusterContext(localMember.memberId(), raft);

    // when

    for (final var member : newRemoteMembers) {
      context.getMemberContext(member.memberId()).setMatchIndex(5);
    }

    // then
    assertThat(context.getQuorumFor(RaftMemberContext::getMatchIndex)).hasValue(5L);
  }

  private RaftContext raftWithStoredConfiguration(final Configuration configuration) {
    final var raft = mock(RaftContext.class, withSettings().stubOnly());
    final var metaStore = mock(MetaStore.class, withSettings().stubOnly());
    when(metaStore.loadConfiguration()).thenReturn(configuration);
    when(raft.getMetaStore()).thenReturn(metaStore);
    return raft;
  }
}
