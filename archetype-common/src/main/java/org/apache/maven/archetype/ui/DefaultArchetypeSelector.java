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

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeManager;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeDefinition;
import org.apache.maven.archetype.exception.ArchetypeSelectionFailure;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(role = ArchetypeSelector.class)
public class DefaultArchetypeSelector
    implements ArchetypeSelector
{
    public static final String DEFAULT_ARCHETYPE_GROUPID = "org.apache.maven.archetypes";

    public static final String DEFAULT_ARCHETYPE_VERSION = "1.0";

    public static final String DEFAULT_ARCHETYPE_ARTIFACTID = "maven-archetype-quickstart";

    @Requirement
    private Logger log;

    @Requirement
    private ArchetypeSelectionQueryer archetypeSelectionQueryer;

    @Requirement
    private ArchetypeManager archetypeManager;

    public void setArchetypeSelectionQueryer(final ArchetypeSelectionQueryer queryer) {
        this.archetypeSelectionQueryer = queryer;
    }

    public void selectArchetype(final ArchetypeGenerationRequest request, final boolean interactive, final String catalogs) throws Exception {
        assert request != null;

        // This should be an internal class
        ArchetypeDefinition definition = new ArchetypeDefinition();
        definition.setGroupId(request.getArchetypeGroupId());
        definition.setArtifactId(request.getArchetypeArtifactId());
        definition.setVersion(request.getArchetypeVersion());

        Map<String,List<Archetype>> archetypes = getArchetypesByCatalog(catalogs);

        if (definition.isDefined() && StringUtils.isNotEmpty(request.getArchetypeRepository())) {
            log.info("Archetype defined by properties");
        }
        else {
            if (definition.isDefined() && StringUtils.isEmpty(request.getArchetypeRepository())) {
                Iterator ca = new ArrayIterator(StringUtils.split(catalogs, ","));
                boolean found = false;
                while (!found && ca.hasNext()) {
                    String catalogKey = (String) ca.next();
                    List<Archetype> catalog = archetypes.get(catalogKey);
                    Archetype example = new Archetype();
                    example.setGroupId(request.getArchetypeGroupId());
                    example.setArtifactId(request.getArchetypeArtifactId());
                    if (catalog.contains(example)) {
                        found = true;

                        Archetype foundArchetype = catalog.get(catalog.indexOf(example));
                        definition.setName(foundArchetype.getArtifactId());
                        if (StringUtils.isNotEmpty(foundArchetype.getRepository())) {
                            definition.setRepository(foundArchetype.getRepository());
                        }
                        else if (catalogKey.contains("-")) {
                            int lastIndex = catalogKey.lastIndexOf("/");
                            String catalogBase = catalogKey.substring(0, (lastIndex > 7 ? lastIndex : catalogKey.length()));
                            definition.setRepository(catalogBase);
                        }

                        log.info("Archetype repository missing. Using the one from " + foundArchetype + " found in catalog " + catalogKey);
                    }
                }
                if (!found) {
                    log.warn("No archetype repository found. Falling back to central repository (http://repo1.maven.org/maven2). ");
                    log.warn("Use -DarchetypeRepository=<your repository> if archetype's repository is elsewhere.");
                    definition.setRepository("http://repo1.maven.org/maven2");
                }
            }

            if (!definition.isDefined() && definition.isPartiallyDefined()) {
                Iterator ca = new ArrayIterator(StringUtils.split(catalogs, ","));
                boolean found = false;
                while (!found && ca.hasNext()) {
                    String catalogKey = (String) ca.next();
                    List<Archetype> catalog = archetypes.get(catalogKey);
                    Archetype example = new Archetype();
                    example.setGroupId(request.getArchetypeGroupId());
                    example.setArtifactId(request.getArchetypeArtifactId());
                    if (catalog.contains(example)) {
                        found = true;

                        Archetype foundArchetype = catalog.get(catalog.indexOf(example));
                        definition.setGroupId(foundArchetype.getGroupId());
                        definition.setArtifactId(foundArchetype.getArtifactId());
                        definition.setVersion(foundArchetype.getVersion());
                        definition.setName(foundArchetype.getArtifactId());
                        if (StringUtils.isNotEmpty(foundArchetype.getRepository())) {
                            definition.setRepository(foundArchetype.getRepository());
                        }
                        else if (catalogKey.contains(":")) {
                            int lastIndex = catalogKey.lastIndexOf("/");
                            String catalogBase = catalogKey.substring(0, (lastIndex > 7 ? lastIndex : catalogKey.length()));
                            definition.setRepository(catalogBase);
                        }

                        String goals = StringUtils.join(foundArchetype.getGoals().iterator(), ",");
                        definition.setGoals(goals);

                        log.info("Archetype " + foundArchetype + " found in catalog " + catalogKey);
                    }
                }

                if (!found) {
                    log.warn("Specified archetype not found.");
                    if (interactive) {
                        definition.setVersion(null);
                        definition.setGroupId(null);
                        definition.setArtifactId(null);
                    }
                }
            }
        }

        // set the defaults - only group and version can be auto-defaulted
        if (definition.getGroupId() == null) {
            definition.setGroupId(DEFAULT_ARCHETYPE_GROUPID);
        }
        if (definition.getVersion() == null) {
            definition.setVersion(DEFAULT_ARCHETYPE_VERSION);
        }

        if (!definition.isDefined() && !definition.isPartiallyDefined()) {
            // if artifact ID is set to it's default, we still prompt to confirm
            if (definition.getArtifactId() == null) {
                log.info("No archetype defined. Using " + DEFAULT_ARCHETYPE_ARTIFACTID + " (" + definition.getGroupId() + ":" + DEFAULT_ARCHETYPE_ARTIFACTID + ":" + definition.getVersion() + ")");
                definition.setArtifactId(DEFAULT_ARCHETYPE_ARTIFACTID);
            }

            if (interactive) {
                if (archetypes.size() > 0) {
                    Archetype selectedArchetype = archetypeSelectionQueryer.selectArchetype(archetypes, definition);

                    definition.setGroupId(selectedArchetype.getGroupId());
                    definition.setArtifactId(selectedArchetype.getArtifactId());
                    definition.setVersion(selectedArchetype.getVersion());
                    definition.setName(selectedArchetype.getArtifactId());
                    String catalogKey = getCatalogKey(archetypes, selectedArchetype);

                    if (StringUtils.isNotEmpty(selectedArchetype.getRepository())) {
                        definition.setRepository(selectedArchetype.getRepository());
                    }
                    else if (catalogKey.contains(":")) {
                        int lastIndex = catalogKey.lastIndexOf("/");
                        String catalogBase = catalogKey.substring(0, (lastIndex > 7 ? lastIndex : catalogKey.length()));
                        definition.setRepository(catalogBase);
                    }

                    String goals = StringUtils.join(selectedArchetype.getGoals().iterator(), ",");
                    definition.setGoals(goals);
                }
            }
        }

        // Make sure the groupId and artifactId are valid, the version may just default to
        // the latest release.

        if (!definition.isPartiallyDefined()) {
            throw new ArchetypeSelectionFailure("No valid archetypes could be found to choose.");
        }

        request.setArchetypeGroupId(definition.getGroupId());
        request.setArchetypeArtifactId(definition.getArtifactId());
        request.setArchetypeVersion(definition.getVersion());
        request.setArchetypeGoals(definition.getGoals());
        request.setArchetypeName(definition.getName());

        if (StringUtils.isNotEmpty(definition.getRepository())) {
            request.setArchetypeRepository(definition.getRepository());
        }
    }

    private Map<String,List<Archetype>> getArchetypesByCatalog(final String catalogs) {
        assert catalogs != null;
        
        Map<String,List<Archetype>> archetypes = new HashMap<String,List<Archetype>>();

        for (String catalog : catalogs.split(",")) {
            if ("internal".equalsIgnoreCase(catalog)) {
                archetypes.put("internal", archetypeManager.getInternalCatalog().getArchetypes());
            }
            else if ("local".equalsIgnoreCase(catalog)) {
                archetypes.put("local", archetypeManager.getDefaultLocalCatalog().getArchetypes());
            }
            else if ("remote".equalsIgnoreCase(catalog)) {
                List<Archetype> archetypesFromRemote = archetypeManager.getRemoteCatalog().getArchetypes();
                if (archetypesFromRemote.size() > 0) {
                    archetypes.put("remote", archetypesFromRemote);
                }
                else {
                    log.warn("No archetype found in Remote catalog. Defaulting to internal Catalog");
                    archetypes.put("internal", archetypeManager.getInternalCatalog().getArchetypes());
                }
            }
            else if (catalog.startsWith("file://")) {
                String path = catalog.substring(7);
                archetypes.put(catalog, archetypeManager.getLocalCatalog(path).getArchetypes());
            }
            else if (catalog.startsWith("http://")) {
                archetypes.put(catalog, archetypeManager.getRemoteCatalog(catalog).getArchetypes());
            }
        }

        if (archetypes.size() == 0) {
            log.warn("No catalog defined. Using internal catalog");
            archetypes.put("internal", archetypeManager.getInternalCatalog().getArchetypes());
        }
        return archetypes;
    }

    private String getCatalogKey(final Map<String,List<Archetype>> archetypes, final Archetype selectedArchetype) {
        for (String key : archetypes.keySet()) {
            List<Archetype> catalog = archetypes.get(key);
            if (catalog.contains(selectedArchetype)) {
                return key;
            }
        }

        return "";
    }
}
