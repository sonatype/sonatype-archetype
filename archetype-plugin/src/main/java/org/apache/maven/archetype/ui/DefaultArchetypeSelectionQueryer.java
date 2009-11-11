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

package org.apache.maven.archetype.ui;

//import org.apache.maven.archetype.common.Archetype;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeDefinition;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.*;

@Component(role=ArchetypeSelectionQueryer.class)
public class DefaultArchetypeSelectionQueryer
    extends AbstractLogEnabled
    implements ArchetypeSelectionQueryer
{
    @Requirement(hint="archetype")
    private Prompter prompter;

    public boolean confirmSelection( ArchetypeDefinition archetypeDefinition )
        throws
        PrompterException
    {
        String query =
            "Confirm archetype selection: \n" + archetypeDefinition.getGroupId() + "/"
                + archetypeDefinition.getName() + "\n";

        String answer = prompter.prompt( query, Arrays.asList( new String[]{"Y", "N"} ), "Y" );

        return "Y".equalsIgnoreCase( answer );
    }

    public Archetype selectArchetype( List archetypes )
        throws PrompterException
    {
        String query = "Choose archetype:\n";
        Map answerMap = new HashMap();
        List answers = new ArrayList();
        Iterator archetypeIterator = archetypes.iterator();
        int counter = 1;
        while ( archetypeIterator.hasNext() )
        {
            org.apache.maven.archetype.catalog.Archetype archetype = (org.apache.maven.archetype.catalog.Archetype) archetypeIterator.next();

            answerMap.put( "" + counter, archetype );
            query +=
                "" + counter + ": " + archetype.getArtifactId() + " (" + archetype.getDescription() + ":"
                    + archetype.getArtifactId() + ")\n";
            answers.add( "" + counter );

            counter++;
        }
        query += "Choose a number: ";

        String answer = prompter.prompt( query, answers );

        return (org.apache.maven.archetype.catalog.Archetype) answerMap.get( answer );
    }

    public Archetype selectArchetype( Map catalogs )
        throws PrompterException
    {
        return selectArchetype( catalogs, null );
    }

    public Archetype selectArchetype( Map catalogs, ArchetypeDefinition defaultDefinition )
        throws PrompterException
    {
        String query = "Choose archetype:\n";
        Map archetypeAnswerMap = new HashMap();
        Map reversedArchetypeAnswerMap = new HashMap();
        List answers = new ArrayList();
        List archetypeVersions;
        Iterator catalogIterator = catalogs.keySet().iterator();
        int counter = 1;
        int defaultSelection = 0;
        while ( catalogIterator.hasNext() )
        {
            String catalog = (String) catalogIterator.next();

            Iterator archetypeIterator = ((List) catalogs.get( catalog )).iterator();
            while ( archetypeIterator.hasNext() )
            {
                org.apache.maven.archetype.catalog.Archetype archetype = (org.apache.maven.archetype.catalog.Archetype) archetypeIterator.next();
                String mapKey = ""+counter;
                String archetypeKey = archetype.getGroupId()+":"+archetype.getArtifactId();
                if( reversedArchetypeAnswerMap.containsKey( archetypeKey ) )
                {
                    mapKey = (String) reversedArchetypeAnswerMap.get( archetypeKey );
                    archetypeVersions = (List) archetypeAnswerMap.get( mapKey );
                }
                else
                {
                    archetypeVersions = new ArrayList();
                    archetypeAnswerMap.put( mapKey, archetypeVersions );
                    reversedArchetypeAnswerMap.put(archetypeKey, mapKey);
                    query +=
                        mapKey + ": " + catalog +
                        " -> " + archetype.getArtifactId() + " (" + archetype.getDescription() + ")\n";
                    answers.add( mapKey );

                    // the version is not tested. This is intentional.
                    if ( defaultDefinition != null && archetype.getGroupId().equals( defaultDefinition.getGroupId() ) &&
                        archetype.getArtifactId().equals( defaultDefinition.getArtifactId() ) )
                    {
                        defaultSelection = counter;
                    }

                    counter++;
                }
                archetypeVersions.add( archetype );

            }

        }

        query += "Choose a number: ";

        String answer;
        if ( defaultSelection == 0 )
        {
            answer = prompter.prompt( query, answers );
        }
        else
        {
            answer = prompter.prompt( query, answers, Integer.toString( defaultSelection ) );
        }

        archetypeVersions = (List) archetypeAnswerMap.get( answer );

        if( archetypeVersions.size() == 1 )
        {
            return (org.apache.maven.archetype.catalog.Archetype) archetypeVersions.get( 0 );
        }
        else
        {
            return selectVersion( archetypeVersions );
        }
    }
    private org.apache.maven.archetype.catalog.Archetype selectVersion( List archetypes )
        throws
        PrompterException
    {
        String query = "Choose version: \n";
        Map answerMap = new HashMap();
        List answers = new ArrayList();

        Collections.sort(archetypes, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                org.apache.maven.archetype.catalog.Archetype a1 = (org.apache.maven.archetype.catalog.Archetype) o1;
                org.apache.maven.archetype.catalog.Archetype a2 = (org.apache.maven.archetype.catalog.Archetype) o2;
                return a1.getVersion().compareTo( a2.getVersion() );
            }
        });

        Iterator archetypeVersionsKeys = archetypes.iterator();
        int counter = 1;
        org.apache.maven.archetype.catalog.Archetype archetype;
        while ( archetypeVersionsKeys.hasNext() )
        {
            archetype = (org.apache.maven.archetype.catalog.Archetype) archetypeVersionsKeys.next();
            String archetypeVersion = archetype.getVersion();

            answerMap.put( "" + counter, archetype );
            query += "" + counter + ": " + archetypeVersion + "\n";
            answers.add( "" + counter );

            counter++;
        }
        query += "Choose a number: ";

        String answer = prompter.prompt( query, answers );

        return  (Archetype) answerMap.get( answer );
    }

