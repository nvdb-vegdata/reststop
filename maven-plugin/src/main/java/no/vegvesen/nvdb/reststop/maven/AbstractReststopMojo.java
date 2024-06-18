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

import no.vegvesen.nvdb.reststop.classloaderutils.CircularDependencyException;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.jetty.maven.plugin.MavenWebAppContext;
import org.eclipse.jetty.server.Server;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;

/**
 *
 */
public abstract class AbstractReststopMojo extends AbstractMojo {


    @Component
    protected RepositorySystem repoSystem;

    @Parameter(defaultValue ="${repositorySystemSession}" ,readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    protected List<RemoteRepository> remoteRepos;

    @Parameter (defaultValue = "no.vegvesen.nvdb.reststop:reststop-webapp:war:${plugin.version}")
    protected String warCoords;

    @Parameter (defaultValue = "no.vegvesen.nvdb.reststop:reststop-bootstrap:jar:${plugin.version}")
    protected String bootstrapCoords;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
    private File pluginJar;

    @Parameter(defaultValue = "${project}")
    protected MavenProject mavenProject;

    @Parameter
    protected List<Plugin> basePlugins;

    @Parameter
    protected List<Plugin> plugins;


    @Parameter (defaultValue = "${plugin.version}")
    protected String pluginVersion;

    @Parameter
    protected List<org.apache.maven.model.Dependency> containerDependencies;


    protected void customizeContext(MavenWebAppContext context) {

    }

    protected void afterServerStart(Server server, int port) throws MojoFailureException {

    }

    protected Document createPluginXmlDocument(boolean prod) throws MojoFailureException, MojoExecutionException {

        List<PluginInfo> pluginInfos = getPluginInfos();

        validateCircularDependencies(pluginInfos);

        return buildPluginsDocument(prod, pluginInfos);


    }

    private Document buildPluginsDocument(boolean prod, List<PluginInfo> pluginInfos) throws MojoExecutionException {

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            Element pluginsElem = doc.createElement("plugins");

            doc.appendChild(pluginsElem);

            for (PluginInfo plugin : pluginInfos) {
                Element pluginElem = doc.createElement("plugin");
                pluginsElem.appendChild(pluginElem);

                for (PluginInfo parent : plugin.getParents(pluginInfos)) {
                    Element dependsElem = doc.createElement("depends-on");
                    pluginElem.appendChild(dependsElem);
                    dependsElem.setAttribute("groupId", parent.getGroupId());
                    dependsElem.setAttribute("artifactId", parent.getArtifactId());
                    dependsElem.setAttribute("version", parent.getVersion());

                }

                if(!prod) {
                    if(!plugin.getConfig().isEmpty()) {
                        Element configElem = doc.createElement("config");

                        for (String name : plugin.getConfig().stringPropertyNames()) {
                            Element propElem = doc.createElement("prop");
                            propElem.setAttribute("name", name);
                            propElem.setAttribute("value", plugin.getConfig().getProperty(name));
                            configElem.appendChild(propElem);
                        }

                        pluginElem.appendChild(configElem);
                    }
                }

                pluginElem.setAttribute("groupId", plugin.getGroupId());
                pluginElem.setAttribute("artifactId", plugin.getArtifactId());
                pluginElem.setAttribute("version", plugin.getVersion());
                if(!prod) {

                    if(plugin.getSourceDirectory() != null) {
                        pluginElem.setAttribute("sourceDirectory", plugin.getSourceDirectory().getAbsolutePath());
                    }


                    pluginElem.setAttribute("pluginFile", plugin.getFile().getAbsolutePath());
                }


                List<String> scopes = prod ? Collections.singletonList(JavaScopes.RUNTIME) : asList(JavaScopes.TEST, JavaScopes.RUNTIME, JavaScopes.COMPILE);

                for(String scope : scopes) {

                    Element scopeElem = doc.createElement(scope);

                    pluginElem.appendChild(scopeElem);

                    for (no.vegvesen.nvdb.reststop.classloaderutils.Artifact artifact : plugin.getClassPath(scope)) {
                        Element artifactElement = doc.createElement("artifact");
                        artifactElement.setAttribute("groupId", artifact.getGroupId());
                        artifactElement.setAttribute("artifactId", artifact.getArtifactId());
                        artifactElement.setAttribute("version", artifact.getVersion());

                        if(!prod) {
                            artifactElement.setAttribute("file", artifact.getFile().getAbsolutePath());
                        }

                        scopeElem.appendChild(artifactElement);
                    }


                }
            }
            return doc;
        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<PluginInfo> getPluginInfos() throws MojoFailureException, MojoExecutionException {
        List<PluginInfo> pluginInfos = new Resolver(repoSystem, repoSession, remoteRepos, getLog()).resolve(getPlugins());
        validateTransitivePluginsMissing(pluginInfos);
        validateNoPluginArtifactsOnRuntimeClasspath(pluginInfos);
        return pluginInfos;
    }

    private void validateCircularDependencies(List<PluginInfo> pluginInfos) throws MojoFailureException {
        try {
            PluginInfo.resolveClassloaderOrder(pluginInfos);
        } catch (CircularDependencyException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void validateNoPluginArtifactsOnRuntimeClasspath(List<PluginInfo> pluginInfos) throws MojoExecutionException, MojoFailureException {
        for (PluginInfo pluginInfo : pluginInfos) {

            Map<String, no.vegvesen.nvdb.reststop.classloaderutils.Artifact> shouldBeProvided = new TreeMap<>();

            for (no.vegvesen.nvdb.reststop.classloaderutils.Artifact dep : pluginInfo.getClassPath("runtime")) {


                try {
                    JarFile jar = new JarFile(dep.getFile());
                    ZipEntry entry = jar.getEntry("META-INF/services/ReststopPlugin/");
                    boolean isPlugin = entry != null;
                    jar.close();

                    if(isPlugin) {
                        shouldBeProvided.put(dep.getGroupIdAndArtifactId(), dep);
                        getLog().error("Plugin " + pluginInfo.getPluginId() +" depends on plugin artifact " + dep.getPluginId() +" which must be in <scope>provided</scope> and declared as a <plugin>!");
                        String decl = "\t<plugin>\n\t\t<groupId>%s</groupId>\n\t\t<artifactId>%s</artifactId>\n\t\t<version>%s</version>\n\t</plugin>".formatted(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                        getLog().error("Please add the following to your <plugins> section:\n" + decl);
                    }


                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }

            }
            if(!shouldBeProvided.isEmpty()) {
                throw new MojoFailureException("Plugin " +pluginInfo.getPluginId() +" has a Maven <dependency> on "
                        + "one or more plugin artifacts which should be made <scope>provided</scope> and directly declared as a <plugin>: " + shouldBeProvided.values());
            }
        }
    }

    private void validateTransitivePluginsMissing(List<PluginInfo> pluginInfos) throws MojoExecutionException, MojoFailureException {

        for (PluginInfo pluginInfo : pluginInfos) {

            Map<String, no.vegvesen.nvdb.reststop.classloaderutils.Artifact> missing = new TreeMap<>();

            for (no.vegvesen.nvdb.reststop.classloaderutils.Artifact dep : pluginInfo.getClassPath("compile")) {


                try {
                    JarFile jar = new JarFile(dep.getFile());
                    ZipEntry entry = jar.getEntry("META-INF/services/ReststopPlugin/");
                    boolean isPlugin = entry != null;
                    jar.close();

                    if(isPlugin && !isDeclaredPlugin(dep, pluginInfos)) {
                        missing.put(dep.getGroupIdAndArtifactId(), dep);
                        File pomFile = new File(mavenProject.getBasedir(), "pom.xml");
                        getLog().error("Plugin " + pluginInfo.getPluginId() +" depends on the plugin " + dep.getPluginId() +" which is not declared as a <plugin> in " + pomFile);
                        String decl = "\t<plugin>\n\t\t<groupId>%s</groupId>\n\t\t<artifactId>%s</artifactId>\n\t\t<version>%s</version>\n\t</plugin>".formatted(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
                        getLog().error("Please add the following to maven-reststop-plugin's <plugins> section in " +pomFile + ":\n" + decl);
                    }


                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }

            }
            if(!missing.isEmpty()) {
                throw new MojoFailureException("Plugin " +pluginInfo.getPluginId() +" has a Maven <dependency> on "
                        + "one or more plugin artifacts which should be directly declared as a <plugin>: " + missing.values());
            }
        }

    }

    private boolean isDeclaredPlugin(no.vegvesen.nvdb.reststop.classloaderutils.Artifact dep, List<PluginInfo> pluginInfos) {

        for(PluginInfo declared : pluginInfos) {
            if(declared.getGroupIdAndArtifactId().equals(dep.getGroupIdAndArtifactId())) {
                return true;
            }
        }
        return false;
    }

    protected List<Plugin> getPlugins() {
        List<Plugin> plugins = new ArrayList<>();

        if(this.plugins != null) {
            plugins.addAll(this.plugins);
        }
        if(this.basePlugins != null) {
            plugins.addAll(this.basePlugins);
        }

        return plugins;
    }

    protected File resolveArtifactFile(String coords) throws MojoFailureException, MojoExecutionException {
        return resolveArtifact(coords).getFile();
    }


    protected Artifact resolveArtifact(String coords) throws MojoFailureException, MojoExecutionException {
        return new Resolver(repoSystem, repoSession, remoteRepos, getLog()).resolveArtifact(coords);
    }

    protected File getSourceDirectory(Plugin plugin) {
        String path = repoSession.getLocalRepositoryManager().getPathForLocalArtifact(new DefaultArtifact(plugin.getGroupId(), plugin.getArtifactId(), "sourceDir", plugin.getVersion()));

        File file = new File(repoSession.getLocalRepository().getBasedir(), path);
        try {
            return file.exists() ? new File(Files.readAllLines(file.toPath(), Charset.forName("utf-8")).getFirst()) : null;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void addDevelopmentPlugins(List<Plugin> plugins) {
        {
            Plugin devConsolePlugin = new Plugin("no.vegvesen.nvdb.reststop", "reststop-development-console", pluginVersion);
            plugins.add(devConsolePlugin);
        }

        {
            Plugin developmentPlugin = new Plugin("no.vegvesen.nvdb.reststop", "reststop-development-plugin", pluginVersion);
            plugins.add(developmentPlugin);
        }


        for (Plugin plugin : plugins) {
            plugin.setSourceDirectory(getSourceDirectory(plugin));
        }
    }

    protected List<Artifact> resolveContainerArtifacts(List<org.apache.maven.model.Dependency> containerDependencies) throws MojoFailureException, MojoExecutionException {

        List<Artifact> containerArtifacts = new ArrayList<>();


        List<org.eclipse.aether.graph.Dependency> containerDeps = new ArrayList<>();

        for (org.apache.maven.model.Dependency dependency : containerDependencies) {

            Artifact dependencyArtifact = resolveArtifact(
                    "%s:%s:%s".formatted(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));

            containerDeps.add(new Dependency(dependencyArtifact, JavaScopes.RUNTIME));
        }

        try {
            CollectRequest collectRequest = new CollectRequest(containerDeps, null, remoteRepos);

            final DependencyFilter filter = DependencyFilterUtils.andFilter(
                    DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME),
                    (dependencyNode, list) ->
                            dependencyNode.getDependency() == null || !dependencyNode.getDependency().isOptional());

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

            DependencyResult dependencyResult = repoSystem.resolveDependencies(repoSession, dependencyRequest);

            if (!dependencyResult.getCollectExceptions().isEmpty()) {
                throw new MojoFailureException("Failed resolving plugin dependencies", dependencyResult.getCollectExceptions().getFirst());
            }


            for (ArtifactResult result : dependencyResult.getArtifactResults()) {
                Artifact artifact = result.getArtifact();
                containerArtifacts.add(artifact);
            }

            return containerArtifacts;
        } catch (DependencyResolutionException e) {
            throw new MojoFailureException("Failed resolving plugin dependencies", e);
        }
    }

}
