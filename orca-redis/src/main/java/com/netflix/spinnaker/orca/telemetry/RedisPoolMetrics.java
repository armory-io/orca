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


import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Pool;

import java.lang.reflect.Field;

@Component
public class RedisPoolMetrics implements MeterBinder {

  Pool<Jedis> pool;

  @Autowired
  public RedisPoolMetrics(Pool<Jedis> pool) {
    this.pool = pool;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Field internalPoolField = ReflectionUtils.findField(Pool.class, "internalPool");
    ReflectionUtils.makeAccessible(internalPoolField);
    try {
      GenericObjectPool<Jedis> internalPool = (GenericObjectPool<Jedis>)internalPoolField.get(this.pool);
      Gauge.builder("redis.connectionPool.maxIdle", internalPool, GenericObjectPool::getMaxIdle)
          .register(registry);

      Gauge.builder("redis.connectionPool.minIdle", internalPool, GenericObjectPool::getMinIdle)
          .register(registry);

      Gauge.builder("redis.connectionPool.numActive", internalPool, GenericObjectPool::getNumActive)
          .register(registry);

      Gauge.builder("redis.connectionPool.numIdle", internalPool, GenericObjectPool::getNumIdle)
          .register(registry);

      Gauge.builder("redis.connectionPool.numWaiters", internalPool, GenericObjectPool::getNumWaiters)
          .register(registry);

    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }

  }

}
