/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.DeploymentIntent;
import io.camunda.zeebe.protocol.record.intent.FormIntent;
import io.camunda.zeebe.protocol.record.value.deployment.Form;
import io.camunda.zeebe.protocol.record.value.deployment.FormMetadataValue;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Rule;
import org.junit.Test;

public class FormDeploymentTest {

  private static final String TEST_FORM_1 = "/form/test-form-1.form";
  private static final String TEST_FORM_1_V2 = "/form/test-form-1_v2.form";
  private static final String TEST_FORM_2 = "/form/test-form-2.form";
  private static final String TEST_FORM_WITHOUT_ID = "/form/test-form_without_id.form";
  private static final String TEST_FORM_WITH_BLANK_ID = "/form/test-form_with_blank_id.form";

  @Rule public final EngineRule engine = EngineRule.singlePartition();

  @Test
  public void shouldDeployFormResource() {
    // when
    final var deploymentEvent = engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    // then
    Assertions.assertThat(deploymentEvent)
        .hasIntent(DeploymentIntent.CREATED)
        .hasValueType(ValueType.DEPLOYMENT)
        .hasRecordType(RecordType.EVENT);
  }

  @Test
  public void shouldRejectWhenFormIdIsMissing() {
    // when
    final var deploymentEvent =
        engine
            .deployment()
            .withJsonClasspathResource(TEST_FORM_WITHOUT_ID)
            .expectRejection()
            .deploy();

    // then
    Assertions.assertThat(deploymentEvent)
        .hasIntent(DeploymentIntent.CREATE)
        .hasRecordType(RecordType.COMMAND_REJECTION)
        .hasRejectionType(RejectionType.INVALID_ARGUMENT);

    assertThat(deploymentEvent.getRejectionReason())
        .contains(String.format("Expected the form id to be present, but none given"));
  }

  @Test
  public void shouldRejectWhenFormIdIsBlank() {
    // when
    final var deploymentEvent =
        engine
            .deployment()
            .withJsonClasspathResource(TEST_FORM_WITH_BLANK_ID)
            .expectRejection()
            .deploy();

    // then
    Assertions.assertThat(deploymentEvent)
        .hasIntent(DeploymentIntent.CREATE)
        .hasRecordType(RecordType.COMMAND_REJECTION)
        .hasRejectionType(RejectionType.INVALID_ARGUMENT);

    assertThat(deploymentEvent.getRejectionReason())
        .contains(String.format("Expected the form id to be filled, but it is blank"));
  }

  @Test
  public void shouldWriteFormRecord() {
    // when
    engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    // then
    final Record<Form> record = RecordingExporter.formRecords().getFirst();

    Assertions.assertThat(record)
        .hasIntent(FormIntent.CREATED)
        .hasValueType(ValueType.FORM)
        .hasRecordType(RecordType.EVENT);

    assertThat(record.getKey()).isPositive();

    final Form formRecord = record.getValue();
    Assertions.assertThat(formRecord)
        .hasFormId("Form_0w7r08e")
        .hasResourceName(TEST_FORM_1)
        .hasVersion(1);

    assertThat(formRecord.getFormKey()).isPositive();
    assertThat(formRecord.isDuplicate()).isFalse();
  }

  @Test
  public void shouldDeployDuplicateInSeparateCommand() {
    // given
    final var firstDeployment = engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    final var formV1 = firstDeployment.getValue().getFormMetadata().get(0);

    // when
    final var secondDeployment =
        engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    // then
    assertThat(secondDeployment.getValue().getFormMetadata()).hasSize(1);

    final var formMetadata = secondDeployment.getValue().getFormMetadata().get(0);
    Assertions.assertThat(formMetadata).hasVersion(1).hasFormKey(formV1.getFormKey()).isDuplicate();
  }

  @Test
  public void shouldOmitRecordsForDuplicate() {
    // given
    engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    // when
    engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    engine.deployment().withJsonClasspathResource(TEST_FORM_1_V2).deploy();

    // then
    assertThat(RecordingExporter.formRecords().limit(2))
        .extracting(Record::getValue)
        .extracting(FormMetadataValue::getVersion)
        .describedAs("Expect to omit form record for duplicate")
        .containsExactly(1, 2);
  }

  @Test
  public void shouldIncreaseVersionIfResourceNameDiffers() {
    // given
    final var formResource = readResource(TEST_FORM_1);
    engine.deployment().withJsonResource(formResource, "test-form-1.form").deploy();

    // when
    final var deploymentEvent =
        engine.deployment().withJsonResource(formResource, "renamed-test-form-1.form").deploy();

    // then
    assertThat(deploymentEvent.getValue().getFormMetadata())
        .extracting(FormMetadataValue::getVersion)
        .describedAs("Expect that the Form version is increased")
        .containsExactly(2);
  }

