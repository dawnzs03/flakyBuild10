/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.state.deployment;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbString;
import io.camunda.zeebe.engine.state.mutable.MutableFormState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.deployment.FormRecord;
import java.util.Optional;
import org.agrona.DirectBuffer;

public class DbFormState implements MutableFormState {

  private static final int DEFAULT_VERSION_VALUE = 0;

  private final DbLong dbFormKey;
  private final PersistedForm dbPersistedForm;
  private final ColumnFamily<DbLong, PersistedForm> formsByKey;
  private final DbString dbFormId;
  private final VersionManager versionManager;
  private final DbLong formVersion;
  private final DbCompositeKey<DbString, DbLong> idAndVersionKey;
  private final ColumnFamily<DbCompositeKey<DbString, DbLong>, PersistedForm>
      formByIdAndVersionColumnFamily;

  public DbFormState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    dbFormKey = new DbLong();
    dbPersistedForm = new PersistedForm();
    formsByKey =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.FORMS, transactionContext, dbFormKey, dbPersistedForm);

    dbFormId = new DbString();
    formVersion = new DbLong();
    idAndVersionKey = new DbCompositeKey<>(dbFormId, formVersion);
    formByIdAndVersionColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.FORM_BY_ID_AND_VERSION,
            transactionContext,
            idAndVersionKey,
            dbPersistedForm);

    versionManager =
        new VersionManager(
            DEFAULT_VERSION_VALUE, zeebeDb, ZbColumnFamilies.FORM_VERSION, transactionContext);
  }

  @Override
  public void storeFormRecord(final FormRecord record) {
    dbFormKey.wrapLong(record.getFormKey());
    dbFormId.wrapString(record.getFormId());
    formVersion.wrapLong(record.getVersion());
    dbPersistedForm.wrap(record);
    formsByKey.upsert(dbFormKey, dbPersistedForm);
    formByIdAndVersionColumnFamily.upsert(idAndVersionKey, dbPersistedForm);

    updateLatestVersion(record);
  }

  @Override
  public Optional<PersistedForm> findLatestFormById(
      final DirectBuffer formId, final String tenantId) {
    dbFormId.wrapBuffer(formId);
    final long latestVersion = versionManager.getLatestResourceVersion(formId, tenantId);
    formVersion.wrapLong(latestVersion);
    return Optional.ofNullable(formByIdAndVersionColumnFamily.get(idAndVersionKey));
  }

  @Override
  public Optional<PersistedForm> findFormByKey(final long formKey, final String tenantId) {
    dbFormKey.wrapLong(formKey);
    return Optional.ofNullable(formsByKey.get(dbFormKey)).map(PersistedForm::copy);
  }

  private void updateLatestVersion(final FormRecord formRecord) {
    final var formId = formRecord.getFormId();
    dbFormId.wrapString(formId);
    final var version = formRecord.getVersion();
    versionManager.addResourceVersion(formId, version, formRecord.getTenantId());
  }
}
