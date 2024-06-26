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

import jakarta.servlet.ServletException;
import no.vegvesen.nvdb.reststop.classloaderutils.BuildSystem;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginClassLoader;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.jetty.ee10.maven.plugin.MavenWebAppContext;
import org.eclipse.jetty.ee10.webapp.Configurations;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ClassMatcher;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 *
 */
public abstract class AbstractReststopRunMojo extends AbstractReststopMojo {

    @Parameter(required = true)
    protected String applicationName;

    @Parameter (defaultValue = "/")
    private String contextPath;

    @Parameter(defaultValue =  "${basedir}/src/config")
    protected File configDir;


    @Parameter(defaultValue = "${project.build.directory}/reststop/temp")
    private File tempDirectory;


    @Parameter(defaultValue = "${project.build.testOutputDirectory}/reststopPort.txt")
    protected File reststopPortFile;

    @Parameter(defaultValue = "8080")
    private int port;

    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    protected File webAppSourceDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classesDirectory;

    @Component
    private ModelBuilder modelBuilder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        File war;
        if("war".equals(mavenProject.getPackaging())) {
            war = webAppSourceDirectory;
        } else {
            war = resolveArtifactFile(warCoords);
        }


        startJetty(war);


    }

    private void startJetty(File war) throws MojoExecutionException {
        try {
            System.setProperty("reststopPluginDir", mavenProject.getBasedir().getAbsolutePath());

            int port = this.port;
            if(port != 0) {
                port = nextAvailablePort(port);
            }

            Server server = new Server(port);
            Configurations.setServerDefault(server);

            mavenProject.setContextValue("jettyServer", server);

            MavenWebAppContext context = new MavenWebAppContext();

            List<String> serverClasses = asList(
                    "org.eclipse.aether.", // TODO: maybe replace with https://maven.apache.org/resolver/
                    "com.sun.codemodel.",
                    "com.jcraft.",
                    "org.apache.commons.",
                    "org.apache.ant.",
                    "org.apache.http.",
                    "org.apache.maven.",
                    "org.codehaus.plexus.",
                    "org.sonatype.plexus.",
                    "org.eclipse.jgit.",
                    "org.twdata.",
                    "com.googlecode.");

            serverClasses.forEach(serverClass -> context.addHiddenClassMatcher(new ClassMatcher(serverClass)));
            getLog().info("Added system classes: " + serverClasses);

            context.setWar(war.getAbsolutePath());
            context.setContextPath(contextPath);
            context.getServletContext().setAttribute("pluginsXml", createPluginXmlDocument(false));
            context.setInitParameter("pluginConfigurationDirectory", configDir.getAbsolutePath());
            context.setInitParameter("applicationName", applicationName);
            context.setInitParameter("org.eclipse.jetty.websocket.jsr356", "false");

            customizeContext(context);


            File jettyTmpDir = new File(tempDirectory, war.getName());
            jettyTmpDir.mkdirs();
            context.setTempDirectory(jettyTmpDir);
            boolean deleteTempDirectory= jettyTmpDir.exists() && war.lastModified() > jettyTmpDir.lastModified();
            context.setTempDirectoryPersistent(!deleteTempDirectory);
            context.setThrowUnavailableOnStartupException(true);

            Handler.Sequence handlers = new Handler.Sequence();

            handlers.addHandler(new ShutdownHandler(context, server, getLog()));
            server.setHandler(handlers);

            server.start();


            ServerConnector connector = (ServerConnector) server.getConnectors()[0];
            int actualPort = connector.getLocalPort();

            mavenProject.getProperties().setProperty("reststopPort", Integer.toString(actualPort));
            String reststopPort = Integer.toString(actualPort);
            System.setProperty("reststopPort", reststopPort);
            FileUtils.writeStringToFile(reststopPortFile, reststopPort);

            handlers.addHandler(context);

            //configureWebSocket(context);

            context.start();

            afterServerStart(server, actualPort);

        } catch (Exception e) {
            throw new MojoExecutionException("Failed starting Jetty ", e);
        }
    }

    // TODO: Care about web sockets?
