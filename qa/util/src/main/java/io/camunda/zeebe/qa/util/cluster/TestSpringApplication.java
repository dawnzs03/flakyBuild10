/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.qa.util.cluster;

import io.camunda.zeebe.qa.util.cluster.util.ContextOverrideInitializer;
import io.camunda.zeebe.qa.util.cluster.util.ContextOverrideInitializer.Bean;
import io.camunda.zeebe.qa.util.cluster.util.RelaxedCollectorRegistry;
import io.camunda.zeebe.shared.MainSupport;
import io.camunda.zeebe.shared.Profile;
import io.camunda.zeebe.test.util.socket.SocketUtil;
import io.prometheus.client.CollectorRegistry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

abstract class TestSpringApplication<T extends TestSpringApplication<T>>
    implements TestApplication<T> {
  private final Class<?> springApplication;
  private final Map<String, Bean<?>> beans;
  private final Map<String, Object> propertyOverrides;

  private ConfigurableApplicationContext springContext;

  public TestSpringApplication(final Class<?> springApplication) {
    this(springApplication, new HashMap<>(), new HashMap<>());
  }

  private TestSpringApplication(
      final Class<?> springApplication,
      final Map<String, Bean<?>> beans,
      final Map<String, Object> propertyOverrides) {
    this.springApplication = springApplication;
    this.beans = beans;
    this.propertyOverrides = propertyOverrides;

    if (!propertyOverrides.containsKey("server.port")) {
      propertyOverrides.put("server.port", SocketUtil.getNextAddress().getPort());
    }

    if (!beans.containsKey("collectorRegistry")) {
      beans.put(
          "collectorRegistry", new Bean<>(new RelaxedCollectorRegistry(), CollectorRegistry.class));
    }
  }

  @Override
  public T start() {
    if (!isStarted()) {
      springContext = createSpringBuilder().run(commandLineArgs());
    }

    return self();
  }

  @Override
  public T stop() {
    if (springContext != null) {
      springContext.close();
      springContext = null;
    }

    return self();
  }

  @Override
  public boolean isStarted() {
    return springContext != null && springContext.isActive();
  }

  @Override
  public <V> T withBean(final String qualifier, final V bean, final Class<V> type) {
    beans.put(qualifier, new Bean<>(bean, type));
    return self();
  }

  @Override
  public int mappedPort(final TestZeebePort port) {
    if (port != TestZeebePort.MONITORING) {
      throw new IllegalArgumentException(
          "No known port %s; must one of MONITORING".formatted(port));
    }

    return monitoringPort();
  }

  @Override
  public <V> V bean(final Class<V> type) {
    return springContext.getBean(type);
  }

  @Override
  public T withProperty(final String key, final Object value) {
    propertyOverrides.put(key, value);
    return self();
  }

  /** Returns the command line arguments that will be passed when the application is started. */
  protected String[] commandLineArgs() {
    return new String[0];
  }

  /**
   * Returns a builder which can be modified to enable more profiles, inject beans, etc. Sub-classes
   * can override this to customize the behavior of the test application.
   */
  protected SpringApplicationBuilder createSpringBuilder() {
    return MainSupport.createDefaultApplicationBuilder()
        .bannerMode(Mode.OFF)
        .lazyInitialization(true)
        .registerShutdownHook(false)
        .initializers(new ContextOverrideInitializer(beans, propertyOverrides))
        .profiles(Profile.TEST.getId())
        .sources(springApplication);
  }

  private int monitoringPort() {
    final Object portProperty;
    if (springContext != null) {
      portProperty = springContext.getEnvironment().getProperty("server.port");
    } else {
      portProperty = propertyOverrides.get("server.port");
    }

    if (portProperty == null) {
      throw new IllegalStateException(
          "No property server.port defined anywhere, cannot infer monitoring port");
    }

    if (portProperty instanceof final Integer port) {
      return port;
    }

    return Integer.parseInt(portProperty.toString());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{nodeId = " + nodeId() + "}";
  }
}
