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

package no.vegvesen.nvdb.reststop.development;

import no.vegvesen.nvdb.reststop.api.Config;
import no.vegvesen.nvdb.reststop.api.Export;
import no.vegvesen.nvdb.reststop.api.Plugin;
import no.vegvesen.nvdb.reststop.api.ReststopPluginManager;
import no.vegvesen.nvdb.reststop.servlet.api.FilterPhase;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import no.vegvesen.nvdb.reststop.api.*;
import no.vegvesen.nvdb.reststop.core.DefaultReststopPluginManager;
import no.vegvesen.nvdb.reststop.development.velocity.SectionDirective;
import no.vegvesen.nvdb.reststop.servlet.api.ServletBuilder;

import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
@Plugin
public class DevelopmentPlugin  {

    @Export
    private final Collection<Filter> filters  = new ArrayList<>();

    @Export
    private final VelocityEngine velocityEngine;

    public DevelopmentPlugin(@Config(defaultValue = "true") String runTestsOnRedeploy,
                             ReststopPluginManager pluginManager, final ServletBuilder servletBuilder) {

        velocityEngine = initVelocityEngine();


        filters.add(servletBuilder.filter(new DevelopmentAssetsFilter(), FilterPhase.PRE_UNMARSHAL, "/dev/assets/*"));
        filters.add(servletBuilder.filter(new RedeployFilter((DefaultReststopPluginManager) pluginManager, servletBuilder, velocityEngine, "true".equals(runTestsOnRedeploy)), FilterPhase.PRE_UNMARSHAL, "/*" ));

        filters.add(servletBuilder.filter(new RemoteDeployFilter((DefaultReststopPluginManager) pluginManager), FilterPhase.PRE_UNMARSHAL, "/dev/deploy"));
    }


    private VelocityEngine initVelocityEngine() {
        VelocityEngine engine = new VelocityEngine();

        engine.addProperty("resource.loader", "classloader");

        engine.addProperty("classloader.resource.loader.class", ClasspathResourceLoader.class.getName());

        engine.addProperty("userdirective", SectionDirective.class.getName());
        engine.addProperty("eventhandler.include.class", "org.apache.velocity.app.event.implement.IncludeRelativePath");

        engine.init();
        return engine;
    }
}
