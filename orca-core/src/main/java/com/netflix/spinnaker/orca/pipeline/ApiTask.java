/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.netflix.spinnaker.orca.pipeline;

import com.netflix.spinnaker.orca.ExecutionStatus;
import com.netflix.spinnaker.orca.Task;
import com.netflix.spinnaker.orca.TaskResult;
import com.netflix.spinnaker.orca.api.StageInput;
import com.netflix.spinnaker.orca.api.StageOutput;
import com.netflix.spinnaker.orca.pipeline.model.Stage;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class ApiTask implements Task {
  private com.netflix.spinnaker.orca.api.Stage apiStage;

  ApiTask(com.netflix.spinnaker.orca.api.Stage apiStage) {
    this.apiStage = apiStage;
  }

  @Nonnull
  public TaskResult execute(@Nonnull Stage stage) {
    TaskResult result;
    StageInput stageInput = new StageInput();
    stageInput.setInput(stage.getContext());
    StageOutput outputs = apiStage.execute(stageInput);
    switch (outputs.getStatus()) {
      case TERMINAL:
        result = TaskResult.ofStatus(ExecutionStatus.TERMINAL);
        break;
      case RUNNING:
        result = TaskResult.ofStatus(ExecutionStatus.RUNNING);
        break;
      case COMPLETED:
        result = TaskResult.ofStatus(ExecutionStatus.SUCCEEDED);
        break;
      case NOT_STARTED:
        result = TaskResult.ofStatus(ExecutionStatus.NOT_STARTED);
        break;
      default:
        result = TaskResult.ofStatus(ExecutionStatus.FAILED_CONTINUE);
        break;
    }

    return result;
  }
}
