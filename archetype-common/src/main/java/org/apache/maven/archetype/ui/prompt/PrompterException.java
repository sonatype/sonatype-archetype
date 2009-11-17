
package org.apache.maven.archetype.ui.prompt;

/**
 * Error while prompting.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PrompterException
    extends Exception
{
    ///CLOVER:OFF
    
    private static final long serialVersionUID = 1;
    
    public PrompterException(String message) {
        super(message);
    }

    public PrompterException(String message, Throwable cause) {
        super(message, cause);
    }
}
