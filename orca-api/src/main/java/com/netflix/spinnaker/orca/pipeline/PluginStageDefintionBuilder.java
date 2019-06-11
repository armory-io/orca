package com.netflix.spinnaker.orca.pipeline;

import static com.netflix.spinnaker.orca.pipeline.TaskNode.Builder;
import static com.netflix.spinnaker.orca.pipeline.TaskNode.GraphType.FULL;
import static com.netflix.spinnaker.orca.pipeline.model.SyntheticStageOwner.STAGE_AFTER;
import static com.netflix.spinnaker.orca.pipeline.model.SyntheticStageOwner.STAGE_BEFORE;
import static java.util.stream.Collectors.toList;

import com.google.common.base.CaseFormat;
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService;
import com.netflix.spinnaker.orca.pipeline.TaskNode.TaskGraph;
import com.netflix.spinnaker.orca.pipeline.graph.StageGraphBuilder;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public interface PluginStageDefintionBuilder extends StageDefinitionBuilder {
  default @Nonnull TaskGraph buildTaskGraph(@Nonnull Stage stage) {
    Builder graphBuilder = Builder(FULL);
    taskGraph(stage, graphBuilder);
    return graphBuilder.build();
  }

  default void taskGraph(@Nonnull Stage stage, @Nonnull Builder builder) {}

  /**
   * Implement this method to define any stages that should run before any tasks in this stage as
   * part of a composed workflow.
   *
   * <p>This default implementation is for backward compatibility with the legacy {@link
   * #aroundStages} and {@link #parallelStages} methods.
   */
  default void beforeStages(@Nonnull Stage parent, @Nonnull StageGraphBuilder graph) {
    List<Stage> stages =
        aroundStages(parent).stream()
            .filter((it) -> it.getSyntheticStageOwner() == STAGE_BEFORE)
            .collect(toList());
    if (!stages.isEmpty()) {
      graph.add(stages.get(0));
    }
    for (int i = 1; i < stages.size(); i++) {
      graph.connect(stages.get(i - 1), stages.get(i));
    }
    parallelStages(parent).stream()
        .filter((it) -> it.getSyntheticStageOwner() == STAGE_BEFORE)
        .forEach(graph::add);
  }

  /**
   * Implement this method to define any stages that should run after any tasks in this stage as
   * part of a composed workflow.
   *
   * <p>This default implementation is for backward compatibility with the legacy {@link
   * #aroundStages} and {@link #parallelStages} methods.
   */
  default void afterStages(@Nonnull Stage parent, @Nonnull StageGraphBuilder graph) {
    List<Stage> stages =
        aroundStages(parent).stream()
            .filter((it) -> it.getSyntheticStageOwner() == STAGE_AFTER)
            .collect(toList());
    if (!stages.isEmpty()) {
      graph.add(stages.get(0));
    }
    for (int i = 1; i < stages.size(); i++) {
      graph.connect(stages.get(i - 1), stages.get(i));
    }
    parallelStages(parent).stream()
        .filter((it) -> it.getSyntheticStageOwner() == STAGE_AFTER)
        .forEach(graph::add);
  }

  /**
   * Implement this method to define any stages that should run in response to a failure in tasks,
   * before or after stages.
   */
  default void onFailureStages(@Nonnull Stage stage, @Nonnull StageGraphBuilder graph) {}

  /** @return the stage type this builder handles. */
  default @Nonnull String getType() {
    return getType(this.getClass());
  }

  /** Implementations can override this if they need any special cleanup on restart. */
  default void prepareStageForRestart(@Nonnull Stage stage) {}

  static String getType(Class<? extends StageDefinitionBuilder> clazz) {
    String className = clazz.getSimpleName();
    return className.substring(0, 1).toLowerCase()
        + className
            .substring(1)
            .replaceFirst("StageDefinitionBuilder$", "")
            .replaceFirst("Stage$", "");
  }

  /** Return true if the stage can be manually skipped from the API. */
  default boolean canManuallySkip() {
    return false;
  }

  default boolean isForceCacheRefreshEnabled(DynamicConfigService dynamicConfigService) {
    String className = getClass().getSimpleName();

    try {
      return dynamicConfigService.isEnabled(
          String.format(
              "stages.%s.force-cache-refresh",
              CaseFormat.LOWER_CAMEL.to(
                  CaseFormat.LOWER_HYPHEN,
                  Character.toLowerCase(className.charAt(0)) + className.substring(1))),
          true);
    } catch (Exception e) {
      return true;
    }
  }

  default Collection<String> aliases() {
    if (getClass().isAnnotationPresent(Aliases.class)) {
      return Arrays.asList(getClass().getAnnotation(Aliases.class).value());
    }

    return Collections.emptyList();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Aliases {
    String[] value() default {};
  }
}
