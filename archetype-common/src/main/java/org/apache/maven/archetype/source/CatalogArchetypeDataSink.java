package org.apache.maven.archetype.source;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

/** @author Jason van Zyl */
public class CatalogArchetypeDataSink
    implements ArchetypeDataSink
{
    private ArchetypeCatalogXpp3Writer catalogWriter = new ArchetypeCatalogXpp3Writer();

    public void putArchetypes( List<Archetype> archetypes,
                               Writer writer )
        throws ArchetypeDataSinkException
    {
        ArchetypeCatalog catalog = new ArchetypeCatalog();

        for (Archetype archetype : archetypes) {
            catalog.addArchetype( archetype );
        }

        try
        {
            catalogWriter.write( writer, catalog );
        }
        catch ( IOException e )
        {
            throw new ArchetypeDataSinkException( "Error writing archetype catalog.", e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public void putArchetypes( ArchetypeDataSource source,
                               Properties properties,
                               Writer writer )
        throws ArchetypeDataSourceException, ArchetypeDataSinkException
    {
        List<Archetype> archetypes = source.getArchetypeCatalog( properties ).getArchetypes();

        putArchetypes( archetypes, writer );
    }
}
