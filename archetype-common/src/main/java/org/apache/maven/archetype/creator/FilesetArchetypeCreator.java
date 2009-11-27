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

package org.apache.maven.archetype.creator;

import org.apache.maven.archetype.ArchetypeCreationRequest;
import org.apache.maven.archetype.ArchetypeCreationResult;
import org.apache.maven.archetype.common.ArchetypeFilesResolver;
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.archetype.common.PomManager;
import org.apache.maven.archetype.common.util.FileCharsetDetector;
import org.apache.maven.archetype.common.util.ListScanner;
import org.apache.maven.archetype.common.util.PathUtils;
import org.apache.maven.archetype.creator.olddescriptor.OldArchetypeDescriptor;
import org.apache.maven.archetype.creator.olddescriptor.OldArchetypeDescriptorXpp3Writer;
import org.apache.maven.archetype.exception.TemplateCreationException;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.FileSet;
import org.apache.maven.archetype.metadata.ModuleDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.maven.archetype.metadata.io.xpp3.ArchetypeDescriptorXpp3Writer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

//import org.apache.maven.shared.invoker.DefaultInvocationRequest;
//import org.apache.maven.shared.invoker.DefaultInvoker;
//import org.apache.maven.shared.invoker.InvocationRequest;
//import org.apache.maven.shared.invoker.Invoker;

