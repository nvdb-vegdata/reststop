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

package no.vegvesen.nvdb.reststop.helloworld.springmvc;

import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import org.kantega.reststop.servlet.api.ServletBuilder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.util.Properties;

/**
 *
 */
@Plugin
public class SpringMvcPlugin {

    @Export
    private final Filter springServlet;

    public SpringMvcPlugin(ServletBuilder servletBuilder) throws ServletException {

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

        context.scan(getClass().getPackage().getName());

        DispatcherServlet servlet = new DispatcherServlet(context);

        Properties properties = new Properties();
        String filterPath = "/spring/*";
        servlet.init(servletBuilder.servletConfig("spring", properties));
        springServlet = servletBuilder.servlet(servlet, filterPath);
    }
}
