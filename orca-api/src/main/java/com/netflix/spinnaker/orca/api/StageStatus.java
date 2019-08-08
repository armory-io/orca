package com.netflix.spinnaker.orca.api;

import com.google.common.annotations.Beta;

@Beta
public enum StageStatus {
  TERMINAL,
  RUNNING,
  COMPLETED,
  NOT_STARTED
}
