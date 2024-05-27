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

import java.io.File;

/**
 *
 */
public class Artifact {
    private String groupId;
    private String artifactId;
    private String version;
    private File file;

    public Artifact(String groupId, String artifactId, String version, File file) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.file = file;
    }

    public Artifact(Artifact artifact) {
        this(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getFile());
    }

    public Artifact() {
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public File getFile() {
        return file;
    }

    public String getPluginId() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public String getGroupIdAndArtifactId() {
        return getGroupId() +":" + getArtifactId();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "Artifact " + getGroupId() +":" + getArtifactId() +":" + getVersion();
    }
}
