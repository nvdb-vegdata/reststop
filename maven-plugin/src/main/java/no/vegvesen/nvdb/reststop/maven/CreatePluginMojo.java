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

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.Invoker;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates a new Reststop plugins in an already created Reststop Maven project.
 */
@Mojo(name = "create-plugin", requiresProject = true, aggregator = true)
public class CreatePluginMojo extends AbstractCreateMojo {

    @Parameter(defaultValue = "${project.groupId}", property = "groupId")
    private String groupId;

    @Parameter(defaultValue = "${project.version}", property = "version")
    private String version;

    @Parameter(defaultValue = "${project.artifactId}", property = "artifactId")
    private String artifactId;

    @Parameter(property = "name")
    private String pluginName;

    @Parameter(property = "package")
    private String pack;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Component
    private Invoker invoker;

    @Component
    protected RepositorySystem repoSystem;

    @Parameter(defaultValue ="${repositorySystemSession}" ,readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    protected List<RemoteRepository> remoteRepos;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String rootArtifactId;
        File basedir = mavenProject.getBasedir();

        try {

            File pluginsDir;
            File appOrWebappDir;
            if (basedir.getName().equalsIgnoreCase("plugins")) {
                rootArtifactId = mavenProject.getParent().getArtifactId();
                pluginsDir = basedir;
                appOrWebappDir = getAppOrWebapp(basedir.getParentFile());
            } else if (basedir.getName().equalsIgnoreCase("webapp") ||
                    basedir.getName().equalsIgnoreCase("app")) {
                rootArtifactId = mavenProject.getParent().getArtifactId();
                pluginsDir = new File(basedir.getParent(), "plugins");
                if (!pluginsDir.exists()) {
                    pluginsDir.mkdirs();
                }
                appOrWebappDir = basedir;
            } else if (basedir.getName().equalsIgnoreCase(artifactId)) {
                rootArtifactId = artifactId;
                pluginsDir = new File(basedir, "plugins");
                if (!pluginsDir.exists()) {
                    pluginsDir.mkdirs();
                }
                appOrWebappDir = getAppOrWebapp(basedir);
            } else {
                //TODO: Check parent until we find root of project.
                //basedir.getParentFile();
                throw new MojoFailureException("Could not find a proper Reststop directory structure, please use create goal.");
            }
            if (!appOrWebappDir.exists()) {
                throw new MojoFailureException("Could not find the webapp directory, resulting in an improper Reststop directory structure, please use create goal.");
            }

            if(rootArtifactId.endsWith("-parent")) {
                rootArtifactId = rootArtifactId.substring(0, rootArtifactId.indexOf("-parent"));
            }

            Map<String, String> options = getOptions();
            pack = options.get("package").toLowerCase();
            pluginName = options.get("name").toLowerCase();

            File pluginDir = new File(pluginsDir, pluginName);

            if (pluginDir.exists()) {
                throw new MojoFailureException("Plugin %s in %s already exists.".formatted(pluginName, pluginsDir));
            }
            File pluginPomFile = new File(pluginDir, "pom.xml");

            Map<String, String> tokens = new HashMap<>();
            tokens.put("${groupId}", groupId);
            tokens.put("${name}", pluginName);
            tokens.put("${rootArtifactId}", rootArtifactId);
            tokens.put("${version}", mavenProject.getVersion());
            createMavenModule(tokens, getClass().getResourceAsStream("dist/template-newplugin-pom.xml"), pluginPomFile);

            new File(pluginDir, "src/main/resources").mkdirs();
            new File(pluginDir, "src/test/resources").mkdirs();
            File sourceDir = new File(pluginDir, "src/main/java");
            sourceDir.mkdirs();

            File pluginClassFile = createPluginClass(pluginName, sourceDir, pack);
            pomAddModule(new File(pluginsDir, "pom.xml"), pluginName);
            pomAddPluginToReststop(new File(appOrWebappDir, "pom.xml"), groupId, rootArtifactId + "-" + pluginName, "${project.version}");

            addNewFilesToGit(pluginsDir, pluginPomFile, pluginClassFile);

            getLog().info("Successfully generated new plugin '%s' in %s.".formatted(pluginName, pluginDir));

            if(false) {
                Resolver resolver = new Resolver(repoSystem, repoSession, remoteRepos, getLog());
                Deployer deployer = new Deployer(resolver, invoker, getLog());
                deployer.cleanInstall(pluginDir);
                deployer.deployPlugin(groupId, rootArtifactId + "-" + pluginName, mavenProject.getVersion(), pluginDir);
            }

        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }


    }

