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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ThreadPoolMetrics implements MeterBinder {

  static class ThreadPoolTaskExecutorMetrics implements MeterBinder {
    private final Iterable<Tag> tags;
    private final String name;

    private final ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutorMetrics(ThreadPoolTaskExecutor executor, String name, List<Tag> tags) {
      this.executor=executor;
      this.name=name;
      this.tags=tags;

    }

    @Override
    public void bindTo(MeterRegistry registry) {
      if (executor == null) {
        return;
      }
      monitor(registry, executor.getThreadPoolExecutor());
    }

    private void monitor(MeterRegistry registry, ThreadPoolExecutor tp) {

      Gauge.builder("threadpool.activeCount", tp, ThreadPoolExecutor::getActiveCount)
          .tags(tags).register(registry);

      Gauge.builder("threadpool..maximumPoolSize", tp, ThreadPoolExecutor::getMaximumPoolSize)
          .tags(tags).register(registry);

      Gauge.builder("threadpool.corePoolSize", tp, ThreadPoolExecutor::getCorePoolSize)
          .tags(tags).register(registry);

      Gauge.builder("threadpool.poolSize", tp, ThreadPoolExecutor::getPoolSize)
          .tags(tags).register(registry);

      Gauge.builder("threadpool.blockingQueueSize", tp, e -> e.getQueue().size())
          .tags(tags).register(registry);

    }
  }

  private  Map<String, ThreadPoolTaskExecutor> executors;

  @Autowired
  public ThreadPoolMetrics(Map<String, ThreadPoolTaskExecutor> executors) {
    this.executors = executors;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
      executors.forEach((beanName, executor) -> {
        List<Tag> tags = Lists.newArrayList();
        tags.add(Tag.of("id", beanName));
        new ThreadPoolTaskExecutorMetrics(executor, "threadpool", tags)
            .bindTo(registry);
      });
  }
}
