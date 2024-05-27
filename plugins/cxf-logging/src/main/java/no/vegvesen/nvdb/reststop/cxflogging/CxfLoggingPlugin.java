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

package no.vegvesen.nvdb.reststop.cxflogging;

import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import no.vegvesen.nvdb.reststop.cxf.EndpointCustomizer;

import jakarta.xml.ws.Endpoint;

/**
 *
 */
@Plugin
public class CxfLoggingPlugin implements EndpointCustomizer {

    @Export
    private final EndpointCustomizer endpointCustomizer;

    public CxfLoggingPlugin() {
        endpointCustomizer = this;
    }

    @Override
    public void customizeEndpoint(Endpoint endpoint) {
//        EndpointImpl endpointImpl = (EndpointImpl) endpoint;
//        endpoint.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
//        endpoint.getProperties().put(Message.SCHEMA_VALIDATION_ENABLED, SchemaValidation.SchemaValidationType.BOTH);
    }
}
