/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.clouddriver.pipeline.job;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.orca.api.pipeline.CancellableStage;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageGraphBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.tasks.artifacts.ConsumeArtifactTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.job.DestroyJobTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.job.MonitorJobTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.job.RunJobTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.job.WaitOnJobCompletion;
import com.netflix.spinnaker.orca.clouddriver.tasks.providers.kubernetes.Manifest;
import com.netflix.spinnaker.orca.clouddriver.tasks.providers.kubernetes.ManifestAnnotationExtractor;
import com.netflix.spinnaker.orca.jackson.OrcaObjectMapper;
import com.netflix.spinnaker.orca.pipeline.tasks.artifacts.BindProducedArtifactsTask;
import java.util.*;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RunJobStage implements StageDefinitionBuilder, CancellableStage {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final DestroyJobTask destroyJobTask;
  private final ObjectMapper objectMapper = OrcaObjectMapper.newInstance();
  private final List<RunJobStageDecorator> runJobStageDecorators;

  public RunJobStage(
      DestroyJobTask destroyJobTask, List<RunJobStageDecorator> runJobStageDecorators) {
    this.destroyJobTask = destroyJobTask;
    this.runJobStageDecorators =
        Optional.ofNullable(runJobStageDecorators).orElse(Collections.emptyList());
  }

  @Override
  public void taskGraph(@Nonnull StageExecution stage, @Nonnull TaskNode.Builder builder) {
    builder
        .withTask(RunJobTask.TASK_NAME, RunJobTask.class)
        .withTask(MonitorJobTask.TASK_NAME, MonitorJobTask.class);

    getCloudProviderDecorator(stage).ifPresent(it -> it.afterRunJobTaskGraph(stage, builder));

    if (!stage
        .getContext()
        .getOrDefault("waitForCompletion", "true")
        .toString()
        .equalsIgnoreCase("false")) {
      builder.withTask("waitOnJobCompletion", WaitOnJobCompletion.class);
    }

    if (stage.getContext().containsKey("expectedArtifacts")) {
      builder.withTask(BindProducedArtifactsTask.TASK_NAME, BindProducedArtifactsTask.class);
    }

    if (stage
        .getContext()
        .getOrDefault("consumeArtifactSource", "")
        .toString()
        .equalsIgnoreCase("artifact")) {
      builder.withTask(ConsumeArtifactTask.TASK_NAME, ConsumeArtifactTask.class);
    }
  }

  @Override
  public void afterStages(@Nonnull StageExecution stage, @Nonnull StageGraphBuilder graph) {
    Map<String, Object> executionLogs = getLogOutputFromManifestArtifact(stage);
    if (stage.getContext().getOrDefault("noOutput", "false").toString().equals("true")) {
      stage.setOutputs(emptyMap());
    }
    stage.getContext().putAll(executionLogs);
  }

  @Override
  public Result cancel(StageExecution stage) {
    log.info(
        "Canceling run job stage {} for executionId {}",
        stage.getId(),
        stage.getExecution().getId());
    Map<String, Object> destroyContext = new HashMap<>();

    try {
      RunJobStageContext context =
          objectMapper.convertValue(stage.getContext(), RunJobStageContext.class);

      if (context.getJobStatus() != null) {
        destroyContext.put("jobName", context.getCloudProvider());
        destroyContext.put("cloudProvider", context.getCloudProvider());
        destroyContext.put("region", context.getJobStatus().getRegion());
        destroyContext.put("credentials", context.getCredentials());

        getCloudProviderDecorator(stage)
            .ifPresent(it -> it.modifyDestroyJobContext(context, destroyContext));

        stage.setContext(destroyContext);

        destroyJobTask.execute(stage);
      }
    } catch (Exception e) {
      log.error(
          String.format(
              "Failed to cancel run job (stageId: %s, executionId: %s), e: %s",
              stage.getId(), stage.getExecution().getId(), e.getMessage()),
          e);
    }

    return new Result(stage, destroyContext);
  }

  @Override
  public void prepareStageForRestart(@Nonnull StageExecution stage) {
    Map<String, Object> context = stage.getContext();

    // preserve previous job details
    if (context.containsKey("jobStatus")) {
      Map<String, Object> restartDetails =
          context.containsKey("restartDetails")
              ? (Map<String, Object>) context.get("restartDetails")
              : new HashMap<>();
      restartDetails.put("jobStatus", context.get("jobStatus"));
      restartDetails.put("completionDetails", context.get("completionDetails"));
      restartDetails.put("propertyFileContents", context.get("propertyFileContents"));
      restartDetails.put("deploy.jobs", context.get("deploy.jobs"));
      context.put("restartDetails", restartDetails);
    }

    context.remove("jobStatus");
    context.remove("completionDetails");
    context.remove("propertyFileContents");
    context.remove("deploy.jobs");
  }

  private Optional<RunJobStageDecorator> getCloudProviderDecorator(StageExecution stage) {
    return Optional.ofNullable((String) stage.getContext().get("cloudProvider"))
        .flatMap(
            cloudProvider ->
                runJobStageDecorators.stream()
                    .filter(it -> it.supports(cloudProvider))
                    .findFirst());
  }

  public Map<String, Object> getLogOutputFromManifestArtifact(StageExecution stage) {
    Map<String, Object> outputs = new HashMap<>();
    Map<String, Object> execution = new HashMap<>();
    if (stage.getContext().containsKey("manifestArtifact")) {
      List<Manifest> manifest =
          objectMapper.convertValue(
              stage.getContext().get("outputs.manifests"), new TypeReference<List<Manifest>>() {});
      String logTemplate = ManifestAnnotationExtractor.logs(manifest.get(0));
      if (logTemplate != null) {
        execution.put("logs", logTemplate);
        outputs.put("execution", execution);
      }
    }
    return outputs;
  }
}
