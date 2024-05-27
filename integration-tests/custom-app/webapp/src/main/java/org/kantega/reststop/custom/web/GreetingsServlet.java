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

package org.kantega.reststop.custom.web;

import org.kantega.reststop.api.ReststopPluginManager;
import org.kantega.reststop.custom.api.GreetingSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class GreetingsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ReststopPluginManager manager = (ReststopPluginManager) request.getServletContext ().getAttribute("reststopPluginManager");

        List<String> greetings = new ArrayList<>();

        for (GreetingSource source : manager.findExports(GreetingSource.class)) {
            greetings.add(source.getGreeting());
        }

        request.setAttribute("greetings", greetings);

        Collection<Object> all = manager.getPlugins();

        request.setAttribute("reststopPlugins", all);

        request.getRequestDispatcher("/WEB-INF/jsp/greetings.jsp").forward(request, response);
    }
}
