package com.netflix.spinnaker.orca.api;

import com.google.common.annotations.Beta;
import java.util.Map;

@Beta
public class StageOutput {
  private StageStatus status;

  public void setStatus(StageStatus status) {
    this.status = status;
  }

  public StageStatus getStatus() {
    return this.status;
  }

  private Map outputs;

  public void setOutputs(Map outputs) {
    this.outputs = outputs;
  }

  public Map getOutputs() {
    return this.outputs;
  }
}