    private File getAppOrWebapp(File basedir) {
        File webapp = new File(basedir, "webapp");
        if(webapp.exists()) {
            return webapp;
        }
        return new File(basedir, "app");
    }

    private void addNewFilesToGit(File pluginsDir, File pluginPomFile, File pluginClassFile) throws IOException {
        File gitDir = new File(new File(pluginsDir.getParent()), ".git");
        if (gitDir.exists()) {
            File workDir = new File(gitDir.getParent());
            Git git = null;

            try {
                git = Git.open(workDir);
                git.add().addFilepattern(getRelativeTo(pluginPomFile, workDir)).call();
                git.add().addFilepattern(getRelativeTo(pluginClassFile, workDir)).call();
            } catch (GitAPIException e) {
                // ignore
            } finally {
                if (git != null) {
                    git.close();
                }

            }
        }
    }

    private String getRelativeTo(File file, File base) {
        return base.toURI().relativize(file.toURI()).getPath();
    }

    private void pomAddModule(File pom, String name) throws MojoExecutionException {
        Model model = getModelFromPom(pom);

        if (!model.getModules().contains(name)) {
            MavenPomUtils pomUtils = new MavenPomUtils();
            try {
                pomUtils.addModule(pom, pom, name);
            } catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
                throw new MojoExecutionException("Could not add module %s to pom file %s".formatted(name, pom), e);
            }
        }
    }

    private void pomAddPluginToReststop(File pom, String groupId, String artifactId, String version) throws MojoExecutionException {
        Model model = getModelFromPom(pom);

        boolean present = false;
        for (Plugin plugin : model.getBuild().getPlugins()) {
            if (plugin.getArtifactId().equalsIgnoreCase("reststop-maven-plugin")) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                Xpp3Dom plugins = configuration.getChild("plugins");
                Xpp3Dom[] children = plugins.getChildren();
                if (children.length != 0) {
                    for (Xpp3Dom p : children) {
                        if (p.getChild("artifactId").getValue().equalsIgnoreCase(artifactId)) {
                            present = true;
                        }
                    }

                    if (!present) {
                        MavenPomUtils pomUtils = new MavenPomUtils();
                        try {
                            pomUtils.addPluginToReststop(pom, pom, groupId, artifactId, version);
                        } catch (IOException | ParserConfigurationException | TransformerException | SAXException | XPathExpressionException e) {
                            throw new MojoExecutionException("Could not add plugin %s:%s:%s to pom file %s".formatted(groupId, artifactId, version, pom), e);
                        }
                    }
                }
            }
        }
    }


    private Model getModelFromPom(File pom) throws MojoExecutionException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try {
            model = reader.read(new FileInputStream(pom));
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read file: %s".formatted(pom), e);
        } catch (XmlPullParserException e) {
            throw new MojoExecutionException("Error parsing XML in file: %s: ".formatted(pom), e);
        }
        return model;
    }

    private Map<String, String> getOptions() {
        Map<String, String> values = new LinkedHashMap<>();

        do {

            boolean onlyParameterValues = true;
            if (pluginName == null) {
                onlyParameterValues = false;
                readValue(values, "name", "example");
            } else {
                values.put("name", pluginName);
            }

            if (pack == null) {
                onlyParameterValues = false;
                String defaultPackage = groupId + "." + removeSpecialCharactersAndCapitalize(values.get("name")).toLowerCase();
                String pack;
                for (; ; ) {
                    pack = readLineWithDefault("package", defaultPackage).trim();
                    if (pack.isEmpty()) pack = defaultPackage;

                    Pattern p = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");
                    if (p.matcher(pack).matches()) {
                        break;
                    }
                }

                values.put("package", pack);
            } else {
                values.put("package", pack);
            }

            if(onlyParameterValues) return values;

            System.out.println();
            System.out.println("Please confirm configuration:");
            for (String option : values.keySet()) {
                System.console().printf("  %s = '%s'\n", option, values.get(option));
            }
        } while (!System.console().readLine(" Y: ").equalsIgnoreCase("y"));

        return values;
    }

}
