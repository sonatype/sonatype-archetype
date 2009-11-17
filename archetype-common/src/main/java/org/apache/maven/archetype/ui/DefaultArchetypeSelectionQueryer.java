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

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeDefinition;
import org.apache.maven.archetype.ui.prompt.Prompter;
import org.apache.maven.archetype.ui.prompt.PrompterException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(role = ArchetypeSelectionQueryer.class)
public class DefaultArchetypeSelectionQueryer
    implements ArchetypeSelectionQueryer
{
    @Requirement(hint = "archetype")
    private Prompter prompter;

    public boolean confirmSelection(ArchetypeDefinition archetypeDefinition) throws PrompterException {
        String query = "Confirm archetype selection: \n" + archetypeDefinition.getGroupId() + "/" + archetypeDefinition.getName() + "\n";

        String answer = prompter.prompt(query, Arrays.asList("Y", "N"), "Y");

        return "Y".equalsIgnoreCase(answer);
    }

    public Archetype selectArchetype(List<Archetype> archetypes) throws PrompterException {
        String query = "Choose archetype:\n";
        Map<String,Archetype> answerMap = new HashMap<String,Archetype>();
        List<String> answers = new ArrayList<String>();
        int counter = 1;

        for (Archetype archetype : archetypes) {
            answerMap.put("" + counter, archetype);
            query += "" + counter + ": " + archetype.getArtifactId() + " (" + archetype.getDescription() + ":" + archetype.getArtifactId() + ")\n";
            answers.add("" + counter);
            counter++;
        }
        query += "Choose a number: ";

        String answer = prompter.prompt(query, answers);

        return answerMap.get(answer);
    }

    public Archetype selectArchetype(Map<String,List<Archetype>> catalogs) throws PrompterException {
        return selectArchetype(catalogs, null);
    }

    public Archetype selectArchetype(Map<String,List<Archetype>> catalogs, ArchetypeDefinition defaultDefinition) throws PrompterException {
        String query = "Choose archetype:\n";
        Map<String,List<Archetype>> archetypeAnswerMap = new HashMap<String,List<Archetype>>();
        Map<String,String> reversedArchetypeAnswerMap = new HashMap<String,String>();
        List<String> answers = new ArrayList<String>();
        List<Archetype> archetypeVersions;
        int counter = 1;
        int defaultSelection = 0;

        for (String catalog : catalogs.keySet()) {
            for (Archetype archetype : catalogs.get(catalog)) {

                String mapKey = "" + counter;
                String archetypeKey = archetype.getGroupId() + ":" + archetype.getArtifactId();
                if (reversedArchetypeAnswerMap.containsKey(archetypeKey)) {
                    mapKey = reversedArchetypeAnswerMap.get(archetypeKey);
                    archetypeVersions = archetypeAnswerMap.get(mapKey);
                }
                else {
                    archetypeVersions = new ArrayList<Archetype>();
                    archetypeAnswerMap.put(mapKey, archetypeVersions);
                    reversedArchetypeAnswerMap.put(archetypeKey, mapKey);
                    query += mapKey + ": " + catalog + " -> " + archetype.getArtifactId() + " (" + archetype.getDescription() + ")\n";
                    answers.add(mapKey);

                    // the version is not tested. This is intentional.
                    if (defaultDefinition != null && archetype.getGroupId().equals(defaultDefinition.getGroupId()) && archetype.getArtifactId().equals(defaultDefinition.getArtifactId())) {
                        defaultSelection = counter;
                    }

                    counter++;
                }
                archetypeVersions.add(archetype);
            }
        }

        query += "Choose a number: ";

        String answer;
        if (defaultSelection == 0) {
            answer = prompter.prompt(query, answers);
        }
        else {
            answer = prompter.prompt(query, answers, Integer.toString(defaultSelection));
        }

        archetypeVersions = archetypeAnswerMap.get(answer);

        if (archetypeVersions.size() == 1) {
            return archetypeVersions.get(0);
        }
        else {
            return selectVersion(archetypeVersions);
        }
    }

    private Archetype selectVersion(List<Archetype> archetypes) throws PrompterException {
        String query = "Choose version: \n";
        Map<String,Archetype> answerMap = new HashMap<String,Archetype>();
        List<String> answers = new ArrayList<String>();

        Collections.sort(archetypes, new Comparator<Archetype>()
        {
            public int compare(Archetype o1, Archetype o2) {
                return o1.getVersion().compareTo(o2.getVersion());
            }
        });

        int counter = 1;
        for (Archetype archetype : archetypes) {
            String archetypeVersion = archetype.getVersion();
            answerMap.put("" + counter, archetype);
            query += "" + counter + ": " + archetypeVersion + "\n";
            answers.add("" + counter);

            counter++;
        }
        query += "Choose a number: ";

        String answer = prompter.prompt(query, answers);

        return answerMap.get(answer);
    }

    public void setPrompter(Prompter prompter) {
        this.prompter = prompter;
    }
}
