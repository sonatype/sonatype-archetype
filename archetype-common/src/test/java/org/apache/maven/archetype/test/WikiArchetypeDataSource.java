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

package org.apache.maven.archetype.test;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author            Jason van Zyl
 * @plexus.component  role-hint="wiki"
 */
public class WikiArchetypeDataSource
implements ArchetypeDataSource
{
    private static String DEFAULT_ARCHETYPE_INVENTORY_PAGE =
        "http://docs.codehaus.org/pages/viewpagesrc.action?pageId=48400";

    static String cleanup( String val )
    {
        val = val.replaceAll( "\\r|\\n|\\s{2,}", "" );
        return val;
    }

    static String cleanupUrl( String val )
    {
        return val.replaceAll( "\\r|\\n|\\s{2,}|\\[|\\]|\\&nbsp;", "" );
    }

    public ArchetypeCatalog getArchetypeCatalog( Properties properties )
    throws ArchetypeDataSourceException
    {
        ArchetypeCatalog ac = new ArchetypeCatalog();
        ac.setArchetypes( getArchetypes( properties ) );
        return ac;
    }

    public List getArchetypes( Properties properties )
    throws ArchetypeDataSourceException
    {
        String url = properties.getProperty( "url" );

        if( url == null )
        {
            url = DEFAULT_ARCHETYPE_INVENTORY_PAGE;
        }

        List archetypes = new ArrayList();

        StringBuffer sb = new StringBuffer();

        try
        {
            InputStream in = new URL( cleanupUrl( url ) ).openStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

            char[] buffer = new char[1024];

            int len = 0;

            while( ( len = reader.read( buffer ) ) > -1 )
            {
                sb.append( buffer, 0, len );
            }
        }
        catch( IOException e )
        {
            throw new ArchetypeDataSourceException(
                "Error retrieving list of archetypes from " + url
            );
        }

        Pattern ptn =
            Pattern.compile(
                "<br>\\|([-a-zA-Z0-9_. ]+)\\|([-a-zA-Z0-9_. ]+)\\|([-a-zA-Z0-9_. ]+)\\|([-a-zA-Z0-9_.:/ \\[\\],]+)\\|([^|]+)\\|"
            );

        Matcher m = ptn.matcher( sb.toString() );

        while( m.find() )
        {
            Archetype archetype = new Archetype();

            archetype.setArtifactId( m.group( 1 ).trim() );

            archetype.setGroupId( m.group( 2 ).trim() );

            String version = m.group( 3 ).trim();

            if( version.equals( "" ) )
            {
                version = "RELEASE";
            }

            archetype.setVersion( version );

            archetype.setRepository( cleanupUrl( m.group( 4 ).trim() ) );

            archetype.setDescription( cleanup( m.group( 5 ).trim() ) );

            archetypes.add( archetype );
        }
        return archetypes;
    }

    public void updateCatalog( Properties properties, Archetype archetype )
    throws ArchetypeDataSourceException
    {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
