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

package org.apache.maven.archetype.source;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Jason van Zyl
 */
@Component(role = ArchetypeDataSource.class, hint = "catalog")
public class CatalogArchetypeDataSource
    implements ArchetypeDataSource
{
    public static final String ARCHETYPE_CATALOG_PROPERTY = "file";

    public static final String ARCHETYPE_CATALOG_FILENAME = "archetype-catalog.xml";

    private ArchetypeCatalogXpp3Reader catalogReader = new ArchetypeCatalogXpp3Reader();

    private ArchetypeCatalogXpp3Writer catalogWriter = new ArchetypeCatalogXpp3Writer();

    @Requirement
    private Logger log;

    public List getArchetypes(Properties properties) throws ArchetypeDataSourceException {
        String s = properties.getProperty(ARCHETYPE_CATALOG_PROPERTY);

        s = StringUtils.replace(s, "${user.home}", System.getProperty("user.home"));

        log.debug("Using catalog: " + s);

        File catalogFile = new File(s);

        if (catalogFile.exists()) {
            try {
                ArchetypeCatalog catalog = readCatalog(new BufferedReader(new FileReader(catalogFile)));

                return createArchetypeMap(catalog);
            }
            catch (FileNotFoundException e) {
                throw new ArchetypeDataSourceException("The specific archetype catalog does not exist.", e);
            }
        }
        else {
            return new ArrayList();
        }
    }

    public void updateCatalog(Properties properties, Archetype archetype) throws ArchetypeDataSourceException {
        String s = properties.getProperty(ARCHETYPE_CATALOG_PROPERTY);

        s = StringUtils.replace(s, "${user.home}", System.getProperty("user.home"));

        log.debug("Using catalog: " + s);

        File catalogFile = new File(s);

        ArchetypeCatalog catalog;
        if (catalogFile.exists()) {
            try {
                log.debug("Reading the catalog: " + catalogFile);
                catalog = readCatalog(new BufferedReader(new FileReader(catalogFile)));
            }
            catch (FileNotFoundException ex) {
                log.debug("Catalog file don't exist");
                catalog = new ArchetypeCatalog();
            }
        }
        else {
            log.debug("Catalog file don't exist");
            catalog = new ArchetypeCatalog();
        }

        boolean found = false;
        Archetype newArchetype = archetype;

        for (Archetype a : catalog.getArchetypes()) {
            if (a.getGroupId().equals(archetype.getGroupId()) && a.getArtifactId().equals(archetype.getArtifactId())) {
                newArchetype = a;
                found = true;
                break;
            }
        }

        if (!found) {
            catalog.addArchetype(newArchetype);
        }

        newArchetype.setVersion(archetype.getVersion());
        newArchetype.setRepository(archetype.getRepository());
        newArchetype.setDescription(archetype.getDescription());
        newArchetype.setProperties(archetype.getProperties());
        newArchetype.setGoals(archetype.getGoals());

        writeLocalCatalog(catalog, catalogFile);
    }

    protected void writeLocalCatalog(ArchetypeCatalog catalog, File catalogFile) throws ArchetypeDataSourceException {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(catalogFile));
            catalogWriter.write(writer, catalog);
        }
        catch (IOException e) {
            throw new ArchetypeDataSourceException("Error writing archetype catalog.", e);
        }
        finally {
            IOUtil.close(writer);
        }
    }

    protected List<Archetype> createArchetypeMap(ArchetypeCatalog catalog) throws ArchetypeDataSourceException {
        List<Archetype> archetypes = new ArrayList<Archetype>();

        for (Archetype archetype : catalog.getArchetypes()) {
            archetypes.add(archetype);
        }

        return archetypes;
    }

    protected ArchetypeCatalog readCatalog(Reader reader) throws ArchetypeDataSourceException {
        try {
            return catalogReader.read(reader);
        }
        catch (IOException e) {
            throw new ArchetypeDataSourceException("Error reading archetype catalog.", e);
        }
        catch (XmlPullParserException e) {
            throw new ArchetypeDataSourceException("Error parsing archetype catalog.", e);
        }
        finally {
            IOUtil.close(reader);
        }
    }

    public ArchetypeCatalog getArchetypeCatalog(Properties properties) throws ArchetypeDataSourceException {
        String s = properties.getProperty(ARCHETYPE_CATALOG_PROPERTY);

        s = StringUtils.replace(s, "${user.home}", System.getProperty("user.home"));

        File catalogFile = new File(s);
        if (catalogFile.exists() && catalogFile.isDirectory()) {
            catalogFile = new File(catalogFile, ARCHETYPE_CATALOG_FILENAME);
        }
        log.debug("Using catalog: " + catalogFile);

        if (catalogFile.exists()) {
            try {
                return readCatalog(new BufferedReader(new FileReader(catalogFile)));
            }
            catch (FileNotFoundException e) {
                throw new ArchetypeDataSourceException("The specific archetype catalog does not exist.", e);
            }
        }
        else {
            return new ArchetypeCatalog();
        }
    }
}