@Component(role = ArchetypeCreator.class, hint = "fileset")
public class FilesetArchetypeCreator
    implements ArchetypeCreator
{
    @Requirement
    private Logger log;

    @Requirement
    private ArchetypeFilesResolver archetypeFilesResolver;

    @Requirement
    private PomManager pomManager;

    @Requirement
    private MavenProjectBuilder projectBuilder;

    public void createArchetype(ArchetypeCreationRequest request, ArchetypeCreationResult result) {
        MavenProject project = request.getProject();
        List<String> languages = request.getLanguages();
        List<String> filtereds = request.getFilteredExtensions();
        String defaultEncoding = request.getDefaultEncoding();
        boolean preserveCData = request.isPreserveCData();
        boolean keepParent = request.isKeepParent();
        boolean partialArchetype = request.isPartialArchetype();
        ArtifactRepository localRepository = request.getLocalRepository();

        Properties properties = new Properties();
        Properties configurationProperties = new Properties();
        if (request.getProperties() != null) {
            properties.putAll(request.getProperties());
            configurationProperties.putAll(request.getProperties());
        }

        if (!properties.containsKey(Constants.GROUP_ID)) {
            properties.setProperty(Constants.GROUP_ID, project.getGroupId());
        }
        configurationProperties.setProperty(Constants.GROUP_ID, properties.getProperty(Constants.GROUP_ID));

        if (!properties.containsKey(Constants.ARTIFACT_ID)) {
            properties.setProperty(Constants.ARTIFACT_ID, project.getArtifactId());
        }
        configurationProperties.setProperty(Constants.ARTIFACT_ID, properties.getProperty(Constants.ARTIFACT_ID));

        if (!properties.containsKey(Constants.VERSION)) {
            properties.setProperty(Constants.VERSION, project.getVersion());
        }
        configurationProperties.setProperty(Constants.VERSION, properties.getProperty(Constants.VERSION));

        if (request.getPackageName() != null) {
            properties.setProperty(Constants.PACKAGE, request.getPackageName());
        }
        else if (!properties.containsKey(Constants.PACKAGE)) {
            properties.setProperty(Constants.PACKAGE, project.getGroupId());
        }
        configurationProperties.setProperty(Constants.PACKAGE, properties.getProperty(Constants.PACKAGE));

        File basedir = project.getBasedir();
        File outputDirectory = request.getOutputDirectory();
        File generatedSourcesDirectory = FileUtils.resolveFile(basedir, outputDirectory.getPath());
        generatedSourcesDirectory.mkdirs();

        log.debug("Creating archetype in " + generatedSourcesDirectory);

        Model model = new Model();
        model.setModelVersion("4.0.0");
        // these values should be retrieve from the requst with sensible defaults
        model.setGroupId(configurationProperties.getProperty(Constants.ARCHETYPE_GROUP_ID, project.getGroupId()));
        model.setArtifactId(configurationProperties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, project.getArtifactId()));
        model.setVersion(configurationProperties.getProperty(Constants.ARCHETYPE_VERSION, project.getVersion()));
        model.setPackaging("maven-archetype");
        model.setName(configurationProperties.getProperty(Constants.ARCHETYPE_ARTIFACT_ID, project.getArtifactId()));

        Build build = new Build();
        model.setBuild(build);

        // In many cases where we are behind a firewall making Archetypes for work mates we want
        // to simply be able to deploy the archetypes once we have created them. In order to do
        // this we want to utilize information from the project we are creating the archetype from.
        // This will be a fully working project that has been testing and inherits from a POM
        // that contains deployment information, along with any extensions required for deployment.
        // We don't want to create archetypes that cannot be deployed after we create them. People
        // might want to edit the archetype POM but they should not have too.

        if (project.getParent() != null) {
            Artifact pa = project.getParentArtifact();

            try {
                MavenProject p = projectBuilder.buildFromRepository(pa, project.getRemoteArtifactRepositories(), localRepository);

                if (p.getDistributionManagement() != null) {
                    model.setDistributionManagement(p.getDistributionManagement());
                }

                if (p.getBuildExtensions() != null) {
                    for (Extension extension : p.getBuildExtensions()) {
                        model.getBuild().addExtension(extension);
                    }
                }
            }
            catch (ProjectBuildingException e) {
                result.setCause(new TemplateCreationException("Error reading parent POM of project: " + pa.getGroupId() + ":" + pa.getArtifactId() + ":" + pa.getVersion()));

                return;
            }
        }

        Extension extension = new Extension();
        extension.setGroupId("org.sonatype.maven.archetype");
        extension.setArtifactId("archetype-packaging");
        extension.setVersion(getArchetypeVersion());
        model.getBuild().addExtension(extension);

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.sonatype.maven.archetype");
        plugin.setArtifactId("archetype-plugin");
        plugin.setVersion(getArchetypeVersion());
        plugin.setExtensions(true);
        model.getBuild().addPlugin(plugin);
        log.debug("Creating archetype's pom");

        File archetypePomFile = FileUtils.resolveFile(basedir, getArchetypePom());

        archetypePomFile.getParentFile().mkdirs();

        try {
            pomManager.writePom(model, archetypePomFile, archetypePomFile);
        }
        catch (IOException e) {
            result.setCause(e);
        }

        File archetypeResourcesDirectory = FileUtils.resolveFile(generatedSourcesDirectory, getTemplateOutputDirectory());
        archetypeResourcesDirectory.mkdirs();

        File archetypeFilesDirectory = FileUtils.resolveFile(archetypeResourcesDirectory, Constants.ARCHETYPE_RESOURCES);
        archetypeFilesDirectory.mkdirs();
        log.debug("Archetype's files output directory " + archetypeFilesDirectory);

        File archetypeDescriptorFile = FileUtils.resolveFile(archetypeResourcesDirectory, Constants.ARCHETYPE_DESCRIPTOR);
        archetypeDescriptorFile.getParentFile().mkdirs();

        ArchetypeDescriptor archetypeDescriptor = new ArchetypeDescriptor();
        archetypeDescriptor.setName(project.getArtifactId());
        log.debug("Starting archetype's descriptor " + project.getArtifactId());
        archetypeDescriptor.setPartial(partialArchetype);

        addRequiredProperties(archetypeDescriptor, properties);

        // TODO ensure reversedproperties contains NO dotted properties
        Properties reverseProperties = getRequiredProperties(archetypeDescriptor, properties);
        // reverseProperties.remove( Constants.GROUP_ID );

        // TODO ensure pomReversedProperties contains NO dotted properties
        Properties pomReversedProperties = getRequiredProperties(archetypeDescriptor, properties);
        // pomReversedProperties.remove( Constants.PACKAGE );

        String packageName = configurationProperties.getProperty(Constants.PACKAGE);

        try {
            Model pom = pomManager.readPom(FileUtils.resolveFile(basedir, Constants.ARCHETYPE_POM));

            List<String> fileNames = resolveFileNames(pom, basedir);

            if (log.isDebugEnabled()) {
                log.debug("Scanned for files " + fileNames.size());
                for (String name : fileNames) {
                    log.debug("- " + name);
                }
            }

            List<FileSet> filesets = resolveFileSets(packageName, fileNames, languages, filtereds, defaultEncoding);
            log.debug("Resolved filesets for " + archetypeDescriptor.getName());

            archetypeDescriptor.setFileSets(filesets);

            createArchetypeFiles(reverseProperties, filesets, packageName, basedir, archetypeFilesDirectory, defaultEncoding);
            log.debug("Created files for " + archetypeDescriptor.getName());

            setParentArtifactId(reverseProperties, configurationProperties.getProperty(Constants.ARTIFACT_ID));

            for (String moduleId : pom.getModules()) {
                String rootArtifactId = configurationProperties.getProperty(Constants.ARTIFACT_ID);
                String moduleIdDirectory = moduleId;
                if (moduleId.indexOf(rootArtifactId) >= 0) {
                    moduleIdDirectory = StringUtils.replace(moduleId, rootArtifactId, "__rootArtifactId__");
                }
                log.debug("Creating module " + moduleId);

                ModuleDescriptor moduleDescriptor = createModule(reverseProperties, rootArtifactId, moduleId, packageName, FileUtils.resolveFile(basedir, moduleId), FileUtils.resolveFile(
                        archetypeFilesDirectory, moduleIdDirectory), languages, filtereds, defaultEncoding, preserveCData, keepParent);

                archetypeDescriptor.addModule(moduleDescriptor);
                log.debug("Added module " + moduleDescriptor.getName() + " in " + archetypeDescriptor.getName());
            }
            restoreParentArtifactId(reverseProperties, null);
            restoreArtifactId(reverseProperties, configurationProperties.getProperty(Constants.ARTIFACT_ID));

            createPoms(pom, configurationProperties.getProperty(Constants.ARTIFACT_ID), configurationProperties.getProperty(Constants.ARTIFACT_ID), archetypeFilesDirectory, basedir,
                    pomReversedProperties, preserveCData, keepParent);
            log.debug("Created Archetype " + archetypeDescriptor.getName() + " pom");

            ArchetypeDescriptorXpp3Writer writer = new ArchetypeDescriptorXpp3Writer();
            writer.write(new FileWriter(archetypeDescriptorFile), archetypeDescriptor);
            log.debug("Archetype " + archetypeDescriptor.getName() + " descriptor written");

            OldArchetypeDescriptor oldDescriptor = convertToOldDescriptor(archetypeDescriptor.getName(), packageName, basedir);
            File oldDescriptorFile = FileUtils.resolveFile(archetypeResourcesDirectory, Constants.OLD_ARCHETYPE_DESCRIPTOR);
            archetypeDescriptorFile.getParentFile().mkdirs();
            writeOldDescriptor(oldDescriptor, oldDescriptorFile);
            log.debug("Archetype " + archetypeDescriptor.getName() + " old descriptor written");

            // InvocationRequest internalRequest = new DefaultInvocationRequest();
            // internalRequest.setPomFile( archetypePomFile );
            // internalRequest.setGoals( Collections.singletonList( request.getPostPhase() ) );
            //
            // Invoker invoker = new DefaultInvoker();
            // invoker.execute(internalRequest);
        }
        catch (Exception e) {
            result.setCause(e);
        }
    }

    private void addRequiredProperties(ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        Properties requiredProperties = new Properties();
        requiredProperties.putAll(properties);
        requiredProperties.remove(Constants.ARCHETYPE_GROUP_ID);
        requiredProperties.remove(Constants.ARCHETYPE_ARTIFACT_ID);
        requiredProperties.remove(Constants.ARCHETYPE_VERSION);
        requiredProperties.remove(Constants.GROUP_ID);
        requiredProperties.remove(Constants.ARTIFACT_ID);
        requiredProperties.remove(Constants.VERSION);
        requiredProperties.remove(Constants.PACKAGE);

        Iterator propertiesIterator = requiredProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String propertyKey = (String) propertiesIterator.next();
            RequiredProperty requiredProperty = new RequiredProperty();
            requiredProperty.setKey(propertyKey);
            requiredProperty.setDefaultValue(requiredProperties.getProperty(propertyKey));
            archetypeDescriptor.addRequiredProperty(requiredProperty);

            log.debug("Adding requiredProperty " + propertyKey + "=" + requiredProperties.getProperty(propertyKey) + " to archetype's descriptor");
        }
    }

    private void createModulePoms(Properties pomReversedProperties, String rootArtifactId, String packageName, File basedir, File archetypeFilesDirectory, boolean preserveCData, boolean keepParent)
            throws IOException, XmlPullParserException
    {
        Model pom = pomManager.readPom(FileUtils.resolveFile(basedir, Constants.ARCHETYPE_POM));

        String parentArtifactId = pomReversedProperties.getProperty(Constants.PARENT_ARTIFACT_ID);
        String artifactId = pom.getArtifactId();
        setParentArtifactId(pomReversedProperties, pomReversedProperties.getProperty(Constants.ARTIFACT_ID));
        setArtifactId(pomReversedProperties, pom.getArtifactId());

        for (String subModuleId : pom.getModules()) {
            String subModuleIdDirectory = subModuleId;
            if (subModuleId.indexOf(rootArtifactId) >= 0) {
                subModuleIdDirectory = StringUtils.replace(subModuleId, rootArtifactId, "__rootArtifactId__");
            }

            createModulePoms(pomReversedProperties, rootArtifactId, packageName, FileUtils.resolveFile(basedir, subModuleId), FileUtils.resolveFile(archetypeFilesDirectory, subModuleIdDirectory),
                    preserveCData, keepParent);
        }
        createModulePom(pom, rootArtifactId, archetypeFilesDirectory, pomReversedProperties, FileUtils.resolveFile(basedir, Constants.ARCHETYPE_POM), preserveCData, keepParent);
        restoreParentArtifactId(pomReversedProperties, parentArtifactId);
        restoreArtifactId(pomReversedProperties, artifactId);
    }

    private void createPoms(Model pom, String rootArtifactId, String artifactId, File archetypeFilesDirectory, File basedir, Properties pomReversedProperties, boolean preserveCData, boolean keepParent)
            throws IOException, XmlPullParserException
    {
        setArtifactId(pomReversedProperties, pom.getArtifactId());

        for (String moduleId : pom.getModules()) {
            String moduleIdDirectory = moduleId;
            if (moduleId.indexOf(rootArtifactId) >= 0) {
                moduleIdDirectory = StringUtils.replace(moduleId, rootArtifactId, "__rootArtifactId__");
            }

            createModulePoms(pomReversedProperties, rootArtifactId, moduleId, FileUtils.resolveFile(basedir, moduleId), FileUtils.resolveFile(archetypeFilesDirectory, moduleIdDirectory),
                    preserveCData, keepParent);
        }
        restoreParentArtifactId(pomReversedProperties, null);
        restoreArtifactId(pomReversedProperties, artifactId);

        createArchetypePom(pom, archetypeFilesDirectory, pomReversedProperties, FileUtils.resolveFile(basedir, Constants.ARCHETYPE_POM), preserveCData, keepParent);
    }

    private String getArchetypePom() {
        return getGeneratedSourcesDirectory() + File.separator + Constants.ARCHETYPE_POM;
    }

    private String getPackageInPathFormat(String aPackage) {
        return StringUtils.replace(aPackage, ".", "/");
    }

    private void rewriteReferences(Model pom, String rootArtifactId, String groupId) {
        // rewrite Dependencies
        if (pom.getDependencies() != null && !pom.getDependencies().isEmpty()) {
            for (Dependency dependency : pom.getDependencies()) {
                if (dependency.getArtifactId() != null && dependency.getArtifactId().indexOf(rootArtifactId) >= 0) {
                    if (dependency.getGroupId() != null) {
                        dependency.setGroupId(StringUtils.replace(dependency.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                    }
                    dependency.setArtifactId(StringUtils.replace(dependency.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                    if (dependency.getVersion() != null) {
                        dependency.setVersion("${" + Constants.VERSION + "}");
                    }
                }
            }
        }

        // rewrite DependencyManagement
        if (pom.getDependencyManagement() != null && pom.getDependencyManagement().getDependencies() != null && !pom.getDependencyManagement().getDependencies().isEmpty()) {
            for (Dependency dependency : pom.getDependencyManagement().getDependencies()) {
                if (dependency.getArtifactId() != null && dependency.getArtifactId().indexOf(rootArtifactId) >= 0) {
                    if (dependency.getGroupId() != null) {
                        dependency.setGroupId(StringUtils.replace(dependency.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                    }
                    dependency.setArtifactId(StringUtils.replace(dependency.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                    if (dependency.getVersion() != null) {
                        dependency.setVersion("${" + Constants.VERSION + "}");
                    }
                }
            }
        }

        // rewrite Plugins
        if (pom.getBuild() != null && pom.getBuild().getPlugins() != null && !pom.getBuild().getPlugins().isEmpty()) {
            for (Plugin plugin : pom.getBuild().getPlugins()) {
                if (plugin.getArtifactId() != null && plugin.getArtifactId().indexOf(rootArtifactId) >= 0) {
                    if (plugin.getGroupId() != null) {
                        plugin.setGroupId(StringUtils.replace(plugin.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                    }
                    plugin.setArtifactId(StringUtils.replace(plugin.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                    if (plugin.getVersion() != null) {
                        plugin.setVersion("${" + Constants.VERSION + "}");
                    }
                }
            }
        }

        // rewrite PluginManagement
        if (pom.getBuild() != null && pom.getBuild().getPluginManagement() != null && pom.getBuild().getPluginManagement().getPlugins() != null
                && !pom.getBuild().getPluginManagement().getPlugins().isEmpty()) {
            for (Plugin plugin : pom.getBuild().getPluginManagement().getPlugins()) {
                if (plugin.getArtifactId() != null && plugin.getArtifactId().indexOf(rootArtifactId) >= 0) {
                    if (plugin.getGroupId() != null) {
                        plugin.setGroupId(StringUtils.replace(plugin.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                    }
                    plugin.setArtifactId(StringUtils.replace(plugin.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                    if (plugin.getVersion() != null) {
                        plugin.setVersion("${" + Constants.VERSION + "}");
                    }
                }
            }
        }
        // rewrite Profiles
        if (pom.getProfiles() != null) {
            for (Profile profile : pom.getProfiles()) {
                // rewrite Dependencies
                if (profile.getDependencies() != null && !profile.getDependencies().isEmpty()) {
                    for (Dependency dependency : profile.getDependencies()) {
                        if (dependency.getArtifactId() != null && dependency.getArtifactId().indexOf(rootArtifactId) >= 0) {
                            if (dependency.getGroupId() != null) {
                                dependency.setGroupId(StringUtils.replace(dependency.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                            }
                            dependency.setArtifactId(StringUtils.replace(dependency.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                            if (dependency.getVersion() != null) {
                                dependency.setVersion("${" + Constants.VERSION + "}");
                            }
                        }
                    }
                }

                // rewrite DependencyManagement
                if (profile.getDependencyManagement() != null && profile.getDependencyManagement().getDependencies() != null && !profile.getDependencyManagement().getDependencies().isEmpty()) {
                    for (Dependency dependency : profile.getDependencyManagement().getDependencies()) {
                        if (dependency.getArtifactId() != null && dependency.getArtifactId().indexOf(rootArtifactId) >= 0) {
                            if (dependency.getGroupId() != null) {
                                dependency.setGroupId(StringUtils.replace(dependency.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                            }
                            dependency.setArtifactId(StringUtils.replace(dependency.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                            if (dependency.getVersion() != null) {
                                dependency.setVersion("${" + Constants.VERSION + "}");
                            }
                        }
                    }
                }

                // rewrite Plugins
                if (profile.getBuild() != null && profile.getBuild().getPlugins() != null && !profile.getBuild().getPlugins().isEmpty()) {
                    for (Plugin plugin : profile.getBuild().getPlugins()) {
                        if (plugin.getArtifactId() != null && plugin.getArtifactId().indexOf(rootArtifactId) >= 0) {
                            if (plugin.getGroupId() != null) {
                                plugin.setGroupId(StringUtils.replace(plugin.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                            }
                            plugin.setArtifactId(StringUtils.replace(plugin.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                            if (plugin.getVersion() != null) {
                                plugin.setVersion("${" + Constants.VERSION + "}");
                            }
                        }
                    }
                }

                // rewrite PluginManagement
                if (profile.getBuild() != null && profile.getBuild().getPluginManagement() != null && profile.getBuild().getPluginManagement().getPlugins() != null
                        && !profile.getBuild().getPluginManagement().getPlugins().isEmpty()) {
                    for (Plugin plugin : profile.getBuild().getPluginManagement().getPlugins()) {
                        if (plugin.getArtifactId() != null && plugin.getArtifactId().indexOf(rootArtifactId) >= 0) {
                            if (plugin.getGroupId() != null) {
                                plugin.setGroupId(StringUtils.replace(plugin.getGroupId(), groupId, "${" + Constants.GROUP_ID + "}"));
                            }
                            plugin.setArtifactId(StringUtils.replace(plugin.getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                            if (plugin.getVersion() != null) {
                                plugin.setVersion("${" + Constants.VERSION + "}");
                            }
                        }
                    }
                }
            }
        }
    }

    private void setArtifactId(Properties properties, String artifactId) {
        properties.setProperty(Constants.ARTIFACT_ID, artifactId);
    }

    private List<String> concatenateToList(List<String> toConcatenate, String with) {
        List<String> result = new ArrayList<String>(toConcatenate.size());

        for (String concatenate : toConcatenate) {
            result.add(((with.length() > 0) ? (with + "/" + concatenate) : concatenate));
        }
        return result;
    }

    private OldArchetypeDescriptor convertToOldDescriptor(String id, String packageName, File basedir) throws IOException {
        log.debug("Resolving OldArchetypeDescriptor files in " + basedir);

        String excludes = "pom.xml,archetype.properties*,**/target/**";

        for (String exclude : ListScanner.DEFAULTEXCLUDES) {
            excludes += "," + exclude + "/**";
        }

        @SuppressWarnings({"unchecked"})
        List<String> fileNames = FileUtils.getFileNames(basedir, "**", excludes, false);

        log.debug("Resolved " + fileNames.size() + " files");

        String packageAsDirectory = StringUtils.replace(packageName, '.', '/') + "/";

        List<String> sources = archetypeFilesResolver.findSourcesMainFiles(fileNames, "java/**");
        fileNames.removeAll(sources);
        sources = removePackage(sources, packageAsDirectory);

        List<String> testSources = archetypeFilesResolver.findSourcesTestFiles(fileNames, "java/**");
        fileNames.removeAll(testSources);
        testSources = removePackage(testSources, packageAsDirectory);

        List<String> resources = archetypeFilesResolver.findResourcesMainFiles(fileNames, "java/**");
        fileNames.removeAll(resources);

        List<String> testResources = archetypeFilesResolver.findResourcesTestFiles(fileNames, "java/**");
        fileNames.removeAll(testResources);

        List<String> siteResources = archetypeFilesResolver.findSiteFiles(fileNames, null);
        fileNames.removeAll(siteResources);

        resources.addAll(fileNames);

        OldArchetypeDescriptor descriptor = new OldArchetypeDescriptor();
        descriptor.setId(id);
        descriptor.setSources(sources);
        descriptor.setTestSources(testSources);
        descriptor.setResources(resources);
        descriptor.setTestResources(testResources);
        descriptor.setSiteResources(siteResources);

        return descriptor;
    }

    private void copyFiles(File basedir, File archetypeFilesDirectory, String directory, List<String> fileSetResources, boolean packaged, String packageName) throws IOException {
        String packageAsDirectory = StringUtils.replace(packageName, ".", File.separator);
        log.debug("Package as Directory: Package:" + packageName + "->" + packageAsDirectory);

        for (String inputFileName : fileSetResources) {
            String outputFileName = packaged ? StringUtils.replace(inputFileName, packageAsDirectory + File.separator, "") : inputFileName;
            log.debug("InputFileName:" + inputFileName);
            log.debug("OutputFileName:" + outputFileName);

            File outputFile = new File(archetypeFilesDirectory, outputFileName);

            File inputFile = new File(basedir, inputFileName);

            outputFile.getParentFile().mkdirs();

            FileUtils.copyFile(inputFile, outputFile);
        }
    }

    private void copyPom(File basedir, File replicaFilesDirectory) throws IOException {
        FileUtils.copyFileToDirectory(new File(basedir, Constants.ARCHETYPE_POM), replicaFilesDirectory);
    }

    private void createArchetypeFiles(Properties reverseProperties, List<FileSet> fileSets, String packageName, File basedir, File archetypeFilesDirectory, String defaultEncoding) throws IOException {
        log.debug("Creating Archetype/Module files from " + basedir + " to " + archetypeFilesDirectory);

        for (FileSet fileSet : fileSets) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(basedir);
            scanner.setIncludes(concatenateToList(fileSet.getIncludes(), fileSet.getDirectory()).toArray(new String[fileSet.getIncludes().size()]));
            scanner.setExcludes(fileSet.getExcludes().toArray(new String[fileSet.getExcludes().size()]));
            scanner.addDefaultExcludes();
            log.debug("Using fileset " + fileSet);
            scanner.scan();

            List<String> fileSetResources = Arrays.asList(scanner.getIncludedFiles());
            log.debug("Scanned " + fileSetResources.size() + " resources");

            if (fileSet.isFiltered()) {
                processFileSet(basedir, archetypeFilesDirectory, fileSet.getDirectory(), fileSetResources, fileSet.isPackaged(), packageName, reverseProperties, defaultEncoding);
                log.debug("Processed " + fileSet.getDirectory() + " files");
            }
            else {
                copyFiles(basedir, archetypeFilesDirectory, fileSet.getDirectory(), fileSetResources, fileSet.isPackaged(), packageName);
                log.debug("Copied " + fileSet.getDirectory() + " files");
            }
        }
    }

    private void createArchetypePom(Model pom, File archetypeFilesDirectory, Properties pomReversedProperties, File initialPomFile, boolean preserveCData, boolean keepParent) throws IOException {
        File outputFile = FileUtils.resolveFile(archetypeFilesDirectory, Constants.ARCHETYPE_POM);

        if (preserveCData) {
            log.debug("Preserving CDATA parts of pom");
            File inputFile = FileUtils.resolveFile(archetypeFilesDirectory, Constants.ARCHETYPE_POM + ".tmp");

            FileUtils.copyFile(initialPomFile, inputFile);

            String initialcontent = FileUtils.fileRead(inputFile);

            String content = getReversedContent(initialcontent, pomReversedProperties);

            outputFile.getParentFile().mkdirs();

            FileUtils.fileWrite(outputFile.getAbsolutePath(), content);

            inputFile.delete();
        }
        else {
            if (!keepParent) {
                pom.setParent(null);
            }

            pom.setModules(null);
            pom.setGroupId("${" + Constants.GROUP_ID + "}");
            pom.setArtifactId("${" + Constants.ARTIFACT_ID + "}");
            pom.setVersion("${" + Constants.VERSION + "}");

            rewriteReferences(pom, pomReversedProperties.getProperty(Constants.ARTIFACT_ID), pomReversedProperties.getProperty(Constants.GROUP_ID));

            pomManager.writePom(pom, outputFile, initialPomFile);
        }

        String initialcontent = FileUtils.fileRead(initialPomFile);
        Iterator properties = pomReversedProperties.keySet().iterator();
        while (properties.hasNext()) {
            String property = (String) properties.next();

            if (initialcontent.indexOf("${" + property + "}") > 0) {
                log.warn("Archetype uses ${" + property + "} for internal processing, but file " + initialPomFile + " contains this property already");
            }
        }
    }

    private FileSet createFileSet(final List<String> excludes, final boolean packaged, final boolean filtered, final String group, final List<String> includes, String defaultEncoding) {
        FileSet fileSet = new FileSet();

        fileSet.setDirectory(group);
        fileSet.setPackaged(packaged);
        fileSet.setFiltered(filtered);
        fileSet.setIncludes(includes);
        fileSet.setExcludes(excludes);
        fileSet.setEncoding(defaultEncoding);

        log.debug("Created Fileset " + fileSet);

        return fileSet;
    }

    private List<FileSet> createFileSets(List<String> files, int level, boolean packaged, String packageName, boolean filtered, String defaultEncoding) {
        List<FileSet> fileSets = new ArrayList<FileSet>();

        if (!files.isEmpty()) {
            log.debug("Creating filesets" + (packaged ? (" packaged (" + packageName + ")") : "") + (filtered ? " filtered" : "") + " at level " + level);
            if (level == 0) {
                List<String> includes = new ArrayList<String>();
                List<String> excludes = new ArrayList<String>();

                for (String file : files) {
                    includes.add(file);
                }

                if (!includes.isEmpty()) {
                    fileSets.add(createFileSet(excludes, packaged, filtered, "", includes, defaultEncoding));
                }
            }
            else {
                Map<String,List<String>> groups = getGroupsMap(files, level);

                for (String group : groups.keySet()) {
                    log.debug("Creating filesets for group " + group);

                    if (!packaged) {
                        fileSets.add(getUnpackagedFileSet(filtered, group, groups.get(group), defaultEncoding));
                    }
                    else {
                        fileSets.addAll(getPackagedFileSets(filtered, group, groups.get(group), packageName, defaultEncoding));
                    }
                }
            }

            log.debug("Resolved fileSets " + fileSets);
        }

        return fileSets;
    }

    private ModuleDescriptor createModule(Properties reverseProperties, String rootArtifactId, String moduleId, String packageName, File basedir, File archetypeFilesDirectory, List<String> languages,
            List<String> filtereds, String defaultEncoding, boolean preserveCData, boolean keepParent) throws IOException, XmlPullParserException
    {
        ModuleDescriptor archetypeDescriptor = new ModuleDescriptor();
        log.debug("Starting module's descriptor " + moduleId);

        archetypeFilesDirectory.mkdirs();
        log.debug("Module's files output directory " + archetypeFilesDirectory);

        Model pom = pomManager.readPom(FileUtils.resolveFile(basedir, Constants.ARCHETYPE_POM));
        String replacementId = pom.getArtifactId();
        String moduleDirectory = pom.getArtifactId();
        if (replacementId.indexOf(rootArtifactId) >= 0) {
            replacementId = StringUtils.replace(replacementId, rootArtifactId, "${rootArtifactId}");
            moduleDirectory = StringUtils.replace(moduleId, rootArtifactId, "__rootArtifactId__");
        }
        if (moduleId.indexOf(rootArtifactId) >= 0) {
            moduleDirectory = StringUtils.replace(moduleId, rootArtifactId, "__rootArtifactId__");
        }
        archetypeDescriptor.setName(replacementId);
        archetypeDescriptor.setId(replacementId);
        archetypeDescriptor.setDir(moduleDirectory);

        setArtifactId(reverseProperties, pom.getArtifactId());

        List<String> fileNames = resolveFileNames(pom, basedir);

        List<FileSet> filesets = resolveFileSets(packageName, fileNames, languages, filtereds, defaultEncoding);
        log.debug("Resolved filesets for module " + archetypeDescriptor.getName());

        archetypeDescriptor.setFileSets(filesets);

        createArchetypeFiles(reverseProperties, filesets, packageName, basedir, archetypeFilesDirectory, defaultEncoding);
        log.debug("Created files for module " + archetypeDescriptor.getName());

        String parentArtifactId = reverseProperties.getProperty(Constants.PARENT_ARTIFACT_ID);
        setParentArtifactId(reverseProperties, pom.getArtifactId());

        for (String subModuleId : pom.getModules()) {
            String subModuleIdDirectory = subModuleId;
            if (subModuleId.indexOf(rootArtifactId) >= 0) {
                subModuleIdDirectory = StringUtils.replace(subModuleId, rootArtifactId, "__rootArtifactId__");
            }

            log.debug("Creating module " + subModuleId);

            ModuleDescriptor moduleDescriptor = createModule(reverseProperties, rootArtifactId, subModuleId, packageName, FileUtils.resolveFile(basedir, subModuleId), FileUtils.resolveFile(
                    archetypeFilesDirectory, subModuleIdDirectory), languages, filtereds, defaultEncoding, preserveCData, keepParent);

            archetypeDescriptor.addModule(moduleDescriptor);
            log.debug("Added module " + moduleDescriptor.getName() + " in " + archetypeDescriptor.getName());
        }
        restoreParentArtifactId(reverseProperties, parentArtifactId);
        restoreArtifactId(reverseProperties, pom.getArtifactId());

        log.debug("Created Module " + archetypeDescriptor.getName() + " pom");

        return archetypeDescriptor;
    }

    private void createModulePom(Model pom, String rootArtifactId, File archetypeFilesDirectory, Properties pomReversedProperties, File initialPomFile, boolean preserveCData, boolean keepParent)
            throws IOException
    {
        File outputFile = FileUtils.resolveFile(archetypeFilesDirectory, Constants.ARCHETYPE_POM);

        if (preserveCData) {
            log.debug("Preserving CDATA parts of pom");
            File inputFile = FileUtils.resolveFile(archetypeFilesDirectory, Constants.ARCHETYPE_POM + ".tmp");

            FileUtils.copyFile(initialPomFile, inputFile);
            String initialcontent = FileUtils.fileRead(inputFile);

            String content = getReversedContent(initialcontent, pomReversedProperties);

            outputFile.getParentFile().mkdirs();

            FileUtils.fileWrite(outputFile.getAbsolutePath(), content);

            inputFile.delete();
        }
        else {
            if (pom.getParent() != null) {
                pom.getParent().setGroupId(StringUtils.replace(pom.getParent().getGroupId(), pomReversedProperties.getProperty(Constants.GROUP_ID), "${" + Constants.GROUP_ID + "}"));
                if (pom.getParent().getArtifactId() != null && pom.getParent().getArtifactId().indexOf(rootArtifactId) >= 0) {
                    pom.getParent().setArtifactId(StringUtils.replace(pom.getParent().getArtifactId(), rootArtifactId, "${rootArtifactId}"));
                }
                if (pom.getParent().getVersion() != null) {
                    pom.getParent().setVersion("${" + Constants.VERSION + "}");
                }
            }
            pom.setModules(null);

            if (pom.getGroupId() != null) {
                pom.setGroupId(StringUtils.replace(pom.getGroupId(), pomReversedProperties.getProperty(Constants.GROUP_ID), "${" + Constants.GROUP_ID + "}"));
            }
            pom.setArtifactId("${" + Constants.ARTIFACT_ID + "}");
            if (pom.getVersion() != null) {
                pom.setVersion("${" + Constants.VERSION + "}");
            }

            rewriteReferences(pom, rootArtifactId, pomReversedProperties.getProperty(Constants.GROUP_ID));

            pomManager.writePom(pom, outputFile, initialPomFile);
        }

        String initialcontent = FileUtils.fileRead(initialPomFile);
        Iterator properties = pomReversedProperties.keySet().iterator();
        while (properties.hasNext()) {
            String property = (String) properties.next();

            if (initialcontent.indexOf("${" + property + "}") > 0) {
                log.warn("OldArchetype uses ${" + property + "} for internal processing, but file " + initialPomFile + " contains this property already");
            }
        }
    }

    private void createReplicaFiles(List<FileSet> filesets, File basedir, File replicaFilesDirectory) throws IOException {
        log.debug("Creating OldArchetype/Module replica files from " + basedir + " to " + replicaFilesDirectory);

        copyPom(basedir, replicaFilesDirectory);

        for (FileSet fileset : filesets) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(basedir);
            scanner.setIncludes((String[]) concatenateToList(fileset.getIncludes(), fileset.getDirectory()).toArray(new String[fileset.getIncludes().size()]));
            scanner.setExcludes((String[]) fileset.getExcludes().toArray(new String[fileset.getExcludes().size()]));
            scanner.addDefaultExcludes();
            log.debug("Using fileset " + fileset);
            scanner.scan();

            List<String> fileSetResources = Arrays.asList(scanner.getIncludedFiles());

            copyFiles(basedir, replicaFilesDirectory, fileset.getDirectory(), fileSetResources, false, null);
            log.debug("Copied " + fileset.getDirectory() + " files");
        }
    }

    private Set<String> getExtensions(List<String> files) {
        Set<String> extensions = new HashSet<String>();

        for (String file : files) {
            extensions.add(FileUtils.extension(file));
        }

        return extensions;
    }

    // FIXME: Expose this as outputDirectory in request
    private String getGeneratedSourcesDirectory() {
        return "target" + File.separator + "generated-sources" + File.separator + "archetype";
    }

    private Map<String,List<String>> getGroupsMap(final List<String> files, final int level) {
        Map<String,List<String>> groups = new HashMap<String,List<String>>();

        for (String file : files) {
            String directory = PathUtils.getDirectory(file, level);
            // make all groups have unix style
            directory = StringUtils.replace(directory, File.separator, "/");

            if (!groups.containsKey(directory)) {
                groups.put(directory, new ArrayList<String>());
            }

            List<String> group = groups.get(directory);

            String innerPath = file.substring(directory.length() + 1);
            // make all groups have unix style
            innerPath = StringUtils.replace(innerPath, File.separator, "/");

            group.add(innerPath);
        }
        log.debug("Sorted " + groups.size() + " groups in " + files.size() + " files");
        log.debug("Sorted Files:" + files);
        return groups;
    }

    private FileSet getPackagedFileSet(final boolean filtered, final Set<String> packagedExtensions, final String group, final Set<String> unpackagedExtensions, final List<String> unpackagedFiles, String defaultEncoding) {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();

        for (String extension : packagedExtensions) {
            includes.add("**/*." + extension);

            if (unpackagedExtensions.contains(extension)) {
                excludes.addAll(archetypeFilesResolver.getFilesWithExtension(unpackagedFiles, extension));
            }
        }

        FileSet fileset = createFileSet(excludes, true, filtered, group, includes, defaultEncoding);
        return fileset;
    }

    private List<FileSet> getPackagedFileSets(final boolean filtered, final String group, final List<String> groupFiles, final String packageName, String defaultEncoding) {
        String packageAsDir = StringUtils.replace(packageName, ".", "/");
        List<FileSet> packagedFileSets = new ArrayList<FileSet>();

        List<String> packagedFiles = archetypeFilesResolver.getPackagedFiles(groupFiles, packageAsDir);
        log.debug("Found packaged Files:" + packagedFiles);

        List<String> unpackagedFiles = archetypeFilesResolver.getUnpackagedFiles(groupFiles, packageAsDir);
        log.debug("Found unpackaged Files:" + unpackagedFiles);

        Set<String> packagedExtensions = getExtensions(packagedFiles);
        log.debug("Found packaged extensions " + packagedExtensions);

        Set<String> unpackagedExtensions = getExtensions(unpackagedFiles);

        if (!packagedExtensions.isEmpty()) {
            packagedFileSets.add(getPackagedFileSet(filtered, packagedExtensions, group, unpackagedExtensions, unpackagedFiles, defaultEncoding));
        }

        if (!unpackagedExtensions.isEmpty()) {
            log.debug("Found unpackaged extensions " + unpackagedExtensions);
            packagedFileSets.add(getUnpackagedFileSet(filtered, unpackagedExtensions, unpackagedFiles, group, packagedExtensions, defaultEncoding));
        }
        return packagedFileSets;
    }

    private void setParentArtifactId(Properties properties, String parentArtifactId) {
        properties.setProperty(Constants.PARENT_ARTIFACT_ID, parentArtifactId);
    }

    private void processFileSet(File basedir, File archetypeFilesDirectory, String directory, List<String> fileSetResources, boolean packaged, String packageName, Properties reverseProperties,
            String defaultEncoding) throws IOException {
        String packageAsDirectory = StringUtils.replace(packageName, ".", File.separator);
        log.debug("Package as Directory: Package:" + packageName + "->" + packageAsDirectory);

        for (String inputFileName : fileSetResources) {
            String outputFileName = packaged ? StringUtils.replace(inputFileName, packageAsDirectory + File.separator, "") : inputFileName;
            log.debug("InputFileName:" + inputFileName);
            log.debug("OutputFileName:" + outputFileName);

            File outputFile = new File(archetypeFilesDirectory, outputFileName);
            File inputFile = new File(basedir, inputFileName);

            FileCharsetDetector detector = new FileCharsetDetector(inputFile);

            String fileEncoding = detector.isFound() ? detector.getCharset() : defaultEncoding;

            String initialcontent = org.apache.commons.io.IOUtils.toString(new FileInputStream(inputFile), fileEncoding);

            Iterator properties = reverseProperties.keySet().iterator();
            while (properties.hasNext()) {
                String property = (String) properties.next();

                if (initialcontent.indexOf("${" + property + "}") > 0) {
                    log.warn("Archetype uses ${" + property + "} for internal processing, but file " + inputFile + " contains this property already");
                }
            }

            String content = getReversedContent(initialcontent, reverseProperties);
            outputFile.getParentFile().mkdirs();
            org.apache.commons.io.IOUtils.write(content, new FileOutputStream(outputFile), fileEncoding);
        } // end while
    }

    private List<String> removePackage(List<String> sources, String packageAsDirectory) {
        if (sources == null) {
            return null;
        }

        List<String> unpackagedSources = new ArrayList<String>(sources.size());

        for (String source : sources) {
            String unpackagedSource = StringUtils.replace(source, packageAsDirectory, "");
            unpackagedSources.add(unpackagedSource);
        }

        return unpackagedSources;
    }

    private Properties getRequiredProperties(ArchetypeDescriptor archetypeDescriptor, Properties properties) {
        Properties reversedProperties = new Properties();

        reversedProperties.putAll(properties);
        reversedProperties.remove(Constants.ARCHETYPE_GROUP_ID);
        reversedProperties.remove(Constants.ARCHETYPE_ARTIFACT_ID);
        reversedProperties.remove(Constants.ARCHETYPE_VERSION);
        reversedProperties.setProperty(Constants.PACKAGE_IN_PATH_FORMAT, getPackageInPathFormat(properties.getProperty(Constants.PACKAGE)));

        return reversedProperties;
    }

    private List<String> resolveFileNames(final Model pom, final File basedir) throws IOException {
        log.debug("Resolving files for " + pom.getId() + " in " + basedir);

        String excludes = "pom.xml*,archetype.properties*,target/**,";
        for (String module : pom.getModules()) {
            excludes += "," + module + "/**";
        }

        for (String exclude : ListScanner.DEFAULTEXCLUDES) {
            excludes += "," + exclude + "/**";
        }

        excludes = PathUtils.convertPathForOS(excludes);

        @SuppressWarnings({"unchecked"})
        List<String> fileNames = FileUtils.getFileNames(basedir, "**,.*,**/.*", excludes, false);

        log.debug("Resolved " + fileNames.size() + " files");
        log.debug("Resolved Files:" + fileNames);

        return fileNames;
    }

    private List<FileSet> resolveFileSets(String packageName, List<String> fileNames, List<String> languages, List<String> filtereds, String defaultEncoding) {
        List<FileSet> resolvedFileSets = new ArrayList<FileSet>();
        log.debug("Resolving filesets with package=" + packageName + ", languages=" + languages + " and extentions=" + filtereds);

        List<String> files = new ArrayList<String>(fileNames);

        String languageIncludes = "";

        for (String language : languages) {
            languageIncludes += ((languageIncludes.length() == 0) ? "" : ",") + language + "/**";
        }

        log.debug("Using languages includes " + languageIncludes);

        String filteredIncludes = "";

        for (String filtered : filtereds) {
            filteredIncludes += ((filteredIncludes.length() == 0) ? "" : ",") + "**/" + (filtered.startsWith(".") ? "" : "*.") + filtered;
        }

        log.debug("Using filtered includes " + filteredIncludes);

        /* sourcesMainFiles */
        List<String> sourcesMainFiles = archetypeFilesResolver.findSourcesMainFiles(files, languageIncludes);
        if (!sourcesMainFiles.isEmpty()) {
            files.removeAll(sourcesMainFiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(sourcesMainFiles, filteredIncludes);
            sourcesMainFiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = sourcesMainFiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 3, true, packageName, true, defaultEncoding));
            }

            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, true, packageName, false, defaultEncoding));
            }
        }

        /* resourcesMainFiles */
        List<String> resourcesMainFiles = archetypeFilesResolver.findResourcesMainFiles(files, languageIncludes);
        if (!resourcesMainFiles.isEmpty()) {
            files.removeAll(resourcesMainFiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(resourcesMainFiles, filteredIncludes);
            resourcesMainFiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = resourcesMainFiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 3, false, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, false, packageName, false, defaultEncoding));
            }
        }

        /* sourcesTestFiles */
        List<String> sourcesTestFiles = archetypeFilesResolver.findSourcesTestFiles(files, languageIncludes);
        if (!sourcesTestFiles.isEmpty()) {
            files.removeAll(sourcesTestFiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(sourcesTestFiles, filteredIncludes);
            sourcesTestFiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = sourcesTestFiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 3, true, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, true, packageName, false, defaultEncoding));
            }
        }

        /* ressourcesTestFiles */
        List<String> resourcesTestFiles = archetypeFilesResolver.findResourcesTestFiles(files, languageIncludes);
        if (!resourcesTestFiles.isEmpty()) {
            files.removeAll(resourcesTestFiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(resourcesTestFiles, filteredIncludes);
            resourcesTestFiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = resourcesTestFiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 3, false, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, false, packageName, false, defaultEncoding));
            }
        }

        /* siteFiles */
        List<String> siteFiles = archetypeFilesResolver.findSiteFiles(files, languageIncludes);
        if (!siteFiles.isEmpty()) {
            files.removeAll(siteFiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(siteFiles, filteredIncludes);
            siteFiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = siteFiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 2, false, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 2, false, packageName, false, defaultEncoding));
            }
        }

        /* thirdLevelSourcesfiles */
        List<String> thirdLevelSourcesfiles = archetypeFilesResolver.findOtherSources(3, files, languageIncludes);
        if (!thirdLevelSourcesfiles.isEmpty()) {
            files.removeAll(thirdLevelSourcesfiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(thirdLevelSourcesfiles, filteredIncludes);
            thirdLevelSourcesfiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = thirdLevelSourcesfiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 3, true, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, true, packageName, false, defaultEncoding));
            }

            /* thirdLevelResourcesfiles */
            List<String> thirdLevelResourcesfiles = archetypeFilesResolver.findOtherResources(3, files, thirdLevelSourcesfiles, languageIncludes);
            if (!thirdLevelResourcesfiles.isEmpty()) {
                files.removeAll(thirdLevelResourcesfiles);
                filteredFiles = archetypeFilesResolver.getFilteredFiles(thirdLevelResourcesfiles, filteredIncludes);
                thirdLevelResourcesfiles.removeAll(filteredFiles);
                unfilteredFiles = thirdLevelResourcesfiles;
                if (!filteredFiles.isEmpty()) {
                    resolvedFileSets.addAll(createFileSets(filteredFiles, 3, false, packageName, true, defaultEncoding));
                }
                if (!unfilteredFiles.isEmpty()) {
                    resolvedFileSets.addAll(createFileSets(unfilteredFiles, 3, false, packageName, false, defaultEncoding));
                }
            }
        } // end if

        /* secondLevelSourcesfiles */
        List<String> secondLevelSourcesfiles = archetypeFilesResolver.findOtherSources(2, files, languageIncludes);
        if (!secondLevelSourcesfiles.isEmpty()) {
            files.removeAll(secondLevelSourcesfiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(secondLevelSourcesfiles, filteredIncludes);
            secondLevelSourcesfiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = secondLevelSourcesfiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 2, true, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 2, true, packageName, false, defaultEncoding));
            }
        }

        /* secondLevelResourcesfiles */
        List<String> secondLevelResourcesfiles = archetypeFilesResolver.findOtherResources(2, files, languageIncludes);
        if (!secondLevelResourcesfiles.isEmpty()) {
            files.removeAll(secondLevelResourcesfiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(secondLevelResourcesfiles, filteredIncludes);
            secondLevelResourcesfiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = secondLevelResourcesfiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 2, false, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 2, false, packageName, false, defaultEncoding));
            }
        }

        /* rootResourcesfiles */
        List<String> rootResourcesfiles = archetypeFilesResolver.findOtherResources(0, files, languageIncludes);
        if (!rootResourcesfiles.isEmpty()) {
            files.removeAll(rootResourcesfiles);

            List<String> filteredFiles = archetypeFilesResolver.getFilteredFiles(rootResourcesfiles, filteredIncludes);
            rootResourcesfiles.removeAll(filteredFiles);

            List<String> unfilteredFiles = rootResourcesfiles;
            if (!filteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(filteredFiles, 0, false, packageName, true, defaultEncoding));
            }
            if (!unfilteredFiles.isEmpty()) {
                resolvedFileSets.addAll(createFileSets(unfilteredFiles, 0, false, packageName, false, defaultEncoding));
            }
        }

        /**/
        if (!files.isEmpty()) {
            log.info("Ignored files: " + files);
        }

        return resolvedFileSets;
    }

    private void restoreArtifactId(Properties properties, String artifactId) {
        if (StringUtils.isEmpty(artifactId)) {
            properties.remove(Constants.ARTIFACT_ID);
        }
        else {
            properties.setProperty(Constants.ARTIFACT_ID, artifactId);
        }
    }

    private void restoreParentArtifactId(Properties properties, String parentArtifactId) {
        if (StringUtils.isEmpty(parentArtifactId)) {
            properties.remove(Constants.PARENT_ARTIFACT_ID);
        }
        else {
            properties.setProperty(Constants.PARENT_ARTIFACT_ID, parentArtifactId);
        }
    }

    private String getReversedContent(String content, Properties properties) {
        String result = StringUtils.replace(StringUtils.replace(content, "$", "${symbol_dollar}"), "\\", "${symbol_escape}");
        Iterator propertyIterator = properties.keySet().iterator();
        while (propertyIterator.hasNext()) {
            String propertyKey = (String) propertyIterator.next();
            result = StringUtils.replace(result, properties.getProperty(propertyKey), "${" + propertyKey + "}");
        }

        // TODO: Replace velocity to a better engine...
        return "#set( $symbol_pound = '#' )\n" + "#set( $symbol_dollar = '$' )\n" + "#set( $symbol_escape = '\\' )\n" + StringUtils.replace(result, "#", "${symbol_pound}");
    }

    private String getTemplateOutputDirectory() {
        return Constants.SRC + File.separator + Constants.MAIN + File.separator + Constants.RESOURCES;
    }

    private FileSet getUnpackagedFileSet(final boolean filtered, final String group, final List<String> groupFiles, String defaultEncoding) {
        Set<String> extensions = getExtensions(groupFiles);

        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();

        for (String extension : extensions) {
            includes.add("**/*." + extension);
        }

        return createFileSet(excludes, false, filtered, group, includes, defaultEncoding);
    }

    private FileSet getUnpackagedFileSet(final boolean filtered, final Set<String> unpackagedExtensions, final List<String> unpackagedFiles, final String group, final Set<String> packagedExtensions, String defaultEncoding) {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();

        for (String extension : unpackagedExtensions) {
            if (packagedExtensions.contains(extension)) {
                includes.addAll(archetypeFilesResolver.getFilesWithExtension(unpackagedFiles, extension));
            }
            else {
                includes.add("**/*." + extension);
            }
        }

        return createFileSet(excludes, false, filtered, group, includes, defaultEncoding);
    }

    private void writeOldDescriptor(OldArchetypeDescriptor oldDescriptor, File oldDescriptorFile) throws IOException {
        OldArchetypeDescriptorXpp3Writer writer = new OldArchetypeDescriptorXpp3Writer();
        writer.write(new FileWriter(oldDescriptorFile), oldDescriptor);
    }

    private static final String MAVEN_PROPERTIES = "META-INF/maven/org.sonatype.maven.archetype/archetype-common/pom.properties";

    public String getArchetypeVersion() {
        InputStream is = null;

        // This should actually come from the pom.properties at testing but it's not generated and
        // put into the JAR, it happens
        // as part of the JAR plugin which is crap as it makes testing inconsistent.
        String version = "version";

        try {
            Properties properties = new Properties();

            is = getClass().getClassLoader().getResourceAsStream(MAVEN_PROPERTIES);

            if (is != null) {
                properties.load(is);

                String property = properties.getProperty("version");

                if (property != null) {
                    return property;
                }
            }

            return version;
        }
        catch (IOException e) {
            return version;
        }
        finally {
            IOUtil.close(is);
        }
    }
}
