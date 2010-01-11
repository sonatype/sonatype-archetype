
package org.apache.maven.archetype.ui.prompt;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Default prompter.
 * 
 * @author Brett Porter
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.7
 */
@Component(role = Prompter.class, instantiationStrategy="per-lookup")
public class DefaultPrompter
    implements Prompter
{
    // TODO: i18n

    @Requirement
    private IOHandler io;

    private Formatter formatter = new DefaultFormatter();

    public IOHandler getIo() {
        return io;
    }

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
                    // FIXME: Really should be in the IOHandler, so we can render ANSI here and there, and also add a beep
                    io.writeln(ansi().a(INTENSITY_BOLD).fg(RED).a("Invalid selection").reset().toString());
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
