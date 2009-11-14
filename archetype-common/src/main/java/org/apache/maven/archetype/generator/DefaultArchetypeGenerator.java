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

package org.apache.maven.archetype.generator;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.common.ArchetypeConfiguration;
import org.apache.maven.archetype.common.ArchetypeRegistryManager;
import org.apache.maven.archetype.exception.ArchetypeGenerationFailure;
import org.apache.maven.archetype.exception.ArchetypeNotConfigured;
import org.apache.maven.archetype.exception.ArchetypeNotDefined;
import org.apache.maven.archetype.exception.InvalidPackaging;
import org.apache.maven.archetype.exception.OutputFileExists;
import org.apache.maven.archetype.exception.PomFileExists;
import org.apache.maven.archetype.exception.ProjectDirectoryExists;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.old.OldArchetype;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(role=ArchetypeGenerator.class)
public class DefaultArchetypeGenerator
    implements ArchetypeGenerator
{
    @Requirement
    private Logger log;
    
    @Requirement
    private ArchetypeRegistryManager archetypeRegistryManager;

    @Requirement
    private ArchetypeArtifactManager archetypeArtifactManager;

    @Requirement
    private FilesetArchetypeGenerator filesetGenerator;

    @Requirement
    private OldArchetype oldArchetype;

    private void generateArchetype(
        ArchetypeGenerationRequest request,
        ArtifactRepository localRepository,
        String basedir
    )
        throws
            IOException,
            ArchetypeNotDefined,
            UnknownArchetype,
            ArchetypeNotConfigured,
            ProjectDirectoryExists,
            PomFileExists,
            OutputFileExists,
            XmlPullParserException,
            DocumentException,
            InvalidPackaging,
            ArchetypeGenerationFailure
    {
        assert request != null;

        if ( !isArchetypeDefined( request ) )
        {
            throw new ArchetypeNotDefined( "The archetype is not defined" );
        }

        List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>( /*repositories*/ );

        ArtifactRepository remoteRepo = null;
        if ( request != null  && request.getArchetypeRepository() != null )
        {
            remoteRepo=archetypeRegistryManager.createRepository(
                request.getArchetypeRepository(),
                request.getArchetypeArtifactId() + "-repo" );

            repos.add( remoteRepo );
        }

        if ( !archetypeArtifactManager.exists(
            request.getArchetypeGroupId(),
            request.getArchetypeArtifactId(),
            request.getArchetypeVersion(),
            remoteRepo,
            localRepository,
            repos ))
        {
            throw new UnknownArchetype(
                "The desired archetype does not exist (" + request.getArchetypeGroupId() + ":"
                    + request.getArchetypeArtifactId() + ":" + request.getArchetypeVersion()
                    + ")"
            );
        }

        if ( archetypeArtifactManager.isFileSetArchetype(
            request.getArchetypeGroupId(),
            request.getArchetypeArtifactId(),
            request.getArchetypeVersion(),remoteRepo,
            localRepository,
            repos))
        {
            processFileSetArchetype(
                request,remoteRepo,
                localRepository,
                basedir,
                repos
            );
        }
        else if (
            archetypeArtifactManager.isOldArchetype(
                request.getArchetypeGroupId(),
                request.getArchetypeArtifactId(),
                request.getArchetypeVersion(),remoteRepo,
                localRepository,
                repos ) )
        {
            processOldArchetype(
                request,remoteRepo,
                localRepository,
                basedir,
                repos
            );
        }
        else
        {
            throw new ArchetypeGenerationFailure( "The defined artifact is not an archetype" );
        }
    }

    /** Common */
    public String getPackageAsDirectory( String packageName )
    {
        return StringUtils.replace( packageName, ".", "/" );
    }

    private boolean isArchetypeDefined( ArchetypeGenerationRequest request )
    {
        assert request != null;
        return org.codehaus.plexus.util.StringUtils.isNotEmpty( request.getArchetypeGroupId() )
            && org.codehaus.plexus.util.StringUtils.isNotEmpty( request.getArchetypeArtifactId() )
            && org.codehaus.plexus.util.StringUtils.isNotEmpty( request.getArchetypeVersion() );
    }

    /** FileSetArchetype */
    private void processFileSetArchetype(
        final ArchetypeGenerationRequest request,
        ArtifactRepository remoteRepo,
        final ArtifactRepository localRepository,
        final String basedir,
        final List<ArtifactRepository> repositories
    )
        throws
            UnknownArchetype,
            ArchetypeNotConfigured,
            ProjectDirectoryExists,
            PomFileExists,
            OutputFileExists,
            ArchetypeGenerationFailure
        {
        //TODO: get rid of the property file usage.
//        Properties properties = request.getProperties();
//
//        properties.setProperty( Constants.ARCHETYPE_GROUP_ID, request.getArchetypeGroupId() );
//
//        properties.setProperty( Constants.ARCHETYPE_ARTIFACT_ID, request.getArchetypeArtifactId() );
//
//        properties.setProperty( Constants.ARCHETYPE_VERSION, request.getArchetypeVersion() );
//
//        properties.setProperty( Constants.GROUP_ID, request.getGroupId(  ) );
//
//        properties.setProperty( Constants.ARTIFACT_ID, request.getArtifactId(  ) );
//
//        properties.setProperty( Constants.VERSION, request.getVersion() );
//
//        properties.setProperty( Constants.PACKAGE, request.getPackage(  ) );
//
//        properties.setProperty( Constants.ARCHETYPE_POST_GENERATION_GOALS, request.getArchetypeGoals() );

        File archetypeFile =
            archetypeArtifactManager.getArchetypeFile(
                request.getArchetypeGroupId(),
                request.getArchetypeArtifactId(),
                request.getArchetypeVersion(),remoteRepo,
                localRepository,
                repositories
            );

        filesetGenerator.generateArchetype( request, archetypeFile, basedir );
    }

    private void processOldArchetype(
        ArchetypeGenerationRequest request,
        ArtifactRepository remoteRepo,
        ArtifactRepository localRepository,
        String basedir,
        List<ArtifactRepository> repositories
    )
        throws
            UnknownArchetype,
            ArchetypeGenerationFailure
    {
        ArchetypeConfiguration archetypeConfiguration;

        org.apache.maven.archetype.old.descriptor.ArchetypeDescriptor archetypeDescriptor =
            archetypeArtifactManager.getOldArchetypeDescriptor(
                request.getArchetypeGroupId(),
                request.getArchetypeArtifactId(),
                request.getArchetypeVersion(),remoteRepo,
                localRepository,
                repositories
            );

        Map<String,String> map = new HashMap<String,String>();

        map.put( "basedir", basedir );

        map.put( "package", request.getPackage() );

        map.put( "packageName", request.getPackage() );

        map.put( "groupId", request.getGroupId() );

        map.put( "artifactId", request.getArtifactId() );

        map.put( "version", request.getVersion() );
//        try
//        {
            oldArchetype.createArchetype(
                request.getArchetypeGroupId(),
                request.getArchetypeArtifactId(),
                request.getArchetypeVersion(),remoteRepo,
                localRepository,
                repositories,
                map
            );
//        }
//        catch ( ArchetypeDescriptorException ex )
//        {
//            throw new ArchetypeGenerationFailure(
//                "Failed to generate project from the old archetype", ex
//            );
//        }
//        catch ( ArchetypeTemplateProcessingException ex )
//        {
//            throw new ArchetypeGenerationFailure(
//                "Failed to generate project from the old archetype", ex
//            );
//        }
//        catch ( ArchetypeNotFoundException ex )
//        {
//            throw new ArchetypeGenerationFailure(
//                "Failed to generate project from the old archetype", ex
//            );
//        }
    }

    public void generateArchetype( ArchetypeGenerationRequest request, ArchetypeGenerationResult result )
    {
        assert request != null;
        assert result != null;
        
        try
        {
            generateArchetype( request, request.getLocalRepository(), request.getOutputDirectory() );
        }
        catch ( IOException ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( ArchetypeNotDefined ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( UnknownArchetype ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( ArchetypeNotConfigured ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( ProjectDirectoryExists ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( PomFileExists ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( OutputFileExists ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( XmlPullParserException ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( DocumentException ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( InvalidPackaging ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
        catch ( ArchetypeGenerationFailure ex )
        {
            log.error(ex.getMessage(), ex);
            result.setCause(ex);
        }
    }
}
