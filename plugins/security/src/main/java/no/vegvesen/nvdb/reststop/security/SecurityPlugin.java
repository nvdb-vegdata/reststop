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

package no.vegvesen.nvdb.reststop.security;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.servlet.api.FilterPhase;
import no.vegvesen.nvdb.reststop.api.Plugin;
import no.vegvesen.nvdb.reststop.servlet.api.ServletBuilder;
import no.vegvesen.nvdb.reststop.jaxrsapi.ApplicationBuilder;

import jakarta.servlet.Filter;
import jakarta.ws.rs.core.Application;

/**
 *
 */
@Plugin
public class SecurityPlugin  {
    @Export
    private final Application securityApp;

    @Export
    private final Filter basicAuthFilter;

    public SecurityPlugin(ServletBuilder servletBuilder, ApplicationBuilder applicationBuilder) {
        basicAuthFilter = servletBuilder.filter(new BasicAuthFilter(), FilterPhase.AUTHENTICATION, "/*");
        securityApp = applicationBuilder.application().resource(RolesAllowedDynamicFeature.class).build();
    }
}
