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

package org.apache.maven.archetype.common;

import org.apache.maven.archetype.common.util.ListScanner;
import org.apache.maven.archetype.common.util.PathUtils;
import org.apache.maven.archetype.metadata.FileSet;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Component(role=ArchetypeFilesResolver.class)
public class DefaultArchetypeFilesResolver
    implements ArchetypeFilesResolver
{
    @Requirement
    private Logger log;

    public List<String> getFilesWithExtension(List<String> files, String extension) {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir("");

        scanner.setIncludes("**/*." + extension);
        scanner.setExcludes("");

        return scanner.scan(files);
    }

    public List<String> getFilteredFiles(List<String> files, String filtered) {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir("");

        scanner.setIncludes(filtered);
        scanner.setExcludes("");

        List<String> result = scanner.scan(files);
        log.debug("Scanned " + result.size() + " filtered files in " + files.size() + " files");

        return result;
    }

    public List<String> filterFiles( String moduleOffset,
                             FileSet fileSet,
                             List<String> archetypeResources )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir(
            ( StringUtils.isEmpty( moduleOffset ) ? "" : ( moduleOffset + File.separatorChar ) )
                + fileSet.getDirectory()
        );
        scanner.setIncludes( fileSet.getIncludes() );
        scanner.setExcludes( fileSet.getExcludes() );
        scanner.setCaseSensitive( true );

        return scanner.scan( archetypeResources );
    }

    public List<String> findOtherResources( int level,
                                    List<String> files,
                                    String languages )
    {
        ListScanner scanner = new ListScanner();

        scanner.setBasedir( "" );

        String includes = "";
        for ( int i = 0; i < level; i++ )
        {
            includes += "*/";
        }

        scanner.setIncludes( includes + "**" );
        scanner.setExcludes( languages );

        List<String> result = scanner.scan( files );
        log.debug(
            "Scanned " + result.size() + " other resources in " + files.size()
                + " files at level " + level
        );

        return result;
    }

    public List<String> findOtherResources( int level,
                                    List<String> files,
                                    List<String> sourcesFiles,
                                    String languages )
    {
        ListScanner scanner = new ListScanner();

        scanner.setBasedir( "" );

        Set<String> selectedDirectories = new HashSet<String>();

        List<String> includes = new ArrayList<String>();

        Iterator sourcesFilesIterator = sourcesFiles.iterator();
        while ( sourcesFilesIterator.hasNext() )
        {
            String sourcesFile = (String) sourcesFilesIterator.next();

            String directory = PathUtils.getDirectory( sourcesFile, level - 1 );
            if ( !selectedDirectories.contains( directory ) )
            {
                includes.add( directory + "/**" );
            }

            selectedDirectories.add( directory );
        }

        scanner.setExcludes( languages );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " other resources in " + files.size() + " files at level " + level);

        return result;
    }

    public List<String> findOtherSources( int level,
                                  List<String> files,
                                  String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "" );

        String levelDirectory = "";
        for ( int i = 0; i < ( level - 1 ); i++ )
        {
            levelDirectory += "*/";
        }

        String includes = "";
        String[] languagesAsArray = StringUtils.split( languages );
        for ( int i = 0; i < languagesAsArray.length; i++ )
        {
            includes += levelDirectory + languagesAsArray[i];
        }

        scanner.setIncludes( includes );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " other sources in " + files.size() + " files at level " + level);

        return result;
    }

    public List<String> findResourcesMainFiles( List<String> files,
                                        String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "src/main" );

        scanner.setIncludes( "**" );
        scanner.setExcludes( languages );

        List<String> result = scanner.scan( files );
        log.debug(
            "Scanned " + result.size() + " resources in " + files.size() + " files"
        );

        return result;
    }

    public List<String> findResourcesTestFiles( List<String> files,
                                        String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "src/test" );

        scanner.setIncludes( "**" );
        scanner.setExcludes( languages );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " test resources in " + files.size() + " files");

        return result;
    }

    public List<String> findSiteFiles( List<String> files,
                               String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "src/site" );

        scanner.setIncludes( "**" );
        scanner.setExcludes( languages );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " site resources in " + files.size() + " files");

        return result;
    }

    public List<String> findSourcesMainFiles( List<String> files,
                                      String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "src/main" );

        scanner.setIncludes( languages );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " sources in " + files.size() + " files");

        return result;
    }

    public List<String> findSourcesTestFiles( List<String> files,
                                      String languages )
    {
        ListScanner scanner = new ListScanner();
        scanner.setBasedir( "src/test" );

        scanner.setIncludes( languages );

        List<String> result = scanner.scan( files );
        log.debug("Scanned " + result.size() + " test sources in " + files.size() + " files");

        return result;
    }

    public List<String> getPackagedFiles( List<String> files,
                                  String packageName )
    {
        List<String> packagedFiles = new ArrayList<String>();
        Iterator filesIterator = files.iterator();
        while ( filesIterator.hasNext() )
        {
            String file = (String) filesIterator.next();

            if ( file.startsWith( packageName ) )
            {
                packagedFiles.add( file.substring( packageName.length() + 1 ) );
            }
        }
        log.debug("Scanned " + packagedFiles.size() + " packaged files in " + files.size() + " files");

        return packagedFiles;
    }

    public String resolvePackage( File basedir,
                                  List<String> languages )
        throws
        IOException
    {
        log.debug( "Resolving package in " + basedir + " using languages " + languages );

        List<String> files = resolveFiles( basedir, languages );

        return resolvePackage( files );
    }

