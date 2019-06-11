package com.netflix.spinnaker.orca;

import com.netflix.spinnaker.orca.pipeline.model.Stage;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

public interface PluginTask extends Task {
  @Nonnull
  TaskResult execute(@Nonnull Stage stage);

  default void onTimeout(@Nonnull Stage stage) {}

  default void onCancel(@Nonnull Stage stage) {}

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
