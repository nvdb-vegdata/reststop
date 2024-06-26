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

package no.vegvesen.nvdb.reststop.servlet.deploy;

import no.vegvesen.nvdb.reststop.api.Plugin;
import no.vegvesen.nvdb.reststop.api.PluginExport;
import no.vegvesen.nvdb.reststop.servlet.api.ServletDeployer;

import jakarta.servlet.Filter;
import java.util.Collection;

/**
 *
 */
@Plugin
public class ServletDeployPlugin {

    public ServletDeployPlugin(ServletDeployer servletDeployer, Collection<PluginExport<Filter>> filters) {
        servletDeployer.deploy(filters);
    }
}
