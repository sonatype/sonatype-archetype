
package org.apache.maven.archetype.ui.prompt;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Default prompter.
 * 
 * @author Brett Porter
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 1.0
 */
@Component(role = Prompter.class, instantiationStrategy="per-lookup")
public class DefaultPrompter
    implements Prompter
{
    // TODO: i18n

    @Requirement
    private IOHandler io;

    private Formatter formatter = new DefaultFormatter();

    public void setFormatter(Formatter formatter) {
        assert formatter != null;
        this.formatter = formatter;
    }

    public String prompt(String message) throws PrompterException {
        try {
            writePrompt(message);
        }
        catch (IOException e) {
            throw new PrompterException("Failed to present prompt", e);
        }

        try {
            return io.readln();
        }
        catch (IOException e) {
            throw new PrompterException("Failed to read user response", e);
        }
    }

    public String prompt(String message, String defaultReply) throws PrompterException {
        try {
            writePrompt(formatMessage(message, null, defaultReply));
        }
        catch (IOException e) {
            throw new PrompterException("Failed to present prompt", e);
        }

        try {
            String line = io.readln();

            if (StringUtils.isEmpty(line)) {
                line = defaultReply;
            }

            return line;
        }
        catch (IOException e) {
            throw new PrompterException("Failed to read user response", e);
        }
    }

    public String prompt(String message, List<String> possibleValues, String defaultReply) throws PrompterException {
        String formattedMessage = formatMessage(message, possibleValues, defaultReply);

        String line;

        do {
            try {
                writePrompt(formattedMessage);
            }
            catch (IOException e) {
                throw new PrompterException("Failed to present prompt", e);
            }

            try {
                line = io.readln();
            }
            catch (IOException e) {
                throw new PrompterException("Failed to read user response", e);
            }

            if (StringUtils.isEmpty(line)) {
                line = defaultReply;
            }

            if (line != null && !possibleValues.contains(line)) {
                try {
                    io.writeln("Invalid selection.");
                }
                catch (IOException e) {
                    throw new PrompterException("Failed to present feedback", e);
                }
            }
        }
        while (line == null || !possibleValues.contains(line));

        return line;
    }

    public String prompt(String message, List<String> possibleValues) throws PrompterException {
        return prompt(message, possibleValues, null);
    }

    private String formatMessage(String message, List<String> possibleValues, String defaultReply) {
        return formatter.format(message, possibleValues, defaultReply);
    }

    private void writePrompt(String message) throws IOException {
        io.write(message + ": ");
    }
}
