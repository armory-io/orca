package com.netflix.spinnaker.orca.plugin.stage;

import com.netflix.spinnaker.orca.CancellableStage;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.pipeline.TaskNode;

import org.springframework.stereotype.Component;

@Component
public class MyEchoStage implements StageDefinitionBuilder, CancellableStage {

  MyEchoStage() {
    System.out.println("MyEchoStage Constructed.");
  }

  @Override
  public void taskGraph(Stage stage, TaskNode.Builder builder) {
    builder.withTask("echoTask", EchoTask.class);
  }


  @Override
  public CancellableStage.Result cancel(Stage stage) {
    return null;
  }

}
