/*
 * Copyright 2018 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.reststop.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.*;
import org.kantega.reststop.api.Export;
import org.kantega.reststop.api.Plugin;

import jakarta.servlet.ServletException;
import java.lang.management.ManagementFactory;

/**
 *
 */
@Plugin
public class MetricsReststopPlugin {


    @Export final MetricRegistry metricRegistry;
    @Export final HealthCheckRegistry healthCheckRegistry;

    public MetricsReststopPlugin() throws ServletException {

        metricRegistry = initMetricsRegistry();

        healthCheckRegistry = initHealthCheckRegistry();
    }


    private MetricRegistry initMetricsRegistry() {
        MetricRegistry registry = new MetricRegistry();

        registry.registerAll(new MemoryUsageGaugeSet());
        registry.register("fileDescriptorRation", new FileDescriptorRatioGauge());
        registry.registerAll(new GarbageCollectorMetricSet());
        registry.registerAll(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        registry.registerAll(new ThreadStatesGaugeSet());

        return registry;
    }

    private HealthCheckRegistry initHealthCheckRegistry() {
        HealthCheckRegistry registry = new HealthCheckRegistry();
        registry.register("threadDeadlock", new ThreadDeadlockHealthCheck());
        return registry;
    }
}
