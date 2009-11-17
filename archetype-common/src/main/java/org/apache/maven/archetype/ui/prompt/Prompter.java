
package org.apache.maven.archetype.ui.prompt;

import java.util.List;

/**
 * Prompt the user for input.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 1.0
 */
public interface Prompter
{
    void setFormatter(Formatter formatter);
    
    String prompt(String message) throws PrompterException;

    String prompt(String message, String defaultReply) throws PrompterException;

    String prompt(String message, List<String> possibleValues) throws PrompterException;

    String prompt(String message, List<String> possibleValues, String defaultReply) throws PrompterException;
}
