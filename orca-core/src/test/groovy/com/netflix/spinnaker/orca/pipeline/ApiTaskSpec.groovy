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

package com.netflix.spinnaker.orca.pipeline

import com.netflix.spinnaker.orca.ExecutionStatus
import com.netflix.spinnaker.orca.api.Stage
import com.netflix.spinnaker.orca.api.StageInput
import com.netflix.spinnaker.orca.api.StageOutput
import com.netflix.spinnaker.orca.api.StageStatus
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ApiTaskSpec extends Specification {
  private static class MyStage implements Stage<Object> {
    @Override
    String getName() {
      return "myStage"
    }

    @Override
    <Object> StageOutput execute(StageInput<Object> input) {
      StageOutput output = new StageOutput()

      Map<String, String> stageOutput = new HashMap<>()
      stageOutput.put("hello", "world")

      output.setStatus(StageStatus.COMPLETED)
      output.setOutputs(stageOutput)
      return output
    }
  }

  @Subject
  def myStage = new MyStage()

  @Unroll
  def "should check dynamic config property"() {
    when:
    def task = new ApiTask(myStage)
    def results = task.execute(new com.netflix.spinnaker.orca.pipeline.model.Stage())

    then:
    results.getStatus() == ExecutionStatus.SUCCEEDED
    results.context.hello == "world"
  }
}
