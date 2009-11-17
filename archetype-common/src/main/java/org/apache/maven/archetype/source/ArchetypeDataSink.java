
package org.apache.maven.archetype.source;

import org.apache.maven.archetype.catalog.Archetype;

import java.io.Writer;
import java.util.List;
import java.util.Properties;

/** @author Jason van Zyl */
public interface ArchetypeDataSink
{
    void putArchetypes(List<Archetype> archetypes, Writer writer) throws ArchetypeDataSinkException;

    void putArchetypes(ArchetypeDataSource source, Properties properties, Writer writer) throws ArchetypeDataSourceException, ArchetypeDataSinkException;
}