//    public List getUnfilteredFiles( List files,
//                                    String filtered )
//    {
//        ListScanner scanner = new ListScanner();
//        scanner.setBasedir( "" );
//
//        scanner.setIncludes( "**" );
//        scanner.setExcludes( filtered );
//
//        List result = scanner.scan( files );
//        log.debug(
//            "Scanned " + result.size() + " unfiltered files in " + files.size() + " files"
//        );
//
//        return result;
//    }

    public List<String> getUnpackagedFiles( List<String> files,
                                    String packageName )
    {
        List<String> unpackagedFiles = new ArrayList<String>();
        Iterator filesIterator = files.iterator();
        while ( filesIterator.hasNext() )
        {
            String file = (String) filesIterator.next();

            if ( !file.startsWith( packageName ) )
            {
                unpackagedFiles.add( file );
            }
        }
        log.debug("Scanned " + unpackagedFiles.size() + " unpackaged files in " + files.size() + " files");
        return unpackagedFiles;
    }

    private String getCommonPackage( String packageName,
                                     String templatePackage )
    {
        String common;
        
        String difference = StringUtils.difference( packageName, templatePackage );
        if ( StringUtils.isNotEmpty( difference ) )
        {
            String temporaryCommon =
                StringUtils.substring(
                    templatePackage,
                    0,
                    templatePackage.lastIndexOf( difference )
                );
            if ( !difference.startsWith( "." ) )
            {
                common =
                    StringUtils.substring(
                        temporaryCommon,
                        0,
                        temporaryCommon.lastIndexOf( "." )
                    );
            }
            else
            {
                common = temporaryCommon;
            }
        }
        else
        {
            common = packageName;
        }

        return common;
    }

    private List<String> resolveFiles( File basedir,
                               List<String> languages )
        throws
        IOException
    {
        String[] languagesArray = languages.toArray( new String[languages.size()] );
        String[] languagesPathesArray = new String[languagesArray.length];
        for ( int i = 0; i < languagesArray.length; i++ )
        {
            languagesPathesArray[i] = "**/src/**/" + languagesArray[i] + "/**";
        }

        String excludes = "target";
        Iterator defaultExcludes = Arrays.asList( ListScanner.DEFAULTEXCLUDES ).iterator();
        while ( defaultExcludes.hasNext() )
        {
            excludes += "," + defaultExcludes.next() + "/**";
        }

        List absoluteFiles =
            FileUtils.getFiles(
                basedir,
                StringUtils.join( languagesPathesArray, "," ),
                excludes
            );

        log.debug( "Found " + absoluteFiles.size() + " potential archetype files" );

        List<String> files = new ArrayList<String>( absoluteFiles.size() );

        Iterator filesIterator = absoluteFiles.iterator();
        while ( filesIterator.hasNext() )
        {
            File file = (File) filesIterator.next();
            String filePath =
                StringUtils.prechomp(
                    file.getAbsolutePath(),
                    basedir.getAbsolutePath() + File.separator
                );

            String minusSrc = StringUtils.prechomp( filePath, "src" + File.separator );

            for ( int i = 0; i < languagesArray.length; i++ )
            {
                String language = languagesArray[i];

                if ( StringUtils.countMatches(
                    minusSrc,
                    File.separator + language + File.separator
                )
                    > 0
                    )
                {
                    String minusLanguage =
                        StringUtils.prechomp( minusSrc, language + File.separator );

                    files.add( toUnixPath( minusLanguage ) );
                }
            }
        }

        log.debug("Found " + files.size() + " archetype files for package resolution ");

        return files;
    }

    private String resolvePackage( List<String> files )
    {
        String packageName = null;
        Iterator minusLanguageIterator = files.iterator();
        while ( minusLanguageIterator.hasNext() )
        {
            String minusLanguage = (String) minusLanguageIterator.next();

            String filePackage;
            if ( minusLanguage.indexOf( "/" ) >= 0 )
            {
                filePackage =
                    StringUtils.replace(
                        minusLanguage.substring( 0, minusLanguage.lastIndexOf( "/" ) ),
                        "/",
                        "."
                    );
            }
            else
            {
                filePackage = "";
            }

            if ( packageName == null )
            {
                packageName = filePackage;
            }
            else
            {
                packageName = getCommonPackage( packageName, filePackage );
            }
        }

        log.debug( "Package resolved to " + packageName );

        return packageName;
    }

    private String toUnixPath( String path )
    {
        return path.replace( File.separatorChar, '/' );
    }
}