//    public static ServerWebSocketContainer configureWebSocket(ServletContextHandler context) throws ServletException
//    {
//
//       // Create Filter
//        WebSocketUpgradeFilter filter = WebSocketUpgradeFilter.configureContext(context);
//
//        // Create the Jetty ServerContainer implementation
//        ServerContainer jettyContainer = new RedeployableServerContainer(filter.getConfiguration(),context.getServer().getThreadPool());
//        context.addBean(jettyContainer, true);
//
//        // Store a reference to the ServerContainer per jakarta.websocket spec 1.0 final section 6.4 Programmatic Server Deployment
//        context.setAttribute(jakarta.websocket.server.ServerContainer.class.getName(),jettyContainer);
//
//        return jettyContainer;
//    }



    private int nextAvailablePort(int first) {
        int port = first;
        for(;;) {
            try {
                ServerSocket socket = new ServerSocket(port);
                socket.close();
                return port;
            } catch (IOException e) {
                port++;
            }
        }
    }

    private class ShutdownHandler extends Handler.Abstract {
        private final WebAppContext context;
        private final Server server;
        private final Log log;

        public ShutdownHandler(WebAppContext context, Server server, Log log) {
            this.context = context;
            this.server = server;
            this.log = log;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            if("/shutdown".equals(request.getHttpURI().getDecodedPath()) && ! (server.isStopping() || server.isStopped())) {                try {
                    log.info("Shutting down Jetty server");
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                context.stop();
                                server.stop();
                            } catch (Throwable e) {
                                log.error(e);
                            }
                        }
                    }.start();
                } catch (Exception e) {
                    throw new ServletException(e);
                }
                return true;
            }
            return false;
        }
    }

    protected void registerBuildSystem() {
        BuildSystem.instance = new BuildSystem() {
            @Override
            public boolean needsRefresh(PluginClassLoader cl) {
                if(cl.getPluginInfo().getSourceDirectory() == null) {
                    return false;
                }

                File pomXml = new File(cl.getPluginInfo().getSourceDirectory(), "pom.xml");

                return cl.getCreationTime() < pomXml.lastModified();
            }

            @Override
            public PluginInfo refresh(PluginInfo pluginInfo) {


                Resolver resolver = new Resolver(repoSystem, repoSession, remoteRepos, getLog());

                try {
                    File pomFile = new File(pluginInfo.getSourceDirectory(), "pom.xml");

                    PluginInfo info = new PluginInfo();
                    info.setGroupId(pluginInfo.getGroupId());
                    info.setArtifactId(pluginInfo.getArtifactId());
                    info.setVersion(pluginInfo.getVersion());
                    info.setSourceDirectory(pluginInfo.getSourceDirectory());

                    Artifact pluginArtifact = resolver.resolveArtifact(info.getGroupId() +":" + info.getArtifactId() +":" + info.getVersion());
                    info.setFile(pluginArtifact.getFile());
                    resolver.resolveClasspaths(info, createCollectRequest(buildModel(pomFile)));
                    return info;
                } catch ( MojoFailureException | DependencyResolutionException | MojoExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            private CollectRequest createCollectRequest(Model model) {
                CollectRequest collectRequest = new CollectRequest();

                for (RemoteRepository repo : remoteRepos) {
                    collectRequest.addRepository(repo);
                }
                for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
                    collectRequest.addDependency(mavenDep2AetherDep(dependency));
                }
                DependencyManagement depm = model.getDependencyManagement();
                if(depm != null) {
                    for (org.apache.maven.model.Dependency dependency : depm.getDependencies()) {
                        collectRequest = collectRequest.addManagedDependency(mavenDep2AetherDep(dependency));
                    }
                }

                return collectRequest;
            }

            private org.eclipse.aether.graph.Dependency mavenDep2AetherDep(Dependency dependency) {
                Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());
                org.eclipse.aether.graph.Dependency aetherDep = new org.eclipse.aether.graph.Dependency(artifact, dependency.getScope());
                if(dependency.getExclusions() != null) {
                    List<Exclusion> exclusions = new ArrayList<>();
                    dependency.getExclusions().forEach( e -> {
                        exclusions.add(new Exclusion(e.getGroupId(), e.getArtifactId(), "*", "*"));
                    });
                    aetherDep = aetherDep.setExclusions(exclusions);
                }
                return aetherDep;
            }

            private Model buildModel(File pomFile) {
                try {
                    DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
                    request.setModelSource(new FileModelSource(pomFile));
                    ModelBuildingResult build = modelBuilder.build(request);
                    return build.getEffectiveModel();
                } catch (ModelBuildingException e) {
                    throw  new RuntimeException(e);
                }
            }
        };
    }
}
