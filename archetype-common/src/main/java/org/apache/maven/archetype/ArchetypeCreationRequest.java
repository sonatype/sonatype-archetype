/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.archetype;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** @author Jason van Zyl */
public class ArchetypeCreationRequest
{
    private ArtifactRepository localRepository;

    private MavenProject project;

//    private File propertyFile;

    private List<String> languages = new ArrayList<String>();

    private List<String> filtereds = new ArrayList<String>();

    private String defaultEncoding = "UTF-8";

    private boolean preserveCData = false;

    private boolean keepParent = true;

    private boolean partialArchetype = false;

    private File archetypeRegistryFile;

    private String packageName;

    private Properties properties;

    private String postPhase;

    private File outputDirectory = new File("target/generated-sources/archetype");

    public String getPostPhase() {
        return postPhase;
    }

    public ArchetypeCreationRequest setPostPhase(final String postPhase) {
        this.postPhase = postPhase;

        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ArchetypeCreationRequest setLocalRepository( final ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ArchetypeCreationRequest setProject( final MavenProject project )
    {
        this.project = project;

        return this;
    }

//    public File getPropertyFile()
//    {
//        return propertyFile;
//    }
//
//    public ArchetypeCreationRequest setPropertyFile( File propertyFile )
//    {
//        this.propertyFile = propertyFile;
//
//        return this;
//    }

    public List<String> getLanguages()
    {
        return languages;
    }

    public ArchetypeCreationRequest setLanguages( final List<String> languages )
    {
        this.languages = languages;

        return this;
    }

    public List<String> getFiltereds()
    {
        return filtereds;
    }

    public ArchetypeCreationRequest setFiltereds( final List<String> filtereds )
    {
        this.filtereds = filtereds;

        return this;
    }

    public String getDefaultEncoding()
    {
        return defaultEncoding;
    }

    public ArchetypeCreationRequest setDefaultEncoding( final String defaultEncoding )
    {
        this.defaultEncoding = defaultEncoding;

        return this;
    }

    public boolean isPreserveCData()
    {
        return preserveCData;
    }

    public ArchetypeCreationRequest setPreserveCData( final boolean preserveCData )
    {
        this.preserveCData = preserveCData;

        return this;
    }

    public boolean isKeepParent()
    {
        return keepParent;
    }

    public ArchetypeCreationRequest setKeepParent( final boolean keepParent )
    {
        this.keepParent = keepParent;

        return this;
    }

    public boolean isPartialArchetype()
    {
        return partialArchetype;
    }

    public ArchetypeCreationRequest setPartialArchetype( final boolean partialArchetype )
    {
        this.partialArchetype = partialArchetype;

        return this;
    }

    public File getArchetypeRegistryFile()
    {
        return archetypeRegistryFile;
    }

    public ArchetypeCreationRequest setArchetypeRegistryFile( final File archetypeRegistryFile )
    {
        this.archetypeRegistryFile = archetypeRegistryFile;

        return this;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public ArchetypeCreationRequest setProperties( final Properties properties )
    {
        this.properties = properties;

        return this;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public ArchetypeCreationRequest setPackageName( final String packageName )
    {
        this.packageName = packageName;

        return this;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public ArchetypeCreationRequest setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
}
