package org.apache.maven.archetype.ui.prompt;

import java.util.List;

/**
 * ???
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.7
 */
public interface Formatter
{
    String format(String message, List<String> possibleValues, String defaultReply);
}