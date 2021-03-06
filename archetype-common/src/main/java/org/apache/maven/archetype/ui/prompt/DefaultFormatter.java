package org.apache.maven.archetype.ui.prompt;

import java.util.Iterator;
import java.util.List;

/**
 * ???
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.7
 */
public class DefaultFormatter
    implements Formatter
{
    public String format(String message, List<String> possibleValues, String defaultReply) {
        StringBuilder formatted = new StringBuilder(message.length() * 2);

        formatted.append(message);

        if (possibleValues != null && !possibleValues.isEmpty()) {
            formatted.append(" (");

            for (Iterator it = possibleValues.iterator(); it.hasNext();) {
                String possibleValue = (String) it.next();

                formatted.append(possibleValue);

                if (it.hasNext()) {
                    formatted.append('/');
                }
            }

            formatted.append(')');
        }

        if (defaultReply != null) {
            formatted.append(' ').append(defaultReply);
        }

        return formatted.toString();
    }
}
