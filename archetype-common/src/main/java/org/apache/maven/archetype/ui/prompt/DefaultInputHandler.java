package org.apache.maven.archetype.ui.prompt;

/*
 * The MIT License
 *
 * Copyright (c) 2005, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Default input handler, that uses the console.
 *
 * @author Brett Porter
 * @version $Id: DefaultInputHandler.java 7276 2008-04-11 17:50:25Z bentmann $
 */
@Component(role=InputHandler.class)
public class DefaultInputHandler
    implements InputHandler, Initializable, Disposable
{
    @Requirement
    private Logger log;

    private BufferedReader consoleReader;

    public String readLine()
        throws IOException
    {
        return consoleReader.readLine();
    }

    public String readPassword()
        throws IOException
    {
        return consoleReader.readLine();
    }

    public List readMultipleLines()
        throws IOException
    {
        List<String> lines = new ArrayList<String>();
        String line = readLine();
        while ( line != null && line.length() > 0 )
        {
            lines.add( line );
            line = readLine();
        }
        return lines;
    }

    public void initialize()
        throws InitializationException
    {
        consoleReader = new BufferedReader( new InputStreamReader( System.in ) );
    }

    public void dispose()
    {
        try
        {
            consoleReader.close();
        }
        catch ( IOException e )
        {
            log.error( "Error closing input stream must be ignored", e );
        }
    }
}
