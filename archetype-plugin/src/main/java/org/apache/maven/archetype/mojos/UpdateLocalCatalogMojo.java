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

package org.apache.maven.archetype.mojos;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 * Updates the local catalog
 *
 * @phase install
 * @goal update-local-catalog
 *
 * @author rafale
 */
public class UpdateLocalCatalogMojo
    extends AbstractMojo
    implements ContextEnabled
{
    /** @component */
    private org.apache.maven.archetype.Archetype archetyper;
    
    /** @component role="org.apache.maven.archetype.source.ArchetypeDataSource" */
    private Map archetypeSources;

    /**
     * The project artifact, which should have the LATEST metadata added to it.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The project artifact, which should have the LATEST metadata added to it.
     *
     * @parameter expression="${settings.localRepository}"
     * @required
     * @readonly
     */
    private File localRepository;

    /**
     * The Maven settings.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    public void execute( )
        throws MojoExecutionException
    {
        Archetype archetype = new Archetype(  );
        archetype.setGroupId( project.getGroupId(  ) );
        archetype.setArtifactId( project.getArtifactId(  ) );
        archetype.setVersion( project.getVersion(  ) );
        if (StringUtils.isNotEmpty(project.getDescription()))
        {
            archetype.setDescription(project.getDescription());
        }
        else
        {
            archetype.setDescription(project.getName());
        }
//        archetype.setRepository( localRepository.toString(  ) );
//            archetype.setGoals(project.get);
//            archetype.setProperties(project.get);
        
        archetyper.updateLocalCatalog(archetype);
        /*
        File archetypeCatalogPropertiesFile = new File( System.getProperty( "user.home" ), ".m2/archetype-catalog.properties" );

        if ( archetypeCatalogPropertiesFile.exists(  ) )
        {
            Properties archetypeCatalogProperties = PropertyUtils.loadProperties( archetypeCatalogPropertiesFile );

            getLog(  ).debug( "Updating catalogs " + archetypeCatalogProperties );

            String[] sources = StringUtils.split( archetypeCatalogProperties.getProperty( "sources" ), "," );

            for ( int i = 0; i < sources.length; i++ )
            {
                String sourceRoleHint = sources[i].trim();

                try
                {
                    getLog(  ).debug( "Updating catalog " + sourceRoleHint );

                    ArchetypeDataSource source = (ArchetypeDataSource) archetypeSources.get( sourceRoleHint );

                    source.updateCatalog( getArchetypeSourceProperties( sourceRoleHint, archetypeCatalogProperties ), archetype, settings );

                    getLog(  ).
                        info( "Updated " + sourceRoleHint + " using repository " + localRepository.toString(  ) );
                }
                catch ( ArchetypeDataSourceException ex )
                {
                    getLog(  ).
                        warn( "Can't update " + sourceRoleHint + " using repository " + localRepository.toString(  ) );
                }
            }
        }
        else
        {
            getLog(  ).debug( "Not updating wiki catalog" );
        }
        */
    }

    private Properties getArchetypeSourceProperties( String sourceRoleHint, Properties archetypeCatalogProperties )
    {
        Properties p = new Properties(  );

        for ( Iterator i = archetypeCatalogProperties.keySet(  ).iterator(  ); i.hasNext(  ); )
        {
            String key = (String) i.next();

            if ( key.startsWith( sourceRoleHint ) )
            {
                String k = key.substring( sourceRoleHint.length(  ) + 1 );

                p.setProperty( k, archetypeCatalogProperties.getProperty( key ) );
            }
        }

        return p;
    }
}