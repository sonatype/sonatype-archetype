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

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.wagon.events.TransferListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** @author Jason van Zyl */
public class ArchetypeGenerationRequest
{
    private boolean offline;

    private boolean interactiveMode;

    private TransferListener transferListener;

    private String outputDirectory;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteArtifactRepositories;
    
    private Proxy activeProxy;

    private List<Server> servers = new ArrayList<Server>();

    private List<Mirror> mirrors = new ArrayList<Mirror>();

    // Archetype definition
    private String archetypeName;

    private String archetypeGroupId;

    private String archetypeArtifactId;

    private String archetypeVersion;

    private String archetypeGoals = "";

    private String archetypeRepository;

    // Archetype configuration
    private String groupId;

    private String artifactId;

    private String version;

    private String packageName;

    private Properties properties = new Properties(  );

    public ArchetypeGenerationRequest( )
    {
    }

    public ArchetypeGenerationRequest( final Archetype archetype )
    {
        this.archetypeGroupId = archetype.getGroupId(  );

        this.archetypeArtifactId = archetype.getArtifactId(  );

        this.archetypeVersion = archetype.getVersion(  );

        this.archetypeRepository = archetype.getRepository(  );
    }

    public String getArchetypeGroupId( )
    {
        return archetypeGroupId;
    }

    public ArchetypeGenerationRequest setArchetypeGroupId( final String archetypeGroupId )
    {
        this.archetypeGroupId = archetypeGroupId;

        return this;
    }

    public String getArchetypeArtifactId( )
    {
        return archetypeArtifactId;
    }

    public ArchetypeGenerationRequest setArchetypeArtifactId( final String archetypeArtifactId )
    {
        this.archetypeArtifactId = archetypeArtifactId;

        return this;
    }

    public String getArchetypeVersion( )
    {
        return archetypeVersion;
    }

    public ArchetypeGenerationRequest setArchetypeVersion( final String archetypeVersion )
    {
        this.archetypeVersion = archetypeVersion;

        return this;
    }

    public String getArchetypeGoals( )
    {
        return archetypeGoals;
    }

    public ArchetypeGenerationRequest setArchetypeGoals( final String archetypeGoals )
    {
        this.archetypeGoals = archetypeGoals;

        return this;
    }

    public String getArchetypeName( )
    {
        return archetypeName;
    }

    public ArchetypeGenerationRequest setArchetypeName( final String archetypeName )
    {
        this.archetypeName = archetypeName;

        return this;
    }

    public String getArchetypeRepository( )
    {
        return archetypeRepository;
    }

    public ArchetypeGenerationRequest setArchetypeRepository( final String archetypeRepository )
    {
        this.archetypeRepository = archetypeRepository;

        return this;
    }

    public String getArtifactId( )
    {
        return artifactId;
    }

    public ArchetypeGenerationRequest setArtifactId( final String artifactId )
    {
        this.artifactId = artifactId;

        return this;
    }

    public String getGroupId( )
    {
        return groupId;
    }

    public ArchetypeGenerationRequest setGroupId( final String groupId )
    {
        this.groupId = groupId;

        return this;
    }

    public String getVersion( )
    {
        return version;
    }

    public ArchetypeGenerationRequest setVersion( final String version )
    {
        this.version = version;

        return this;
    }

    public String getPackage( )
    {
        return packageName;
    }

    public ArchetypeGenerationRequest setPackage( final String packageName )
    {
        this.packageName = packageName;

        return this;
    }

    public Properties getProperties( )
    {
        return properties;
    }

    public ArchetypeGenerationRequest setProperties( final Properties additionalProperties )
    {
        this.properties = additionalProperties;

        return this;
    }

    public ArtifactRepository getLocalRepository( )
    {
        return localRepository;
    }

    public ArchetypeGenerationRequest setLocalRepository( final ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public String getOutputDirectory( )
    {
        return outputDirectory;
    }

    public ArchetypeGenerationRequest setOutputDirectory( final String outputDirectory )
    {
        this.outputDirectory = outputDirectory;

        return this;
    }

    public boolean isInteractiveMode( )
    {
        return interactiveMode;
    }

    public ArchetypeGenerationRequest setInteractiveMode( final boolean interactiveMode )
    {
        this.interactiveMode = interactiveMode;

        return this;
    }

    public boolean isOffline( )
    {
        return offline;
    }

    public ArchetypeGenerationRequest setOffline( final boolean offline )
    {
        this.offline = offline;

        return this;
    }

    public TransferListener getTransferListener( )
    {
        return transferListener;
    }

    public ArchetypeGenerationRequest setTransferListener( final TransferListener transferListener )
    {
        this.transferListener = transferListener;

        return this;
    }

    public Proxy getActiveProxy( )
    {
        return activeProxy;
    }

    public ArchetypeGenerationRequest setActiveProxy( final Proxy activeProxy )
    {
        this.activeProxy = activeProxy;

        return this;
    }

    public ArchetypeGenerationRequest addMirror( final Mirror mirror )
    {
        mirrors.add( mirror );

        return this;
    }

    public List<Mirror> getMirrors( )
    {
        return mirrors;
    }

    public ArchetypeGenerationRequest addMirror( final Server server )
    {
        servers.add( server );

        return this;
    }

    public List<Server> getServers( )
    {
        return servers;
    }
    
    public List<ArtifactRepository> getRemoteArtifactRepositories()
    {
        return remoteArtifactRepositories;
    }

    public ArchetypeGenerationRequest setRemoteArtifactRepositories( final List<ArtifactRepository> remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;

        return this;
    }
}
