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

import org.apache.maven.archetype.common.ArchetypeConfiguration;
import org.apache.maven.archetype.common.ArchetypeDefinition;
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.Iterator;
import java.util.Properties;

@Component(role = ArchetypeFactory.class)
public class DefaultArchetypeFactory implements ArchetypeFactory
{
    @Requirement
    private Logger log;

    public ArchetypeConfiguration createArchetypeConfiguration(ArchetypeDefinition archetypeDefinition, Properties properties) {
        ArchetypeConfiguration configuration = new ArchetypeConfiguration();
        log.debug("Creating ArchetypeConfiguration from ArchetypeDefinition and Properties");

        configuration.setGroupId(archetypeDefinition.getGroupId());
        configuration.setArtifactId(archetypeDefinition.getArtifactId());
        configuration.setVersion(archetypeDefinition.getVersion());

        Iterator propertiesIterator = properties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String property = (String) propertiesIterator.next();
            if (!Constants.ARCHETYPE_GROUP_ID.equals(property) && !Constants.ARCHETYPE_ARTIFACT_ID.equals(property) && !Constants.ARCHETYPE_VERSION.equals(property)) {
                configuration.addRequiredProperty(property);

                log.debug("Adding requiredProperty " + property);

                configuration.setProperty(property, properties.getProperty(property));

                log.debug("Adding property " + property + "=" + properties.getProperty(property));
            }
        }

