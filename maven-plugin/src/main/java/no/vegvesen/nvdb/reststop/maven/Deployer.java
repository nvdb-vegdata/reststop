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

import no.vegvesen.nvdb.reststop.classloaderutils.Artifact;
import no.vegvesen.nvdb.reststop.classloaderutils.PluginInfo;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 *
 */
public class Deployer {

    private final Resolver resolver;
    private final Invoker invoker;
    private final Log log;

    public Deployer(Resolver resolver, Invoker invoker, Log log) {
        this.resolver = resolver;
        this.invoker = invoker;
        this.log = log;
    }

    public void deployPlugin(String groupId, String artifactId, String version, File basedir) throws MojoFailureException, MojoExecutionException {
        Plugin plugin = new Plugin(groupId, artifactId, version);

        PluginInfo resolved = resolver.resolve(plugin);

        sendDeployPost(resolved, basedir);
    }

    private void sendDeployPost(PluginInfo resolved, File basedir) throws MojoExecutionException {
        try {
            HttpURLConnection con = (HttpURLConnection) URI.create("http://localhost:8080/dev/deploy").toURL().openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try(OutputStream out = con.getOutputStream()) {
                out.write(getDeployParams(resolved, basedir));
            }
            String s = IOUtils.toString(con.getInputStream());
            log.info("Server said: " + con.getResponseCode() +": " + s);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private byte[] getDeployParams(PluginInfo resolved, File basedir) {
        StringBuilder sb = new StringBuilder();

        addParam(sb, "groupId", resolved.getGroupId());
        addParam(sb, "artifactId", resolved.getArtifactId());
        addParam(sb, "version", resolved.getVersion());
        addParam(sb, "basedir", basedir.getAbsolutePath());
        for(String scope : asList(JavaScopes.TEST, JavaScopes.RUNTIME, JavaScopes.COMPILE)) {
            addParam(sb, scope, getClassPath(resolved, scope));
        }

        try {
            return sb.toString().getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getClassPath(PluginInfo pluginInfo, String scope) {

        StringBuilder sb = new StringBuilder();

        for (Artifact artifact : pluginInfo.getClassPath(scope)) {
            if(sb.length() > 0) {
                sb.append(";");
            }
            sb.append(Stream.of(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getFile().getAbsolutePath())
                    .collect(Collectors.joining(":")));
        }
        return sb.toString();
    }

    private void addParam(StringBuilder sb, String name, String value) {
        if(sb.length()> 0) {
            sb.append("&");
        }
        sb.append(name).append("=").append(value);
    }

    public void cleanInstall(File pluginDir) throws MojoExecutionException {
        maven(pluginDir, "clean", "install");
    }


    public void validate(File pluginDir) throws MojoExecutionException {
        maven(pluginDir, "validate");
    }


    public void maven(File pluginDir, String... cmd) throws MojoExecutionException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(pluginDir, "pom.xml"));
        request.setGoals(asList(cmd));
        request.addShellEnvironment("MAVEN_DEBUG_OPTS", "");
        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Failed executing mvn clean install on created project", e);
        }
    }
}
