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

package no.vegvesen.nvdb.reststop.classloaderutils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static java.util.Arrays.asList;

/**
 *
 */
public class PluginInfo extends Artifact {

    private Map<String, List<Artifact>> classpaths = new HashMap<>();
    private File sourceDirectory;
    private List<Artifact> dependsOn = new ArrayList<>();
    private Properties config = new Properties();
    private Integer priority = null;

    public List<Artifact> getClassPath(String scope) {
        if (!classpaths.containsKey(scope)) {
            classpaths.put(scope, new ArrayList<>());
        }

        return classpaths.get(scope);
    }

    public static List<PluginInfo> parse(Document document) {
        List<PluginInfo> infos = new ArrayList<>();

        NodeList pluginElements = document.getDocumentElement().getElementsByTagName("plugin");

        for (int i = 0; i < pluginElements.getLength(); i++) {

            PluginInfo pluginInfo = new PluginInfo();

            infos.add(pluginInfo);

            Element pluginElement = (Element) pluginElements.item(i);

            parseGav(pluginInfo, pluginElement);

            String pluginFile = pluginElement.getAttribute("pluginFile");
            if(!pluginFile.isEmpty()) {
                File pluginJar = new File(pluginFile);
                pluginInfo.setFile(pluginJar);
            }
            String sourceDir = pluginElement.getAttribute("sourceDirectory");
            if(sourceDir != null && !sourceDir.trim().isEmpty()) {
                pluginInfo.setSourceDirectory(new File(sourceDir));
            }

            NodeList dependsOnElems = pluginElement.getElementsByTagName("depends-on");
            for(int d = 0; d < dependsOnElems.getLength(); d++) {
                Element depElem = (Element) dependsOnElems.item(d);
                Artifact depArt = new Artifact();
                parseGav(depArt, depElem);
                pluginInfo.addDependsOn(depArt);
            }


            NodeList configElems = pluginElement.getElementsByTagName("config");

            Properties props = new Properties();
            for(int c = 0; c < configElems.getLength(); c++) {
                Element configElem = (Element) configElems.item(c);

                NodeList propElems = configElem.getElementsByTagName("prop");
                for(int p = 0; p < configElems.getLength(); p++) {
                    Element propElem = (Element) propElems.item(p);

                    props.setProperty(propElem.getAttribute("name"), propElem.getAttribute("value"));
                }

            }

            pluginInfo.setConfig(props);

            for (String scope : asList("test", "runtime", "compile")) {

                NodeList classPathElems = pluginElement.getElementsByTagName(scope);
                if(classPathElems.getLength() > 0) {
                    Element runtimeElement = (Element) classPathElems.item(0);


                    NodeList artifacts = runtimeElement.getElementsByTagName("artifact");


                    for (int a = 0; a < artifacts.getLength(); a++) {
                        Element artifactElement = (Element) artifacts.item(a);
                        String filePath = artifactElement.getAttribute("file");
                        File file = null;
                        if(filePath != null && !"".equals(filePath)) {
                            file = new File(filePath);
                        }
                        Artifact artifact = new Artifact(artifactElement.getAttribute("groupId"),
                                artifactElement.getAttribute("artifactId"),
                                artifactElement.getAttribute("version"), file);

                        pluginInfo.getClassPath(scope).add(artifact);

                    }
                }

            }
        }
        return infos;
    }

    public void addDependsOn(Artifact depArt) {
        dependsOn.add(depArt);
    }

    private static void parseGav(Artifact pluginInfo, Element pluginElement) {
        pluginInfo.setGroupId(pluginElement.getAttribute("groupId"));
        pluginInfo.setArtifactId(pluginElement.getAttribute("artifactId"));
        pluginInfo.setVersion(pluginElement.getAttribute("version"));
    }


