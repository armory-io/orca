/*
 * Copyright 2021 Armory, Inc.
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

package com.netflix.spinnaker.orca.clouddriver.pipeline.manifest

import com.netflix.spinnaker.orca.api.test.pipeline
import com.netflix.spinnaker.orca.api.test.stage
import com.netflix.spinnaker.orca.pipeline.StageDefinitionBuilderFactory
import com.netflix.spinnaker.orca.pipeline.util.ContextParameterProcessor
import com.netflix.spinnaker.orca.q.handler.ExpressionAware
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.mockk
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DeployManifestExpressionEvaluationTest : JUnit5Minutests {
  fun tests() = rootContext<Fixture> {
    fixture { Fixture() }

    listOf("v3", "v4", "").forEach { spelVersion ->
      test("should evaluate expression in manifest when SpEL ${if (spelVersion == "") "default version" else spelVersion} is enabled") {
        val pipeline = pipeline {
          if (spelVersion != "") spelEvaluator = spelVersion
          stage {
            refId = "1"
            type = "deployManifest"
            context["manifests"] = listOf(
              mapOf(
                "metadata" to mapOf(
                  "name" to "my-k8s-manifest",
                  "should-evaluate-to-literal-true" to "\${#toBoolean('true')}"
                )
              )
            )
          }
        }
        val deployStage = pipeline.stageByRef("1")

        expectThat(deployStage.withMergedContext().context["manifests"]).isEqualTo(listOf(mapOf(
          "metadata" to mapOf(
            "name" to "my-k8s-manifest",
            "should-evaluate-to-literal-true" to true,
          )
        )))
      }
    }

    test("when v4 expression evaluator is enabled, should not evaluate expressions when 'skipExpressionEvaluation' context flag is enabled") {
      val pipeline = pipeline {
        spelEvaluator = "v4"
        stage {
          refId = "1"
          type = "deployManifest"
          context["skipExpressionEvaluation"] = true
          context["manifests"] = listOf(
            mapOf(
              "metadata" to mapOf(
                "name" to "my-k8s-manifest",
                "should-not-evaluate-to-literal-true" to "\${#toBoolean('true')}"
              )
            )
          )
        }
      }
      val deployStage = pipeline.stageByRef("1")

      expectThat(deployStage.withMergedContext().context["manifests"]).isEqualTo(listOf(mapOf(
        "metadata" to mapOf(
          "name" to "my-k8s-manifest",
          "should-not-evaluate-to-literal-true" to "\${#toBoolean('true')}"
        )
      )))
    }

    listOf("v3", "v4", "").forEach { spelVersion ->
      listOf(true, false).forEach { skipExpressionEvaluation ->
        test("non-manifest context values should be evaluated when SpEL ${if (spelVersion == "") "default version" else spelVersion} is enabled and skipExpressionEvaluation=$skipExpressionEvaluation") {
          val pipeline = pipeline {
            if (spelVersion != "") spelEvaluator = spelVersion
            stage {
              refId = "1"
              type = "deployManifest"
              context["skipExpressionEvaluation"] = skipExpressionEvaluation
              context["should-evaluate-to-literal-true"] = "\${#toBoolean('true')}"
            }
          }
          val deployStage = pipeline.stageByRef("1")

          expectThat(deployStage.withMergedContext().context["should-evaluate-to-literal-true"]).isEqualTo(true)
        }
      }
    }
  }

  class Fixture : ExpressionAware {
    override val contextParameterProcessor = ContextParameterProcessor()
    override val stageDefinitionBuilderFactory = StageDefinitionBuilderFactory { execution ->
      when (execution.type) {
        "deployManifest" -> DeployManifestStage(mockk())
        else -> throw IllegalArgumentException("Test factory can't make \"${execution.type}\" stages.")
      }
    }
  }
}
