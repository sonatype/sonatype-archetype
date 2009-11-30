/*
 * Copyright (C) 2009 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.maven.archetype.mojos;

import org.apache.maven.archetype.ui.prompt.IOHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Mojo {@link IOHandler}
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.7
 */
@Component(role=IOHandler.class)
public class MojoIOHandler
    implements IOHandler, Initializable
{
    private BufferedReader reader;

    private PrintWriter writer;

    public void initialize() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out);
    }

    public String readln() throws IOException {
        assert reader != null;
        return reader.readLine();
    }

    public void write(final String line) throws IOException {
        assert writer != null;
        writer.print(line);
    }

    public void writeln(final String line) throws IOException {
        assert writer != null;
        writer.println(line);
    }
}