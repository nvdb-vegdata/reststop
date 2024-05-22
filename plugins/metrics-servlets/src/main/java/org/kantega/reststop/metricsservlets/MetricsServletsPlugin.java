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

package org.kantega.reststop.metricsservlets;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import org.kantega.reststop.api.Config;
import org.kantega.reststop.api.Export;
import org.kantega.reststop.api.Plugin;
import org.kantega.reststop.servlet.api.ServletBuilder;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Plugin
public class MetricsServletsPlugin {

    @Export
    private final Filter metricsServlet;

    @Export
    private final Filter healthCheckServlet;

    public MetricsServletsPlugin(
            @Config(defaultValue = "/metrics/*") String metricsJsonServletPath,
            @Config(defaultValue = "/healthchecks/*") String metricsHealthCheckServletPath,
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            ServletBuilder servletBuilder,
            ServletContext servletContext) throws ServletException {


        MetricsServlet metricsServlet = new MetricsServlet(metricRegistry);
        metricsServlet.init(new EmptyServletConfig(createProxy(servletContext)));

        this.metricsServlet = servletBuilder.servlet(
                metricsServlet,
                metricsJsonServletPath);

        HealthCheckServlet healthCheckServlet = new HealthCheckServlet(healthCheckRegistry);
        healthCheckServlet.init(servletBuilder.servletConfig("healthcheck", new Properties()));

        this.healthCheckServlet = servletBuilder.servlet(
                healthCheckServlet,
                metricsHealthCheckServletPath);

    }


    private ServletContext createProxy(final ServletContext servletContext) {
        return (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ServletContext.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("getInitParameter")) {
                    if (MetricsServlet.DURATION_UNIT.equals(args[0])) {
                        return TimeUnit.MILLISECONDS.toString();
                    }
                }
                return method.invoke(servletContext, args);
            }
        });
    }


    private class EmptyServletConfig implements ServletConfig {
        private final ServletContext servletContext;

        private EmptyServletConfig(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        public String getServletName() {
            return "metrics";
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.emptyEnumeration();
        }
    }
}
