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

package no.vegvesen.nvdb.reststop.developmentconsole;

import no.vegvesen.nvdb.reststop.classloaderutils.Artifact;
import no.vegvesen.nvdb.reststop.classloaderutils.DelegateClassLoader;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginClassLoader;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import no.vegvesen.nvdb.reststop.api.Export;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ConsoleTool {

    public Map<Class, Object> getExports(Object plugin) throws IllegalAccessException {

        Map<Class, Object> exports = new HashMap<>();

        for (Field field : plugin.getClass().getDeclaredFields()) {
            if (field.getAnnotation(Export.class) != null) {
                field.setAccessible(true);
                exports.put(field.getType(), field.get(plugin));
            }
        }
        return exports;
    }

    public boolean isParentUsed(PluginClassLoader classLoader, PluginInfo parent) {
        if(classLoader.getParent().getParent() instanceof DelegateClassLoader) {
            DelegateClassLoader delegateClassLoader = (DelegateClassLoader) classLoader.getParent().getParent();

            return delegateClassLoader.isParentUsed(parent);
        } else {
            return false;
        }

    }

    public long runtimeSize(PluginClassLoader classLoader) {

        return classLoader.getPluginInfo().getClassPath("runtime").stream()
                .map(Artifact::getFile)
                .mapToLong(File::length)
                .sum();
    }
}
