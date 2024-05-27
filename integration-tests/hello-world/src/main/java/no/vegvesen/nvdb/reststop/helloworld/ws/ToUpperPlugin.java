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

package no.vegvesen.nvdb.reststop.helloworld.ws;

//import no.vegvesen.nvdb.reststop.api.Export;
//import no.vegvesen.nvdb.reststop.api.Plugin;
//import no.vegvesen.nvdb.reststop.servlet.api.ServletBuilder;
//
//import jakarta.servlet.Filter;
//import jakarta.servlet.ServletContext;
//
//import jakarta.websocket.DeploymentException;
//import jakarta.websocket.server.ServerContainer;
//
///**
// *
// */
//@Plugin
//public class ToUpperPlugin {
//
//    @Export
//    private final Filter ws;
//
//    public ToUpperPlugin(ServletContext context, ServletBuilder builder) throws DeploymentException {
//        ServerContainer cont = (ServerContainer) context.getAttribute(ServerContainer.class.getName());
//        cont.addEndpoint(ToUpperEndpoint.class);
//
//        ws = builder.resourceServlet(getClass().getResource("/assets/ws/upper.html"), "/upper.html");
//    }
//}