//
//    public String selectGroup( List groups )
//        throws
//        PrompterException
//    {
//        String query = "Choose group:\n";
//        Map answerMap = new HashMap();
//        List answers = new ArrayList();
//        Iterator groupIterator = groups.iterator();
//        int counter = 1;
//        while ( groupIterator.hasNext() )
//        {
//            String group = (String) groupIterator.next();
//
//            answerMap.put( "" + counter, group );
//            query += "" + counter + ": " + group + "\n";
//            answers.add( "" + counter );
//
//            counter++;
//        }
//        query += "Choose a number: ";
//
//        String answer = prompter.prompt( query, answers );
//
//        return (String) answerMap.get( answer );
//    }
//
//    public Archetype selectArtifact( List archetypes )
//        throws
//        PrompterException
//    {
//        String query = "Choose archetype:\n";
//        Map answerMap = new HashMap();
//        List answers = new ArrayList();
//        Iterator archetypeIterator = archetypes.iterator();
//        int counter = 1;
//        while ( archetypeIterator.hasNext() )
//        {
//            Archetype archetype = (Archetype) archetypeIterator.next();
//
//            answerMap.put( "" + counter, archetype );
//            query +=
//                "" + counter + ": " + archetype.getName() + " (" + archetype.getGroupId() + ":"
//                    + archetype.getArtifactId() + ")\n";
//            answers.add( "" + counter );
//
//            counter++;
//        }
//        query += "Choose a number: ";
//
//        String answer = prompter.prompt( query, answers );
//
//        return (Archetype) answerMap.get( answer );
//    }
//
//    public String selectVersion( List archetypeVersions )
//        throws
//        PrompterException
//    {
//        String query = "Choose version: \n";
//        Map answerMap = new HashMap();
//        List answers = new ArrayList();
//
//        Iterator archetypeVersionsKeys = archetypeVersions.iterator();
//        int counter = 1;
//        while ( archetypeVersionsKeys.hasNext() )
//        {
//            String archetypeVersion = (String) archetypeVersionsKeys.next();
//
//            answerMap.put( "" + counter, archetypeVersion );
//            query += "" + counter + ": " + archetypeVersion + "\n";
//            answers.add( "" + counter );
//
//            counter++;
//        }
//        query += "Choose a number: ";
//
//        String answer = prompter.prompt( query, answers );
//
//        return (String) answerMap.get( answer );
//    }

    public void setPrompter( Prompter prompter )
    {
        this.prompter = prompter;
    }
}
