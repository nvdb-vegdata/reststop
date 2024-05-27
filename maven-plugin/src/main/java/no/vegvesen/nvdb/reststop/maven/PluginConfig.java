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

package no.vegvesen.nvdb.reststop.maven;

import no.vegvesen.nvdb.reststop.classloaderutils.config.PluginConfigParams;

/**
 *
 */
public class PluginConfig  {

    private final String className;

    private final PluginConfigParams configParams;

    public PluginConfig(String className, PluginConfigParams configParams) {
        this.className = className;
        this.configParams = configParams;
    }

    public String getClassName() {
        return className;
    }

    public PluginConfigParams getConfigParams() {
        return configParams;
    }
}
