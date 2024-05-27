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

package no.vegvesen.nvdb.reststop.jerseymetrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 *
 */
@Provider
public class TimerBeforeFilter implements ContainerRequestFilter {


    private final String path;

    public TimerBeforeFilter(String path) {

        this.path = path;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        MetricRegistry registry = JerseyMetricsPlugin.getMetricRegistry();

        String name = name("REST", requestContext.getMethod(), path);

        Timer.Context context = registry.timer(name).time();

        requestContext.setProperty("metrics.timeContext", context);

        requestContext.setProperty("metrics.path", path);

    }


}