  @Test
  public void shouldIncreaseVersionIfFormJSONDiffers() {
    // given
    engine.deployment().withJsonClasspathResource(TEST_FORM_1).deploy();

    // when
    final var deploymentEvent =
        engine.deployment().withJsonClasspathResource(TEST_FORM_1_V2).deploy();

    // then
    assertThat(deploymentEvent.getValue().getFormMetadata())
        .extracting(FormMetadataValue::getVersion)
        .describedAs("Expect that the Form version is increased")
        .containsExactly(2);

    assertThat(RecordingExporter.formRecords().limit(2))
        .hasSize(2)
        .extracting(Record::getValue)
        .extracting(FormMetadataValue::getFormId, FormMetadataValue::getVersion)
        .contains(tuple("Form_0w7r08e", 1), tuple("Form_0w7r08e", 2));
  }

  @Test
  public void shouldSetInitialVersionForDifferentFormIds() {
    // given
    final var deploymentEvent1 = engine.deployment().withXmlClasspathResource(TEST_FORM_1).deploy();

    // when
    final var deploymentEvent2 = engine.deployment().withXmlClasspathResource(TEST_FORM_2).deploy();

    // then
    assertThat(deploymentEvent1.getValue().getFormMetadata())
        .extracting(FormMetadataValue::getVersion)
        .describedAs("Expect that the Form version is 1")
        .containsExactly(1);

    assertThat(deploymentEvent2.getValue().getFormMetadata())
        .extracting(FormMetadataValue::getVersion)
        .describedAs("Expect that the Form version is 1")
        .containsExactly(1);

    assertThat(RecordingExporter.formRecords().limit(2))
        .hasSize(2)
        .extracting(Record::getValue)
        .extracting(FormMetadataValue::getFormId, FormMetadataValue::getVersion)
        .contains(tuple("Form_0w7r08e", 1), tuple("Form_6s1b76p", 1));
  }

  @Test
  public void shouldDeployIfMultipleFormsHaveDifferentId() {
    // when
    final var deploymentEvent =
        engine
            .deployment()
            .withXmlClasspathResource(TEST_FORM_1)
            .withXmlClasspathResource(TEST_FORM_2)
            .deploy();

    // then
    Assertions.assertThat(deploymentEvent)
        .hasIntent(DeploymentIntent.CREATED)
        .hasValueType(ValueType.DEPLOYMENT)
        .hasRecordType(RecordType.EVENT);

    assertThat(deploymentEvent.getValue().getFormMetadata()).hasSize(2);

    final var formMetadata1 = deploymentEvent.getValue().getFormMetadata().get(0);
    Assertions.assertThat(formMetadata1).hasFormId("Form_0w7r08e");
    assertThat(formMetadata1.getFormKey()).isPositive();
    assertThat(formMetadata1.getChecksum())
        .describedAs("Expect the MD5 checksum of the Form resource")
        .isEqualTo(getChecksum(TEST_FORM_1));

    final var formMetadata2 = deploymentEvent.getValue().getFormMetadata().get(1);
    Assertions.assertThat(formMetadata2).hasFormId("Form_6s1b76p");
    assertThat(formMetadata2.getFormKey()).isPositive();
    assertThat(formMetadata2.getChecksum())
        .describedAs("Expect the MD5 checksum of the DMN resource")
        .isEqualTo(getChecksum(TEST_FORM_2));
  }

  @Test
  public void shouldRejectIfMultipleFormHaveTheSameId() {
    // when
    final var deploymentEvent =
        engine
            .deployment()
            .withXmlClasspathResource(TEST_FORM_1)
            .withXmlClasspathResource(TEST_FORM_1_V2)
            .expectRejection()
            .deploy();

    // then
    Assertions.assertThat(deploymentEvent)
        .hasIntent(DeploymentIntent.CREATE)
        .hasRecordType(RecordType.COMMAND_REJECTION)
        .hasRejectionType(RejectionType.INVALID_ARGUMENT);

    assertThat(deploymentEvent.getRejectionReason())
        .contains(
            String.format(
                "Expected the form ids to be unique within a deployment"
                    + " but found a duplicated id '%s' in the resources '%s' and '%s'.",
                "Form_0w7r08e", TEST_FORM_1, TEST_FORM_1_V2));
  }

  private byte[] readResource(final String resourceName) {
    final var resourceAsStream = getClass().getResourceAsStream(resourceName);
    assertThat(resourceAsStream).isNotNull();

    try {
      return resourceAsStream.readAllBytes();
    } catch (final IOException e) {
      fail("Failed to read resource '{}'", resourceName, e);
      return new byte[0];
    }
  }

  private byte[] getChecksum(final String resourceName) {
    var checksum = new byte[0];
    try {
      final byte[] resource = readResource(resourceName);
      final var digestGenerator = MessageDigest.getInstance("MD5");
      checksum = digestGenerator.digest(resource);

    } catch (final NoSuchAlgorithmException e) {
      fail("Failed to calculate the checksum", e);
    }
    return checksum;
  }
}
