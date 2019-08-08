package com.netflix.spinnaker.orca.api;

import com.google.common.annotations.Beta;

@Beta
public interface Stage {
  StageOutput execute(StageInput stageInput);
}
