/*
 * Copyright 2015 Kantega AS
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

package org.kantega.reststop.jerseydeploy;

import org.kantega.reststop.api.Plugin;
import org.kantega.reststop.jaxrsapi.ApplicationDeployer;

import javax.ws.rs.core.Application;
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