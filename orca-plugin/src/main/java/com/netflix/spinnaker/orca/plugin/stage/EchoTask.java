package com.netflix.spinnaker.orca.plugin.stage;

import com.netflix.spinnaker.orca.RetryableTask;
import com.netflix.spinnaker.orca.TaskResult;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import com.netflix.spinnaker.orca.ExecutionStatus;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Component
public class EchoTask implements RetryableTask {

  @Override
  public TaskResult execute(Stage stage) {
    System.out.println("Executed echo task");
    Map<String, Object> map = new HashMap<>();
    return new TaskResult(ExecutionStatus.SUCCEEDED, map);
  }

  @Override
  public long getBackoffPeriod() {
    return TimeUnit.MINUTES.toMillis(1);
  }

  @Override
  public long getTimeout() {
    return TimeUnit.DAYS.toMillis(1);
  }

}
