package com.netflix.spinnaker.orca.api;

import com.google.common.annotations.Beta;
import java.util.Map;

@Beta
public class StageInput {
  private Map input;

  public void setInput(Map input) {
    this.input = input;
  }

  public Map getInput() {
    return this.input;
  }
}
