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

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.artifact.Artifact;
import no.vegvesen.nvdb.reststop.bootstrap.Bootstrap;
import no.vegvesen.nvdb.reststop.bootstrap.BootstrapHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 *
 */
@Mojo(name = "boot-start",
        defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresDirectInvocation = true,
        requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.PACKAGE)

public class StartBootMojo extends AbstractReststopRunMojo {


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            preExecute();

            Document pluginsXml = createPluginXmlDocument(false);

            List<URL> dependencies = addBootstrapClasspath(pluginsXml);

            BootstrapHelper helper = new BootstrapHelper();

            ClassLoader mavenHidingClassLoader = new MavenHidingClassLoader((ClassRealm) getClass().getClassLoader());

            ClassLoader classLoader = helper.createClassLoader(dependencies, mavenHidingClassLoader);

            ServiceLoader<Bootstrap> load = ServiceLoader.load(Bootstrap.class, classLoader);
            Iterator<Bootstrap> iterator = load.iterator();
            if(! iterator.hasNext()) {
                throw new IllegalStateException("Could not find any service instance of " + Bootstrap.class +" in class path " + dependencies);
            }

            List<Bootstrap> bootstraps = new ArrayList<>();

            for (Bootstrap bootstrap : load) {
                bootstraps.add(bootstrap);
            }

            for (Bootstrap bootstrap : load) {
                bootstrap.preBootstrap();
            }
            for (Bootstrap bootstrap : load) {
                bootstrap.bootstrap(new File(configDir, applicationName +".conf"), pluginsXml, null, classLoader);
            }
            for (Bootstrap bootstrap : load) {
                bootstrap.postBootstrap();
            }


            FileUtils.writeStringToFile(reststopPortFile, System.getProperty("reststopPort"));

            postExecute(bootstraps);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    protected void postExecute(List<Bootstrap> bootstraps) {
        mavenProject.setContextValue("stopHook", (Runnable) () -> {
            Collections.reverse(bootstraps);
            for (Bootstrap bootstrap : bootstraps) {
                bootstrap.shutdown();
            }
        });
    }

    protected void preExecute() {
        // Do nothing
    }

    private List<URL> addBootstrapClasspath(Document pluginXmlDocument) throws MojoFailureException, MojoExecutionException, MalformedURLException {
        List<Dependency> deps = new ArrayList<>();
        if (containerDependencies != null) {
            deps.addAll(containerDependencies);
        }
        org.apache.maven.model.Dependency reststopCore = new org.apache.maven.model.Dependency();
        reststopCore.setGroupId("no.vegvesen.nvdb.reststop");
        reststopCore.setArtifactId("reststop-core");
        reststopCore.setVersion(pluginVersion);

        deps.add(reststopCore);
        List<Artifact> containerArtifacts = resolveContainerArtifacts(deps);
        List<URL> dependencyLocations = new ArrayList<>(containerArtifacts.size());
        for (Artifact containerArtifact : containerArtifacts) {
            Element common = pluginXmlDocument.createElement("common");
            common.setAttribute("groupId", containerArtifact.getGroupId());
            common.setAttribute("artifactId", containerArtifact.getArtifactId());
            common.setAttribute("version", containerArtifact.getBaseVersion());
            pluginXmlDocument.getDocumentElement().appendChild(common);
            dependencyLocations.add(containerArtifact.getFile().toURI().toURL());
        }
        return dependencyLocations;
    }

    public List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<>(super.getPlugins());
        if (new File(mavenProject.getBasedir(), "target/classes/META-INF/services/ReststopPlugin").exists()) {
            plugins.add(new Plugin(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion()));
        }

        return plugins;
    }

    private class MavenHidingClassLoader extends ClassLoader {

        private final ClassRealm classRealm;

        public MavenHidingClassLoader(ClassRealm classLoader) {
            super(classLoader);
            classRealm = classLoader;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            try {
                return classRealm.getParentClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                if(isHiddenPackage(name)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }

        }

        @Override
        protected Package getPackage(String name) {
            Package pack = super.getPackage(name);
            return isHiddenPackage(name) ? null : pack;
        }

        @Override
        public URL getResource(String name) {
            URL resource = classRealm.getResource(name);

            if(resource == null) {
                return null;
            } else {
                URL foundResource = classRealm.findResource(name);

                if (foundResource != null && foundResource.toString().equals(resource.toString())) {
                    return isHiddenPackage(name) ? null : foundResource;
                } else {
                    return resource;
                }
            }
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Map<String, URL> resources = new HashMap<>();
            Enumeration<URL> e = classRealm.getResources(name);
            while(e.hasMoreElements()) {
                URL resource = e.nextElement();
                resources.put(resource.toString(), resource);
            }
            if(isHiddenPackage(name)) {
                Enumeration<URL> localResources = classRealm.findResources(name);
                while (localResources.hasMoreElements()) {
                    URL localResource = localResources.nextElement();
                    resources.remove(localResource.toString());
                }
            }
            return Collections.enumeration(resources.values());
        }

        private boolean isHiddenPackage(String name) {
            name = name.replace('/', '.');

            return !name.startsWith("no.vegvesen.nvdb.reststop.bootstrap");
        }
    }
}
