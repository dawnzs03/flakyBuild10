/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.migration.to_8_3;

import static io.camunda.zeebe.util.buffer.BufferUtil.bufferAsString;
import static io.camunda.zeebe.util.buffer.BufferUtil.wrapString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.engine.EngineConfiguration;
import io.camunda.zeebe.engine.state.deployment.DbDecisionState;
import io.camunda.zeebe.engine.state.deployment.DbProcessState;
import io.camunda.zeebe.engine.state.deployment.DeployedDrg;
import io.camunda.zeebe.engine.state.deployment.DeployedProcess;
import io.camunda.zeebe.engine.state.deployment.PersistedDecision;
import io.camunda.zeebe.engine.state.deployment.PersistedProcess.PersistedProcessState;
import io.camunda.zeebe.engine.state.immutable.MigrationState;
import io.camunda.zeebe.engine.state.immutable.ProcessingState;
import io.camunda.zeebe.engine.state.migration.to_8_3.legacy.LegacyDecisionState;
import io.camunda.zeebe.engine.state.migration.to_8_3.legacy.LegacyProcessState;
import io.camunda.zeebe.engine.state.mutable.MutableProcessingState;
import io.camunda.zeebe.engine.util.ProcessingStateExtension;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DecisionRecord;
import io.camunda.zeebe.protocol.impl.record.value.deployment.DecisionRequirementsRecord;
import io.camunda.zeebe.protocol.impl.record.value.deployment.ProcessRecord;
import io.camunda.zeebe.protocol.record.value.TenantOwned;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public class MultiTenancyMigrationTest {

  final MultiTenancyMigration sut = new MultiTenancyMigration();

  @Nested
  class MockBasedTests {

    @Test
    void migrationNeededWhenMigrationNotFinished() {
      // given
      final var mockProcessingState = mock(ProcessingState.class);
      final var migrationState = mock(MigrationState.class);
      when(mockProcessingState.getMigrationState()).thenReturn(migrationState);
      when(migrationState.isMigrationFinished(anyString())).thenReturn(false);

      // when
      final var actual = sut.needsToRun(mockProcessingState);

      // then
      assertThat(actual).isTrue();
    }
  }

  @Nested
  @ExtendWith(ProcessingStateExtension.class)
  class MigrateProcessStateForMultiTenancyTest {

    private ZeebeDb<ZbColumnFamilies> zeebeDb;
    private MutableProcessingState processingState;
    private TransactionContext transactionContext;

    private LegacyProcessState legacyState;
    private DbProcessState processState;

    @BeforeEach
    void setup() {
      legacyState = new LegacyProcessState(zeebeDb, transactionContext);
      processState = new DbProcessState(zeebeDb, transactionContext);
    }

    @Test
    void shouldMigrateProcessColumnFamily() {
      // given
      final var model = Bpmn.createExecutableProcess("processId").startEvent().done();
      legacyState.putProcess(
          123,
          new ProcessRecord()
              .setKey(123)
              .setBpmnProcessId("processId")
              .setVersion(1)
              .setResourceName("resourceName")
              .setResource(wrapString(Bpmn.convertToString(model)))
              .setChecksum(wrapString("checksum")));

      // when
      sut.runMigration(processingState);

      // then
      assertProcessPersisted(
          processState.getProcessByKeyAndTenant(123, TenantOwned.DEFAULT_TENANT_IDENTIFIER),
          new PersistedProcess(
              "processId",
              1,
              PersistedProcessState.ACTIVE,
              "resourceName",
              TenantOwned.DEFAULT_TENANT_IDENTIFIER,
              model));
      assertThat(legacyState.getProcessByKey(123)).isNull();
    }

    @Test
    void shouldMigrateProcessByIdAndVersionColumnFamily() {
      // given
      final var model = Bpmn.createExecutableProcess("processId").startEvent().done();
      legacyState.putProcess(
          123,
          new ProcessRecord()
              .setKey(123)
              .setBpmnProcessId("processId")
              .setVersion(1)
              .setResourceName("resourceName")
              .setResource(wrapString(Bpmn.convertToString(model)))
              .setChecksum(wrapString("checksum")));

      // when
      sut.runMigration(processingState);

      // then
      assertProcessPersisted(
          processState.getProcessByProcessIdAndVersion(
              wrapString("processId"), 1, TenantOwned.DEFAULT_TENANT_IDENTIFIER),
          new PersistedProcess(
              "processId",
              1,
              PersistedProcessState.ACTIVE,
              "resourceName",
              TenantOwned.DEFAULT_TENANT_IDENTIFIER,
              model));
      assertThat(legacyState.getProcessByProcessIdAndVersion(wrapString("processId"), 1)).isNull();
    }

    @Test
    void shouldMigrateProcessByIdAndVersionColumnFamilyUsingVersionManager() {
      // given
      final var model = Bpmn.createExecutableProcess("processId").startEvent().done();
      legacyState.putProcess(
          123,
          new ProcessRecord()
              .setKey(123)
              .setBpmnProcessId("processId")
              .setVersion(1)
              .setResourceName("resourceName")
              .setResource(wrapString(Bpmn.convertToString(model)))
              .setChecksum(wrapString("checksum")));
      // the version manager must be migrated first to ensure that the known versions are set
      new ProcessDefinitionVersionMigration().runMigration(processingState);

      // when
      sut.runMigration(processingState);

      // then
      assertProcessPersisted(
          processState.getLatestProcessVersionByProcessId(
              wrapString("processId"), TenantOwned.DEFAULT_TENANT_IDENTIFIER),
          new PersistedProcess(
              "processId",
              1,
              PersistedProcessState.ACTIVE,
              "resourceName",
              TenantOwned.DEFAULT_TENANT_IDENTIFIER,
              model));
      assertThat(legacyState.getLatestProcessVersionByProcessId(wrapString("processId"))).isNull();
    }

    @Test
    void shouldMigrateDigestByIdColumnFamily() {
      // given
      final var model = Bpmn.createExecutableProcess("processId").startEvent().done();
      legacyState.putProcess(
          123,
          new ProcessRecord()
              .setKey(123)
              .setBpmnProcessId("processId")
              .setVersion(1)
              .setResourceName("resourceName")
              .setResource(wrapString(Bpmn.convertToString(model)))
              .setChecksum(wrapString("checksum")));

      // when
      sut.runMigration(processingState);

      // then
      assertThat(
              processState.getLatestVersionDigest(
                  wrapString("processId"), TenantOwned.DEFAULT_TENANT_IDENTIFIER))
          .extracting(BufferUtil::bufferAsString)
          .isEqualTo("checksum");
      assertThat(legacyState.getLatestVersionDigest(wrapString("processId"))).isNull();
    }

    void assertProcessPersisted(final DeployedProcess actual, final PersistedProcess expected) {
      assertThat(actual)
          .extracting(
              p -> bufferAsString(p.getBpmnProcessId()),
              DeployedProcess::getVersion,
              DeployedProcess::getState,
              p -> bufferAsString(p.getResourceName()),
              DeployedProcess::getTenantId,
              p -> bufferAsString(p.getResource()))
          .containsExactly(
              expected.bpmnProcessId(),
              expected.version(),
              expected.state(),
              expected.resourceName(),
              expected.tenantId(),
              Bpmn.convertToString(expected.model()));
    }

    record PersistedProcess(
        String bpmnProcessId,
        int version,
        PersistedProcessState state,
        String resourceName,
        String tenantId,
        BpmnModelInstance model) {}
  }

  @Nested
  @ExtendWith(ProcessingStateExtension.class)
  class MigrateDecisionStateForMultiTenancyTest {

    private ZeebeDb<ZbColumnFamilies> zeebeDb;
    private MutableProcessingState processingState;
    private TransactionContext transactionContext;

    private LegacyDecisionState legacyState;
    private DbDecisionState decisionState;

    @BeforeEach
    void setup() {
      final var cfg = new EngineConfiguration();
      legacyState = new LegacyDecisionState(zeebeDb, transactionContext, cfg);
      decisionState = new DbDecisionState(zeebeDb, transactionContext, cfg);
    }

    @Test
    void shouldMigrateDecisionsByKeyColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));

      sut.runMigration(processingState);

      final PersistedDecision persistedDecision =
          decisionState
              .findDecisionByTenantAndKey(TenantOwned.DEFAULT_TENANT_IDENTIFIER, 456)
              .orElseThrow();
      assertThat(bufferAsString(persistedDecision.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(persistedDecision.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(persistedDecision.getDecisionId())).isEqualTo("decisionId");
      assertThat(bufferAsString(persistedDecision.getDecisionName())).isEqualTo("decisionName");
      assertThat(persistedDecision.getDecisionKey()).isEqualTo(456L);
      assertThat(persistedDecision.getVersion()).isEqualTo(1);
      assertThat(persistedDecision.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }

    @Test
    void shouldMigrateDecisionRequirementsByKeyColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));

      sut.runMigration(processingState);

      final DeployedDrg deployedDrg =
          decisionState
              .findDecisionRequirementsByTenantAndKey(TenantOwned.DEFAULT_TENANT_IDENTIFIER, 123)
              .orElseThrow();
      assertThat(bufferAsString(deployedDrg.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(deployedDrg.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(deployedDrg.getDecisionRequirementsName())).isEqualTo("drgName");
      assertThat(deployedDrg.getDecisionRequirementsVersion()).isEqualTo(1);
      assertThat(bufferAsString(deployedDrg.getResourceName())).isEqualTo("resourceName");
      assertThat(bufferAsString(deployedDrg.getResource())).isEqualTo("resource");
      assertThat(bufferAsString(deployedDrg.getChecksum())).isEqualTo("checksum");
      assertThat(deployedDrg.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }

    @Test
    void shouldMigrateDecisionKeyByDecisionRequirementsKeyColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));

      sut.runMigration(processingState);

      final List<PersistedDecision> persistedDecisions =
          decisionState.findDecisionsByTenantAndDecisionRequirementsKey(
              TenantOwned.DEFAULT_TENANT_IDENTIFIER, 123);
      assertThat(persistedDecisions).hasSize(1);

      final PersistedDecision persistedDecision = persistedDecisions.get(0);
      assertThat(bufferAsString(persistedDecision.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(persistedDecision.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(persistedDecision.getDecisionId())).isEqualTo("decisionId");
      assertThat(bufferAsString(persistedDecision.getDecisionName())).isEqualTo("decisionName");
      assertThat(persistedDecision.getDecisionKey()).isEqualTo(456L);
      assertThat(persistedDecision.getVersion()).isEqualTo(1);
      assertThat(persistedDecision.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }

    @Test
    void shouldMigrateLatestDecisionKeysByDecisionIdColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));

      sut.runMigration(processingState);

      final PersistedDecision persistedDecision =
          decisionState
              .findLatestDecisionByIdAndTenant(
                  wrapString("decisionId"), TenantOwned.DEFAULT_TENANT_IDENTIFIER)
              .orElseThrow();
      assertThat(bufferAsString(persistedDecision.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(persistedDecision.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(persistedDecision.getDecisionId())).isEqualTo("decisionId");
      assertThat(bufferAsString(persistedDecision.getDecisionName())).isEqualTo("decisionName");
      assertThat(persistedDecision.getDecisionKey()).isEqualTo(456L);
      assertThat(persistedDecision.getVersion()).isEqualTo(1);
      assertThat(persistedDecision.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }

    @Test
    void shouldMigrateLatestDecisionRequirementsKeysByIdColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));

      sut.runMigration(processingState);

      final DeployedDrg deployedDrg =
          decisionState
              .findLatestDecisionRequirementsByTenantAndId(
                  TenantOwned.DEFAULT_TENANT_IDENTIFIER, wrapString("drgId"))
              .orElseThrow();
      assertThat(bufferAsString(deployedDrg.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(deployedDrg.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(deployedDrg.getDecisionRequirementsName())).isEqualTo("drgName");
      assertThat(deployedDrg.getDecisionRequirementsVersion()).isEqualTo(1);
      assertThat(bufferAsString(deployedDrg.getResourceName())).isEqualTo("resourceName");
      assertThat(bufferAsString(deployedDrg.getResource())).isEqualTo("resource");
      assertThat(bufferAsString(deployedDrg.getChecksum())).isEqualTo("checksum");
      assertThat(deployedDrg.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }

    @Test
    void shouldMigrateDecisionKeyByDecisionIdAndVersionColumnFamily() {
      legacyState.storeDecisionRequirements(
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(1)
              .setDecisionRequirementsKey(123)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource"))
              .setChecksum(wrapString("checksum")));
      legacyState.storeDecisionRecord(
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(123)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(1)
              .setDecisionKey(456)
              .setTenantId(""));
      final DecisionRequirementsRecord drgV2 =
          new DecisionRequirementsRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsName("drgName")
              .setDecisionRequirementsVersion(2)
              .setDecisionRequirementsKey(234)
              .setNamespace("namespace")
              .setResourceName("resourceName")
              .setResource(wrapString("resource2"))
              .setChecksum(wrapString("checksum2"));
      legacyState.storeDecisionRequirements(drgV2);
      final DecisionRecord decisionV2 =
          new DecisionRecord()
              .setDecisionRequirementsId("drgId")
              .setDecisionRequirementsKey(234)
              .setDecisionId("decisionId")
              .setDecisionName("decisionName")
              .setVersion(2)
              .setDecisionKey(567)
              .setTenantId("");
      legacyState.storeDecisionRecord(decisionV2);

      // when
      sut.runMigration(processingState);

      // then

      // by deleting the second version, we use decisionKeyByDecisionIdAndVersion to find the
      // new latest drg of the decision. We can then use this to make our assertion below
      decisionState.deleteDecision(decisionV2.setTenantId(TenantOwned.DEFAULT_TENANT_IDENTIFIER));
      decisionState.deleteDecisionRequirements(
          drgV2.setTenantId(TenantOwned.DEFAULT_TENANT_IDENTIFIER));

      final PersistedDecision persistedDecision =
          decisionState
              .findLatestDecisionByIdAndTenant(
                  wrapString("decisionId"), TenantOwned.DEFAULT_TENANT_IDENTIFIER)
              .orElseThrow();
      assertThat(bufferAsString(persistedDecision.getDecisionRequirementsId())).isEqualTo("drgId");
      assertThat(persistedDecision.getDecisionRequirementsKey()).isEqualTo(123L);
      assertThat(bufferAsString(persistedDecision.getDecisionId())).isEqualTo("decisionId");
      assertThat(bufferAsString(persistedDecision.getDecisionName())).isEqualTo("decisionName");
      assertThat(persistedDecision.getDecisionKey()).isEqualTo(456L);
      assertThat(persistedDecision.getVersion()).isEqualTo(1);
      assertThat(persistedDecision.getTenantId()).isEqualTo(TenantOwned.DEFAULT_TENANT_IDENTIFIER);
    }
  }
}
