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

package no.vegvesen.nvdb.reststop.core.bootstrap;

import no.vegvesen.nvdb.reststop.bootstrap.Bootstrap;
import no.vegvesen.nvdb.reststop.classloaderutils.Artifact;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginClassLoader;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import no.vegvesen.nvdb.reststop.core.DefaultReststopPluginManager;
import no.vegvesen.nvdb.reststop.core.ClassLoaderFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

/**
 *
 */
@SuppressWarnings("Duplicates")
public class DefaultBootstrap implements Bootstrap {

    private DefaultReststopPluginManager manager;


    @Override
    public void bootstrap(File globalConfigurationFile, Document pluginsXml, File repositoryDirectory, ClassLoader parentClassLoader) {
        List<PluginInfo> parsed = PluginInfo.parse(pluginsXml);

        manager = new DefaultReststopPluginManager(parentClassLoader, globalConfigurationFile);

        ClassLoaderFactory classLoaderFactory = new DefaultClassLoaderFactory(repositoryDirectory);

        deployPlugins(parsed, classLoaderFactory);

    }

    private void deployPlugins(List<PluginInfo> plugins, ClassLoaderFactory classLoaderFactory) {
        manager.deploy(plugins, classLoaderFactory);
    }



    private File getPluginFile(Artifact artifact, File repoDir) {
        if (repoDir != null) {
            return new File(repoDir,
                    artifact.getGroupId().replace('.', '/') + "/"
                            + artifact.getArtifactId() + "/"
                            + artifact.getVersion() + "/"
                            + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");

        } else {
            return artifact.getFile();
        }
    }


    @Override
    public void shutdown() {
        manager.stop();
    }

    private class DefaultClassLoaderFactory implements ClassLoaderFactory {

        private final File repositoryDirectory;


        private DefaultClassLoaderFactory(File repositoryDirectory) {
            this.repositoryDirectory = repositoryDirectory;
        }

        @Override
        public PluginClassLoader createPluginClassLoader(PluginInfo info, ClassLoader parentClassLoader, List<PluginInfo> allPlugins) {
            PluginClassLoader pluginClassloader = new PluginClassLoader(info, parentClassLoader);

            File pluginJar = getPluginFile(info, repositoryDirectory);

            try {
                pluginClassloader.addURL(pluginJar.toURI().toURL());
                info.setFile(pluginJar);

                for (Artifact artifact : info.getClassPath("runtime")) {
                    pluginClassloader.addURL(getPluginFile(artifact, repositoryDirectory).toURI().toURL());

                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return pluginClassloader;
        }
    }
}
