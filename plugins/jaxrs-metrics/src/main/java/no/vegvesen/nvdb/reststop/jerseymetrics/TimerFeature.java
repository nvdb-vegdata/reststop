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

package no.vegvesen.nvdb.reststop.jerseymetrics;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

/**
 *
 */
@Provider
public class TimerFeature implements DynamicFeature {


    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {

        Path methodPath = resourceInfo.getResourceMethod().getAnnotation(Path.class);
        Path classPath  = resourceInfo.getResourceClass().getAnnotation(Path.class);

        Path path = methodPath != null ? methodPath : classPath;
        if(path != null) {
            UriBuilder builder = methodPath != null
                    ? UriBuilder.fromResource(resourceInfo.getResourceClass()).path(resourceInfo.getResourceClass(),resourceInfo.getResourceMethod().getName())
                    : UriBuilder.fromResource(resourceInfo.getResourceClass());

            String template = builder.toTemplate();
            context.register(new TimerBeforeFilter(template));
            context.register(TimerAfterFilter.class);
        }
    }
}
