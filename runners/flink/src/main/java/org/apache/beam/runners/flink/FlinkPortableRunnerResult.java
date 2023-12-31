/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.flink;

import java.io.IOException;
import java.util.Map;
import org.apache.beam.model.jobmanagement.v1.JobApi;
import org.apache.beam.model.pipeline.v1.MetricsApi;
import org.apache.beam.runners.jobsubmission.PortablePipelineResult;
import org.apache.beam.sdk.metrics.MetricResults;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Result of executing a portable {@link org.apache.beam.sdk.Pipeline} with Flink. */
public class FlinkPortableRunnerResult extends FlinkRunnerResult implements PortablePipelineResult {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkPortableRunnerResult.class);

  FlinkPortableRunnerResult(Map<String, Object> accumulators, long runtime) {
    super(accumulators, runtime);
  }

  @Override
  public JobApi.MetricResults portableMetrics() throws UnsupportedOperationException {
    Iterable<MetricsApi.MonitoringInfo> monitoringInfos =
        this.getMetricsContainerStepMap().getMonitoringInfos();

    return JobApi.MetricResults.newBuilder()
        .addAllCommitted(monitoringInfos)
        .addAllAttempted(monitoringInfos)
        .build();
  }

  static class Detached implements PortablePipelineResult {

    Detached() {
      super();
    }

    @Override
    public JobApi.MetricResults portableMetrics() throws UnsupportedOperationException {
      LOG.warn(
          "Collecting monitoring infos is not implemented yet in Flink portable runner (detached mode).");
      return JobApi.MetricResults.newBuilder().build();
    }

    @Override
    public State getState() {
      return State.UNKNOWN;
    }

    @Override
    public State cancel() throws IOException {
      throw new UnsupportedOperationException("Cancelling is not yet supported.");
    }

    @Override
    public State waitUntilFinish(Duration duration) {
      return State.UNKNOWN;
    }

    @Override
    public State waitUntilFinish() {
      return State.UNKNOWN;
    }

    @Override
    public MetricResults metrics() {
      throw new UnsupportedOperationException(
          "The FlinkRunner does not currently support metrics.");
    }
  }
}