        return configuration;
    }

    public ArchetypeConfiguration createArchetypeConfiguration(org.apache.maven.archetype.old.descriptor.ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        ArchetypeConfiguration configuration = new ArchetypeConfiguration();
        log.debug("Creating ArchetypeConfiguration from legacy descriptor and Properties");

        configuration.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID, null));
        configuration.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, null));
        configuration.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION, null));

        configuration.setName(archetypeDescriptor.getId());

        configuration.addRequiredProperty(Constants.GROUP_ID);
        log.debug("Adding requiredProperty " + Constants.GROUP_ID);
        if (null != properties.getProperty(Constants.GROUP_ID)) {
            configuration.setProperty(Constants.GROUP_ID, properties.getProperty(Constants.GROUP_ID));
            configuration.setDefaultProperty(Constants.GROUP_ID, configuration.getProperty(Constants.GROUP_ID));
        }
        log.debug("Setting property " + Constants.GROUP_ID + "=" + configuration.getProperty(Constants.GROUP_ID));

        configuration.addRequiredProperty(Constants.ARTIFACT_ID);
        log.debug("Adding requiredProperty " + Constants.ARTIFACT_ID);
        if (null != properties.getProperty(Constants.ARTIFACT_ID)) {
            configuration.setProperty(Constants.ARTIFACT_ID, properties.getProperty(Constants.ARTIFACT_ID));
            configuration.setDefaultProperty(Constants.ARTIFACT_ID, configuration.getProperty(Constants.ARTIFACT_ID));
        }
        log.debug("Setting property " + Constants.ARTIFACT_ID + "=" + configuration.getProperty(Constants.ARTIFACT_ID));

        configuration.addRequiredProperty(Constants.VERSION);
        log.debug("Adding requiredProperty " + Constants.VERSION);
        if (null != properties.getProperty(Constants.VERSION)) {
            configuration.setProperty(Constants.VERSION, properties.getProperty(Constants.VERSION));
            configuration.setDefaultProperty(Constants.VERSION, configuration.getProperty(Constants.VERSION));
        }
        else {
            configuration.setDefaultProperty(Constants.VERSION, "1.0-SNAPSHOT");
        }
        log.debug("Setting property " + Constants.VERSION + "=" + configuration.getProperty(Constants.VERSION));

        configuration.addRequiredProperty(Constants.PACKAGE);
        log.debug("Adding requiredProperty " + Constants.PACKAGE);
        if (null != properties.getProperty(Constants.PACKAGE)) {
            configuration.setProperty(Constants.PACKAGE, properties.getProperty(Constants.PACKAGE));
            configuration.setDefaultProperty(Constants.PACKAGE, configuration.getProperty(Constants.PACKAGE));
        }
        else if (null != configuration.getProperty(Constants.GROUP_ID)) {
            configuration.setProperty(Constants.PACKAGE, configuration.getProperty(Constants.GROUP_ID));
            configuration.setDefaultProperty(Constants.PACKAGE, configuration.getProperty(Constants.PACKAGE));
        }
        log.debug("Setting property " + Constants.PACKAGE + "=" + configuration.getProperty(Constants.PACKAGE));

        return configuration;
    }

    public ArchetypeConfiguration createArchetypeConfiguration(org.apache.maven.archetype.metadata.ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        ArchetypeConfiguration configuration = new ArchetypeConfiguration();
        log.debug("Creating ArchetypeConfiguration from fileset descriptor and Properties");

        configuration.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID, null));
        configuration.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, null));
        configuration.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION, null));

        configuration.setName(archetypeDescriptor.getName());

        Iterator requiredProperties = archetypeDescriptor.getRequiredProperties().iterator();

        while (requiredProperties.hasNext()) {
            org.apache.maven.archetype.metadata.RequiredProperty requiredProperty = (org.apache.maven.archetype.metadata.RequiredProperty) requiredProperties.next();

            configuration.addRequiredProperty(requiredProperty.getKey());
            log.debug("Adding requiredProperty " + requiredProperty.getKey());

            if (null != properties.getProperty(requiredProperty.getKey(), requiredProperty.getDefaultValue()) && !containsInnerProperty(requiredProperty.getDefaultValue())) {
                configuration.setProperty(requiredProperty.getKey(), properties.getProperty(requiredProperty.getKey(), requiredProperty.getDefaultValue()));
                log.debug("Setting property " + requiredProperty.getKey() + "=" + configuration.getProperty(requiredProperty.getKey()));
            }
            if (null != requiredProperty.getDefaultValue()) {
                configuration.setDefaultProperty(requiredProperty.getKey(), requiredProperty.getDefaultValue());
                log.debug("Setting defaultProperty " + requiredProperty.getKey() + "=" + configuration.getDefaultValue(requiredProperty.getKey()));
            }
        } // end while

        if (!configuration.isConfigured(Constants.GROUP_ID) && null == configuration.getDefaultValue(Constants.GROUP_ID)) {
            configuration.addRequiredProperty(Constants.GROUP_ID);
            log.debug("Adding requiredProperty " + Constants.GROUP_ID);
            if (null != properties.getProperty(Constants.GROUP_ID, configuration.getDefaultValue(Constants.GROUP_ID)) && !containsInnerProperty(configuration.getDefaultValue(Constants.GROUP_ID))) {
                configuration.setProperty(Constants.GROUP_ID, properties.getProperty(Constants.GROUP_ID, configuration.getDefaultValue(Constants.GROUP_ID)));
                configuration.setDefaultProperty(Constants.GROUP_ID, configuration.getProperty(Constants.GROUP_ID));
            }
            log.debug("Setting property " + Constants.GROUP_ID + "=" + configuration.getProperty(Constants.GROUP_ID));
        }
        if (!configuration.isConfigured(Constants.ARTIFACT_ID) && null == configuration.getDefaultValue(Constants.ARTIFACT_ID)) {
            configuration.addRequiredProperty(Constants.ARTIFACT_ID);
            log.debug("Adding requiredProperty " + Constants.ARTIFACT_ID);
            if (null != properties.getProperty(Constants.ARTIFACT_ID, configuration.getDefaultValue(Constants.ARTIFACT_ID))
                    && !containsInnerProperty(configuration.getDefaultValue(Constants.ARTIFACT_ID))) {
                configuration.setProperty(Constants.ARTIFACT_ID, properties.getProperty(Constants.ARTIFACT_ID));
                configuration.setDefaultProperty(Constants.ARTIFACT_ID, configuration.getProperty(Constants.ARTIFACT_ID));
            }
            log.debug("Setting property " + Constants.ARTIFACT_ID + "=" + configuration.getProperty(Constants.ARTIFACT_ID));
        }
        if (!configuration.isConfigured(Constants.VERSION) && null == configuration.getDefaultValue(Constants.VERSION)) {
            configuration.addRequiredProperty(Constants.VERSION);
            log.debug("Adding requiredProperty " + Constants.VERSION);
            if (null != properties.getProperty(Constants.VERSION, configuration.getDefaultValue(Constants.VERSION)) && !containsInnerProperty(configuration.getDefaultValue(Constants.VERSION))) {
                configuration.setProperty(Constants.VERSION, properties.getProperty(Constants.VERSION, configuration.getDefaultValue(Constants.VERSION)));
                configuration.setDefaultProperty(Constants.VERSION, configuration.getProperty(Constants.VERSION));
            }
            else {
                configuration.setDefaultProperty(Constants.VERSION, "1.0-SNAPSHOT");
            }
            log.debug("Setting property " + Constants.VERSION + "=" + configuration.getProperty(Constants.VERSION));
        }
        if (!configuration.isConfigured(Constants.PACKAGE) && null == configuration.getDefaultValue(Constants.PACKAGE)) {
            configuration.addRequiredProperty(Constants.PACKAGE);
            log.debug("Adding requiredProperty " + Constants.PACKAGE);
            if (null != properties.getProperty(Constants.PACKAGE, configuration.getDefaultValue(Constants.PACKAGE)) && !containsInnerProperty(configuration.getDefaultValue(Constants.PACKAGE))) {
                configuration.setProperty(Constants.PACKAGE, properties.getProperty(Constants.PACKAGE, configuration.getDefaultValue(Constants.PACKAGE)));
                configuration.setDefaultProperty(Constants.PACKAGE, configuration.getProperty(Constants.PACKAGE));
            }
            else if (null != configuration.getProperty(Constants.GROUP_ID) && !containsInnerProperty(configuration.getDefaultValue(Constants.PACKAGE))) {
                configuration.setProperty(Constants.PACKAGE, configuration.getProperty(Constants.GROUP_ID));
                configuration.setDefaultProperty(Constants.PACKAGE, configuration.getProperty(Constants.PACKAGE));
            }
            log.debug("Setting property " + Constants.PACKAGE + "=" + configuration.getProperty(Constants.PACKAGE));
        }

        if (null != properties.getProperty(Constants.ARCHETYPE_POST_GENERATION_GOALS)) {
            configuration.setProperty(Constants.ARCHETYPE_POST_GENERATION_GOALS, properties.getProperty(Constants.ARCHETYPE_POST_GENERATION_GOALS));
        }

        return configuration;
    }

    public ArchetypeConfiguration createArchetypeConfiguration(MavenProject project, ArchetypeDefinition archetypeDefinition, Properties properties) {
        ArchetypeConfiguration configuration = new ArchetypeConfiguration();
        log.debug("Creating ArchetypeConfiguration from ArchetypeDefinition, MavenProject and Properties");

        configuration.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID));
        configuration.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID));
        configuration.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION));

        Iterator requiredProperties = properties.keySet().iterator();

        while (requiredProperties.hasNext()) {
            String requiredProperty = (String) requiredProperties.next();

            if (requiredProperty.indexOf(".") < 0) {
                configuration.addRequiredProperty(requiredProperty);
                log.debug("Adding requiredProperty " + requiredProperty);
                configuration.setProperty(requiredProperty, properties.getProperty(requiredProperty));
                log.debug("Setting property " + requiredProperty + "=" + configuration.getProperty(requiredProperty));
            }
        }

        configuration.addRequiredProperty(Constants.GROUP_ID);
        log.debug("Adding requiredProperty " + Constants.GROUP_ID);
        configuration.setDefaultProperty(Constants.GROUP_ID, project.getGroupId());
        if (null != properties.getProperty(Constants.GROUP_ID, null)) {
            configuration.setProperty(Constants.GROUP_ID, properties.getProperty(Constants.GROUP_ID));
            log.debug("Setting property " + Constants.GROUP_ID + "=" + configuration.getProperty(Constants.GROUP_ID));
        }

        configuration.addRequiredProperty(Constants.ARTIFACT_ID);
        log.debug("Adding requiredProperty " + Constants.ARTIFACT_ID);
        configuration.setDefaultProperty(Constants.ARTIFACT_ID, project.getArtifactId());
        if (null != properties.getProperty(Constants.ARTIFACT_ID, null)) {
            configuration.setProperty(Constants.ARTIFACT_ID, properties.getProperty(Constants.ARTIFACT_ID));
            log.debug("Setting property " + Constants.ARTIFACT_ID + "=" + configuration.getProperty(Constants.ARTIFACT_ID));
        }

        configuration.addRequiredProperty(Constants.VERSION);
        log.debug("Adding requiredProperty " + Constants.VERSION);
        configuration.setDefaultProperty(Constants.VERSION, project.getVersion());
        if (null != properties.getProperty(Constants.VERSION, null)) {
            configuration.setProperty(Constants.VERSION, properties.getProperty(Constants.VERSION));
            log.debug("Setting property " + Constants.VERSION + "=" + configuration.getProperty(Constants.VERSION));
        }

        configuration.addRequiredProperty(Constants.PACKAGE);
        log.debug("Adding requiredProperty " + Constants.PACKAGE);
        if (null != properties.getProperty(Constants.PACKAGE)) {
            configuration.setProperty(Constants.PACKAGE, properties.getProperty(Constants.PACKAGE));

            log.debug("Setting property " + Constants.PACKAGE + "=" + configuration.getProperty(Constants.PACKAGE));
        }

        if (null != properties.getProperty(Constants.ARCHETYPE_GROUP_ID, null)) {
            configuration.setProperty(Constants.ARCHETYPE_GROUP_ID, properties.getProperty(Constants.ARCHETYPE_GROUP_ID));
        }

        if (null != properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, null)) {
            configuration.setProperty(Constants.ARCHETYPE_ARTIFACT_ID, properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID));
        }

        if (null != properties.getProperty(Constants.ARCHETYPE_VERSION, null)) {
            configuration.setProperty(Constants.ARCHETYPE_VERSION, properties.getProperty(Constants.ARCHETYPE_VERSION));
        }
        return configuration;
    }

    public ArchetypeDefinition createArchetypeDefinition(Properties properties) {
        ArchetypeDefinition definition = new ArchetypeDefinition();

        definition.setGroupId(properties.getProperty(Constants.ARCHETYPE_GROUP_ID, null));

        definition.setArtifactId(properties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, null));

        definition.setVersion(properties.getProperty(Constants.ARCHETYPE_VERSION, null));

        definition.setRepository(properties.getProperty(Constants.ARCHETYPE_REPOSITORY, null));

        return definition;
    }

    public void updateArchetypeConfiguration(ArchetypeConfiguration archetypeConfiguration, ArchetypeDefinition archetypeDefinition) {
        archetypeConfiguration.setGroupId(archetypeDefinition.getGroupId());
        archetypeConfiguration.setArtifactId(archetypeDefinition.getArtifactId());
        archetypeConfiguration.setVersion(archetypeDefinition.getVersion());
    }

    private boolean containsInnerProperty(String defaultValue) {
        if (null == defaultValue) {
            return false;
        }
        return (defaultValue.indexOf("${") >= 0) && (defaultValue.indexOf("}") >= 0);
    }
}
