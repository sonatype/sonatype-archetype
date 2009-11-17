
package org.apache.maven.archetype.downloader;

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface Downloader
{
    File download(String groupId, String artifactId, String version, ArtifactRepository archetypeRepository, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws DownloadException, DownloadNotFoundException;

    File downloadOld(String groupId, String artifactId, String version, ArtifactRepository archetypeRepository, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws DownloadException, DownloadNotFoundException;
}
