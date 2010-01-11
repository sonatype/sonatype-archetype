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
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.archetype.ui.prompt.Formatter;
import org.apache.maven.archetype.ui.prompt.Prompter;
import org.apache.maven.archetype.ui.prompt.PrompterException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.ansi;

@Component(role = ArchetypeCreationQueryer.class)
public class DefaultArchetypeCreationQueryer
    implements ArchetypeCreationQueryer, Initializable
{
    @Requirement
    private Prompter prompter;

    public void initialize() throws InitializationException {
        prompter.setFormatter(new Formatter() {
            public String format(String message, List<String> possibleValues, String defaultReply) {
                if (defaultReply != null && defaultReply.trim().length() != 0) {
                    return String.format("%s (%s)", message, ansi().fg(CYAN).a(defaultReply).reset());
                }
                return message;
            }
        });
    }

    public String getArchetypeArtifactId(String defaultValue) throws PrompterException {
        return getValue(Constants.ARCHETYPE_ARTIFACT_ID, defaultValue);
    }

    public String getArchetypeGroupId(String defaultValue) throws PrompterException {
        return getValue(Constants.ARCHETYPE_GROUP_ID, defaultValue);
    }

    public String getArchetypeVersion(String defaultValue) throws PrompterException {
        return getValue(Constants.ARCHETYPE_VERSION, defaultValue);
    }

    public String getArtifactId(String defaultValue) throws PrompterException {
        return getValue(Constants.ARTIFACT_ID, defaultValue);
    }

    public boolean askAddAnotherProperty() throws PrompterException {
        StringWriter buff = new StringWriter();
        PrintWriter out = new PrintWriter(buff);

        out.println(ansi().a(INTENSITY_BOLD).a("Add a new custom property").reset().a("?"));
        out.flush();

        String answer = prompter.prompt(buff.toString(), "Y");

        return "Y".equalsIgnoreCase(answer);
    }

    public String askNewPropertyKey() throws PrompterException {
        StringWriter buff = new StringWriter();
        PrintWriter out = new PrintWriter(buff);

        out.println(ansi().a(INTENSITY_BOLD).a("Define property key").reset());
        out.flush();

        return prompter.prompt(buff.toString());
    }

    public String askReplacementValue(String propertyKey, String defaultValue) throws PrompterException {
        return getValue(propertyKey, defaultValue);
    }

    public boolean confirmConfiguration(ArchetypeConfiguration archetypeConfiguration) throws PrompterException {
        StringWriter buff = new StringWriter();
        PrintWriter out = new PrintWriter(buff);

        out.println(ansi().a(INTENSITY_BOLD).a("Confirm archetype configuration").reset().a("..."));

        out.format("  %s=%s", Constants.ARCHETYPE_GROUP_ID, archetypeConfiguration.getGroupId()).println();
        out.format("  %s=%s", Constants.ARCHETYPE_ARTIFACT_ID, archetypeConfiguration.getArtifactId()).println();
        out.format("  %s=%s", Constants.ARCHETYPE_VERSION, archetypeConfiguration.getVersion()).println();

        for (Map.Entry entry : archetypeConfiguration.getProperties().entrySet()) {
            out.format("  %s=%s", entry.getKey(), entry.getValue()).println();
        }

        out.flush();

        String answer = prompter.prompt(buff.toString(), "Y");

        return "Y".equalsIgnoreCase(answer);
    }

    public String getGroupId(String defaultValue) throws PrompterException {
        return getValue(Constants.GROUP_ID, defaultValue);
    }

    public String getPackage(String defaultValue) throws PrompterException {
        return getValue(Constants.PACKAGE, defaultValue);
    }

    public String getVersion(String defaultValue) throws PrompterException {
        return getValue(Constants.VERSION, defaultValue);
    }

    private String getValue(String requiredProperty, String defaultValue) throws PrompterException {
        StringWriter buff = new StringWriter();
        PrintWriter out = new PrintWriter(buff);

        out.format("%s '%s'",
            ansi().a(INTENSITY_BOLD).a("Define value for property").reset(),
            ansi().fg(GREEN).a(requiredProperty).reset());

        out.flush();

        if ((defaultValue != null) && !defaultValue.equals("null")) {
            return prompter.prompt(buff.toString(), defaultValue);
        }
        else {
            return prompter.prompt(buff.toString());
        }
    }
}
