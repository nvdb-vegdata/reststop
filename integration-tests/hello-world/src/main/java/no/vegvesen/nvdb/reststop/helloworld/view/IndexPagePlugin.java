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

package no.vegvesen.nvdb.reststop.helloworld.view;

import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import org.kantega.reststop.servlet.api.ServletBuilder;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
@Plugin
public class IndexPagePlugin  {

    @Export
    private final Filter indexServlet;

    @Export
    private final Filter cssServlet;
    @Export
    private final Filter helloWorldServlet;
    @Export
    private final Filter redirect;

    public IndexPagePlugin(ServletBuilder servletBuilder) {
        indexServlet = servletBuilder.resourceServlet(getClass().getResource("index.html"), "/");
        cssServlet = servletBuilder.resourceServlet(getClass().getResource("ws.css"), "/ws.css");

        redirect = servletBuilder.redirectFrom("/christmastable").to("http://disney.com");

        helloWorldServlet = servletBuilder.servlet(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().println("halloOo");
                resp.getWriter().println(req.getServletPath());
                resp.getWriter().println(req.getPathInfo());
            }
        }, "/heiverden", "/heiverda*");
    }
}