    private int readPriority() {


        if(this.getFile() != null) {
            try (JarFile jar = new JarFile(this.getFile())) {
                ZipEntry pluginsEntry = jar.getEntry("META-INF/services/ReststopPlugin/priority.txt");

                if (pluginsEntry != null) {
                    Set<String> lines = readLines(jar.getInputStream(pluginsEntry));
                    if (!lines.isEmpty()) {
                        return Integer.parseInt(lines.iterator().next());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }

    private Set<String> readLines(InputStream inputStream) throws IOException {
        Set<String> lines = new TreeSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public List<PluginInfo> getParents(Collection<PluginInfo> all) {
        List<PluginInfo> deps = new ArrayList<>();

        for (Artifact compile : getClassPath("compile")) {
            for (PluginInfo info : all) {
                if(compile.getGroupIdAndArtifactId().equals(info.getGroupIdAndArtifactId())) {
                    deps.add(info);
                }
            }
        }

        return deps;
    }

    public List<PluginInfo> getChildren(Collection<PluginInfo> all) {
        List<PluginInfo> deps = new ArrayList<>();

            for (PluginInfo info : all) {
                for (Artifact compile : info.getClassPath("compile")) {
                    if(compile.getGroupIdAndArtifactId().equals(getGroupIdAndArtifactId())) {
                        deps.add(info);
                    }
                }
            }


        return deps;
    }

    @Override
    public String toString() {
        return "Plugin " + getGroupId() +":" + getArtifactId() +":" + getVersion();
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public String getPluginId() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }


    public static List<PluginInfo> resolveClassloaderOrder(List<PluginInfo> infos) throws CircularDependencyException {
        return resolveOrder(infos, PluginInfo::getDependsOn);

    }

    public static List<PluginInfo> resolveOrder(List<PluginInfo> infos, Function<PluginInfo, Collection<Artifact>> dependencyFunction) throws CircularDependencyException {

        infos = sortByPriority(new ArrayList<>(infos));

        Map<String, Boolean> colors = new HashMap<>();
        List<PluginInfo> sorted = new LinkedList<>();

        Map<String, PluginInfo> plugins = new HashMap<>();
        for (PluginInfo info : infos) {
            plugins.put(info.getGroupIdAndArtifactId(), info);
        }

        Set<String> ancestors = new LinkedHashSet<>();
        for (PluginInfo info : infos) {
            if (!Boolean.TRUE.equals(colors.get(info.getGroupIdAndArtifactId())))
                dfs(info, plugins, colors, ancestors, sorted, dependencyFunction);
        }
        return sorted;
    }

    private static List<PluginInfo> sortByPriority(ArrayList<PluginInfo> pluginInfos) {
        Collections.sort(pluginInfos, Comparator.comparing(PluginInfo::getPriority));
        return pluginInfos;
    }

    private static void dfs(PluginInfo info, Map<String, PluginInfo> plugins, Map<String, Boolean> colors, Set<String> ancestors, List<PluginInfo> sorted, Function<PluginInfo, Collection<Artifact>> dependencyFunction) {
        detectCircularDependencyChain(info, ancestors);
        ancestors.add(info.getGroupIdAndArtifactId());

        colors.put(info.getGroupIdAndArtifactId(), Boolean.FALSE);
        for (Artifact dep : dependencyFunction.apply(info)) {
            String key = dep.getGroupIdAndArtifactId();
            if (plugins.containsKey(key) && !Boolean.TRUE.equals(colors.get(key))) {
                dfs(plugins.get(key), plugins, colors, ancestors, sorted, dependencyFunction);
            }
        }
        colors.put(info.getGroupIdAndArtifactId(), Boolean.TRUE);
        sorted.add(info);
    }

    private static void detectCircularDependencyChain(PluginInfo info, Set<String> ancestors) {
        if(ancestors.contains(info.getGroupIdAndArtifactId())) {
            StringBuilder chain = new StringBuilder();
            for (String ancestor : ancestors) {
                if(chain.length() > 0 || ancestor.equals(info.getGroupIdAndArtifactId())) {
                    if(chain.length() > 0) {
                        chain.append(" => ");
                    }
                    chain.append(ancestor);
                }
            }
            if(chain.length() > 0) {
                chain.append(" => ");
            }
            chain.append(info.getGroupIdAndArtifactId());
            throw new CircularDependencyException("Detected circular plugin dependency chain: " + chain.toString());
        }
    }

    public String getGroupIdAndArtifactId() {
        return getGroupId() +":" + getArtifactId();
    }

    public List<Artifact> getDependsOn() {
        return dependsOn;
    }

    public void setConfig(Properties config) {
        this.config = cloneProperties(config);
    }

    public Properties getConfig() {
        return cloneProperties(config);
    }

    private static Properties cloneProperties(Properties props) {
        Properties properties = new Properties();
        if(props != null) {
            properties.putAll(props);
        }
        return properties;
    }


    public synchronized int getPriority() {
        if(priority == null) {
            priority = readPriority();
        }
        return priority;
    }
}
