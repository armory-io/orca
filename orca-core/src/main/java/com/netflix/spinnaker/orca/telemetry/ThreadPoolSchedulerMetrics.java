/*
 * Copyright 2021 Netflix, Inc.
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

package com.netflix.spinnaker.orca.telemetry;

import com.google.common.collect.Lists;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ThreadPoolSchedulerMetrics implements MeterBinder {

  private Map<String, ThreadPoolTaskScheduler> schedulerExecutors;

  @Autowired
  public ThreadPoolSchedulerMetrics(Map<String, ThreadPoolTaskScheduler> schedulerExecutors) {
    this.schedulerExecutors = schedulerExecutors;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    schedulerExecutors.forEach((beanName, executor) -> {
      List<Tag> tags = Lists.newArrayList();
      tags.add(Tag.of("id", beanName));
      monitor("threadpool", executor.getScheduledThreadPoolExecutor(),
          registry, tags);
    });
  }


  private void monitor(String name, ThreadPoolExecutor tp, MeterRegistry registry, List<Tag> tags) {
    Gauge.builder(name + ".activeCount", tp, ThreadPoolExecutor::getActiveCount)
        .tags(tags).register(registry);

    Gauge.builder(name + ".maximumPoolSize", tp, ThreadPoolExecutor::getMaximumPoolSize)
        .tags(tags).register(registry);

    Gauge.builder(name + ".corePoolSize", tp, ThreadPoolExecutor::getCorePoolSize)
        .tags(tags).register(registry);

    Gauge.builder(name + ".poolSize", tp, ThreadPoolExecutor::getPoolSize)
        .tags(tags).register(registry);

    Gauge.builder(name + ".blockingQueueSize", tp, e -> e.getQueue().size())
        .tags(tags).register(registry);

  }
}
