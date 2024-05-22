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

package org.kantega.reststop.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.kantega.reststop.api.Config;
import org.kantega.reststop.api.Export;
import org.kantega.reststop.api.Plugin;
import org.kantega.reststop.api.PluginExport;
import org.kantega.reststop.servlet.api.ServletBuilder;
import org.kantega.reststop.servlet.api.ServletDeployer;
import org.kantega.reststop.servlets.ReststopInitializer;

import jakarta.annotation.PreDestroy;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.EnumSet;

/**
 *
 */
@Plugin
public class JettyPlugin {

    @Export final ServletBuilder servletBuilder;
    @Export final ServletContext servletContext;
    @Export final ServletDeployer servletDeployer;

    private final Server server;

    public JettyPlugin(@Config(defaultValue = "8080") int jettyPort,
                       @Config(defaultValue = "false") boolean jettyEnableXForwarded,
                       Collection<PluginExport<ServletContextCustomizer>> servletContextCustomizers)throws Exception {

        server = new Server();

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);


        ReststopInitializer.PluginDelegatingFilter filter = new ReststopInitializer.PluginDelegatingFilter();

        servletDeployer = filter;
        handler.addFilter(new FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(handler);

        Thread thread = Thread.currentThread();
        ClassLoader oldCl = thread.getContextClassLoader();

        try {
            for (PluginExport<ServletContextCustomizer> export : servletContextCustomizers) {
                thread.setContextClassLoader(export.getClassLoader());
                export.getExport().customize(handler);
            }
        } finally {
            thread.setContextClassLoader(oldCl);
        }

        server.addConnector(createHttpConnector(server, jettyPort, jettyEnableXForwarded));

        try {
            server.start();
        } catch (Exception e) {
            server.stop();
            throw e;
        }

        servletContext =  handler.getServletContext();

        ReststopInitializer.DefaultServletBuilder defaultServletBuilder = new ReststopInitializer.DefaultServletBuilder(servletContext, filter);

        servletBuilder = defaultServletBuilder;

        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        int actualPort = connector.getLocalPort();

        System.setProperty("reststopPort", Integer.toString(actualPort));
    }

    private static Connector createHttpConnector(Server server, int jettyPort, boolean jettyEnableXForwarded) {
        
        final HttpConfiguration httpConfig = new HttpConfiguration();

        if(jettyEnableXForwarded) {
            httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        }

        final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(jettyPort);

        return httpConnector;
    }

    @PreDestroy
    public void stop() throws Exception {
        if(server != null && !server.isStopped()) {
            server.setStopTimeout(500l);
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
