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

package no.vegvesen.nvdb.reststop.core;

import no.vegvesen.nvdb.reststop.api.PluginExport;
import no.vegvesen.nvdb.reststop.api.ReststopPluginManager;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;

import java.io.File;
import java.util.*;

/**
 *
 */
public class DefaultReststopPluginManager implements ReststopPluginManager {



    private final PluginDeployer pluginDeployer;
    private final File configFile;

    private volatile PluginState pluginState;


    public DefaultReststopPluginManager(ClassLoader parentClassLoader, File configFile) {
        this(parentClassLoader, configFile, Collections.emptyMap());
    }

    public DefaultReststopPluginManager(ClassLoader parentClassLoader, File configFile, Map<Class, Object> staticServices) {
        this.configFile = configFile;
        this.pluginDeployer = new PluginDeployer(parentClassLoader, configFile);
        Map<Class, Object> services = new HashMap<>(staticServices);
        services.put(ReststopPluginManager.class, this);
        pluginState = new PluginState(services);
    }

    public synchronized void deploy(Collection<PluginInfo> plugins, ClassLoaderFactory classLoaderFactory) {
        pluginState = pluginDeployer.deploy(plugins, classLoaderFactory, pluginState);
    }
    public synchronized void undeploy(Collection<PluginInfo> classloaders) {
        pluginState = pluginDeployer.undeploy(classloaders, pluginState);
    }

    public synchronized void restart(List<LoadedPluginClass> plugins) {
        pluginState = pluginDeployer.restart(plugins, pluginState);
    }

    public synchronized void stop() {
        pluginState = pluginDeployer.undeploy(pluginState.getPluginInfosById().values(), pluginState);
    }

    @Override
    public <T> Collection<T> findExports(Class<T> type) {
        return pluginState.getServices(type);
    }

    @Override
    public Collection<Object> getPlugins() {
        return pluginState.getPlugins();
    }

    @Override
    public <T> Collection<T> getPlugins(Class<T> pluginClass) {
        return pluginState.getPlugins(pluginClass);
    }

    @Override
    public ClassLoader getClassLoader(Object plugin) {
        return pluginState.getClassLoader(plugin);
    }

    @Override
    public Collection<ClassLoader> getPluginClassLoaders() {
        return new ArrayList<>(pluginState.getClassLoaders());
    }

    @Override
    public <T> Collection<PluginExport<T>> findPluginExports(Class<T> type) {
        return pluginState.findExports((Class)type);
    }

    public File getConfigFile() {
        return configFile;
    }

    public PluginState getPluginState() {
        return pluginState;
    }
}
