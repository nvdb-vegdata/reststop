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

package no.vegvesen.nvdb.reststop.jaxrs;

import no.vegvesen.nvdb.reststop.jaxrsapi.ApplicationDeployer;
import no.vegvesen.nvdb.reststop.api.Plugin;

import jakarta.ws.rs.core.Application;
import java.util.Collection;

/**
 *
 */
@Plugin
public class JerseyDeployPlugin {

    public JerseyDeployPlugin(ApplicationDeployer deployer, Collection<Application> applications) {
        deployer.deploy(applications);
    }
}
