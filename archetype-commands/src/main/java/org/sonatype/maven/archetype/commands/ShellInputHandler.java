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

package org.sonatype.maven.archetype.commands;

import jline.console.ConsoleReader;
import org.apache.maven.archetype.ui.prompt.InputHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.gshell.ShellHolder;
import org.sonatype.gshell.command.IO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shell {@link InputHandler}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.0
 */
@Component(role = InputHandler.class, hint = "shell")
public class ShellInputHandler
    implements InputHandler, Initializable
{
    private static final Logger log = LoggerFactory.getLogger(ShellInputHandler.class);

    private ConsoleReader reader;

    public ShellInputHandler() {
        System.out.println("HERE");
        Thread.dumpStack();
    }

    public void initialize() throws InitializationException {
        try {
            IO io = ShellHolder.get().getIo();
            reader = new ConsoleReader(
                io.streams.in,
                io.out,
                io.getTerminal());

            log.info("Initialized reader: {}", reader);
        }
        catch (IOException e) {
            throw new InitializationException(e.getMessage(), e);
        }
    }

    public String readLine() throws IOException {
        log.info("Reading line");
        assert reader != null;
        return reader.readLine();
    }

    public String readPassword() throws IOException {
        log.info("Reading password");
        assert reader != null;
        return reader.readLine(new Character('*'));
    }

    public List readMultipleLines() throws IOException {
        List<String> lines = new ArrayList<String>();
        String line = readLine();
        while (line != null && line.length() > 0) {
            lines.add(line);
            line = readLine();
        }
        return lines;
    }
}