package org.apache.maven.archetype.old;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id: OldArchetype.java 668260 2008-06-16 18:52:02Z rafale $
 */
@Deprecated
public interface OldArchetype
{
    String ROLE = OldArchetype.class.getName();

    String ARCHETYPE_DESCRIPTOR = "META-INF/maven/archetype.xml";

    String ARCHETYPE_OLD_DESCRIPTOR = "META-INF/archetype.xml";

    String ARCHETYPE_RESOURCES = "archetype-resources";

    // TODO: delete this, it probably should be project.getFile instead
    String ARCHETYPE_POM = "pom.xml";

    void createArchetype( String archetypeGroupId, String archetypeArtifactId, String archetypeVersion,
                          ArtifactRepository archetypeRepository,
                          ArtifactRepository localRepository, List remoteRepositories, Map parameters )
        throws UnknownArchetype, ArchetypeNotFoundException, ArchetypeDescriptorException, ArchetypeTemplateProcessingException;
}
