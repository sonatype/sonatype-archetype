
package org.apache.maven.archetype.downloader;

/**
 * @author Jason van Zyl
 */
public class DownloadNotFoundException
    extends Exception
{
    ///CLOVER:OFF

    private static final long serialVersionUID=1;

    public DownloadNotFoundException(String string) {
        super(string);
    }

    public DownloadNotFoundException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public DownloadNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
