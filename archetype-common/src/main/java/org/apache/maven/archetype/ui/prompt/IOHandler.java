package org.apache.maven.archetype.ui.prompt;

import java.io.IOException;

/**
 * ???
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 1.0
 */
public interface IOHandler
{
    String readln() throws IOException;

    void write(String line) throws IOException;

    void writeln(String line) throws IOException;
}