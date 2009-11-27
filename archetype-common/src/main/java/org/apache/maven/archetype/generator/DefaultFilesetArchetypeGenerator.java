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

package org.apache.maven.archetype.generator;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.common.ArchetypeFilesResolver;
import org.apache.maven.archetype.common.Constants;
import org.apache.maven.archetype.common.PomManager;
import org.apache.maven.archetype.exception.ArchetypeGenerationFailure;
import org.apache.maven.archetype.exception.ArchetypeNotConfigured;
import org.apache.maven.archetype.exception.InvalidPackaging;
import org.apache.maven.archetype.exception.OutputFileExists;
import org.apache.maven.archetype.exception.PomFileExists;
import org.apache.maven.archetype.exception.ProjectDirectoryExists;
import org.apache.maven.archetype.metadata.AbstractArchetypeDescriptor;
import org.apache.maven.archetype.metadata.ArchetypeDescriptor;
import org.apache.maven.archetype.metadata.FileSet;
import org.apache.maven.archetype.metadata.ModuleDescriptor;
import org.apache.maven.archetype.metadata.RequiredProperty;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component(role=FilesetArchetypeGenerator.class)
public class DefaultFilesetArchetypeGenerator
    implements FilesetArchetypeGenerator
{
    @Requirement
    private Logger log;

    @Requirement
    private ArchetypeArtifactManager archetypeArtifactManager;

    @Requirement
    private ArchetypeFilesResolver archetypeFilesResolver;

    @Requirement
    private PomManager pomManager;

    @Requirement
    private VelocityComponent velocity;

    /**
     * Token delimiter.
     */
    private static final String DELIMITER="__";

    /**
     * Pattern used to detect tokens in a string. Tokens are any text surrounded by the delimiter.
     */
    private static final Pattern TOKEN_PATTERN=Pattern.compile(".*" + DELIMITER + ".*" + DELIMITER + ".*");

    public void generateArchetype(ArchetypeGenerationRequest request, File archetypeFile, String basedir) throws Exception {
        ClassLoader old=Thread.currentThread().getContextClassLoader();

        try {
            ArchetypeDescriptor archetypeDescriptor=archetypeArtifactManager.getFileSetArchetypeDescriptor(archetypeFile);

            if (!isArchetypeConfigured(archetypeDescriptor, request)) {
                if (request.isInteractiveMode()) {
                    throw new ArchetypeNotConfigured("No archetype was chosen", null);
                }
                else {
                    StringBuilder buff=new StringBuilder();
                    buff.append("Archetype ");
                    buff.append(request.getArchetypeGroupId());
                    buff.append(":");
                    buff.append(request.getArchetypeArtifactId());
                    buff.append(":");
                    buff.append(request.getArchetypeVersion());
                    buff.append(" is not configured");

                    List<String> missingProperties=new ArrayList<String>();
                    Iterator requiredProperties=archetypeDescriptor.getRequiredProperties().iterator();
                    while (requiredProperties.hasNext()) {
                        RequiredProperty requiredProperty=(RequiredProperty) requiredProperties.next();
                        if (org.codehaus.plexus.util.StringUtils.isEmpty(request.getProperties().getProperty(requiredProperty.getKey()))) {
                            buff.append("\n\tProperty ");
                            buff.append(requiredProperty.getKey());
                            missingProperties.add(requiredProperty.getKey());
                            buff.append(" is missing.");
                        }
                    }

                    throw new ArchetypeNotConfigured(buff.toString(), missingProperties);
                }
            }

            Context context=prepareVelocityContext(request);

            String packageName=request.getPackage();
            String artifactId=request.getArtifactId();
            File outputDirectoryFile=new File(basedir, artifactId);
            File basedirPom=new File(basedir, Constants.ARCHETYPE_POM);
            File pom=new File(outputDirectoryFile, Constants.ARCHETYPE_POM);

            List<String> archetypeResources=archetypeArtifactManager.getFilesetArchetypeResources(archetypeFile);

            ZipFile archetypeZipFile=archetypeArtifactManager.getArchetypeZipFile(archetypeFile);

            ClassLoader archetypeJarLoader=archetypeArtifactManager.getArchetypeJarLoader(archetypeFile);

            Thread.currentThread().setContextClassLoader(archetypeJarLoader);

            if (archetypeDescriptor.isPartial()) {
                log.debug("Processing partial archetype " + archetypeDescriptor.getName());
                if (outputDirectoryFile.exists()) {
                    if (!pom.exists()) {
                        throw new PomFileExists("This is a partial archetype and the pom.xml file doesn't exist.");
                    }
                    else {
                        processPomWithMerge(context, pom, "");
                        processArchetypeTemplatesWithWarning(archetypeDescriptor, archetypeResources, archetypeZipFile, "", context, packageName, outputDirectoryFile);
                    }
                }
                else {
                    if (basedirPom.exists()) {
                        processPomWithMerge(context, basedirPom, "");
                        processArchetypeTemplatesWithWarning(archetypeDescriptor, archetypeResources, archetypeZipFile, "", context, packageName, new File(basedir));
                    }
                    else {
                        processPom(context, pom, "");
                        processArchetypeTemplates(archetypeDescriptor, archetypeResources, archetypeZipFile, "", context, packageName, outputDirectoryFile);
                    }
                }

                if (archetypeDescriptor.getModules().size() > 0) {
                    log.info("Modules ignored in partial mode");
                }
            }
            else {
                log.debug("Processing complete archetype " + archetypeDescriptor.getName());
                if (outputDirectoryFile.exists() && pom.exists()) {
                    throw new ProjectDirectoryExists("A Maven 2 project already exists in the directory " + outputDirectoryFile.getPath());
                }
                else {
                    if (outputDirectoryFile.exists()) {
                        log.warn("The directory " + outputDirectoryFile.getPath() + " already exists.");
                    }
                    context.put("rootArtifactId", artifactId);

                    processFilesetModule(artifactId, artifactId, archetypeResources, pom, archetypeZipFile, "", basedirPom, outputDirectoryFile, packageName, archetypeDescriptor, context);
                }
            }
        }
        catch (Exception ex) {
            throw new ArchetypeGenerationFailure(ex);
        }
        finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public String getPackageAsDirectory(String packageName) {
        return StringUtils.replace(packageName, ".", "/");
    }

    private void copyFile(final File outFile, final String template, final boolean failIfExists, final ZipFile archetypeZipFile) throws OutputFileExists, IOException {
        log.debug("Copying file " + template);

        if (failIfExists && outFile.exists()) {
            throw new OutputFileExists("Don't rewrite file " + outFile.getName());
        }
        else if (outFile.exists()) {
            log.warn("CP Don't override file " + outFile);
        }
        else {
            ZipEntry input=archetypeZipFile.getEntry(Constants.ARCHETYPE_RESOURCES + "/" + template);

            InputStream inputStream=archetypeZipFile.getInputStream(input);

            outFile.getParentFile().mkdirs();

            IOUtil.copy(inputStream, new FileOutputStream(outFile));
        }
    }

    private void copyFiles(String directory, List<String> fileSetResources, boolean packaged, String packageName, File outputDirectoryFile, ZipFile archetypeZipFile, String moduleOffset,
            boolean failIfExists, Context context) throws OutputFileExists, IOException
    {
        for (String template : fileSetResources) {
            File outputFile=getOutputFile(template, directory, outputDirectoryFile, packaged, packageName, moduleOffset, context);

            copyFile(outputFile, template, failIfExists, archetypeZipFile);
        }
    }

    private String getEncoding(String archetypeEncoding) {
        return ((null == archetypeEncoding) || "".equals(archetypeEncoding)) ? "UTF-8" : archetypeEncoding;
    }

    private String getOffsetSeparator(String moduleOffset) {
        return (StringUtils.isEmpty(moduleOffset) ? "/" : ("/" + moduleOffset + "/"));
    }

    private File getOutputFile(String template, String directory, File outputDirectoryFile, boolean packaged, String packageName, String moduleOffset, Context context) {
        String templateName=StringUtils.replaceOnce(template, directory, "");
        String outputFileName=directory + "/" + (packaged ? getPackageAsDirectory(packageName) : "") + "/" + templateName.substring(moduleOffset.length());

        if (TOKEN_PATTERN.matcher(outputFileName).matches()) {
            outputFileName=replaceFilenameTokens(outputFileName, context);
        }

        File outputFile=new File(outputDirectoryFile, outputFileName);

        return outputFile;
    }

    /**
     * Replaces all tokens (text surrounded by the {@link #DELIMITER}) within the given string,
     * using properties contained within the context. If a property does not exist in the context,
     * the token is left unmodified and a warning is logged.
     * 
     * @param filePath the file name and path to be interpolated
     * @param context contains the available properties
     */
    private String replaceFilenameTokens(String filePath, Context context) {
        String interpolatedResult=filePath;
        String propertyToken=null;
        String contextPropertyValue;

        int start=0;
        int end;
        int skipUndefinedPropertyIndex=0;

        int maxAttempts=StringUtils.countMatches(interpolatedResult, DELIMITER) / 2;

        for (int x=0; x < maxAttempts && start != -1; x++) {
            start=interpolatedResult.indexOf(DELIMITER, skipUndefinedPropertyIndex);

            if (start != -1) {
                end=interpolatedResult.indexOf(DELIMITER, start + DELIMITER.length());

                if (end != -1) {
                    propertyToken=interpolatedResult.substring(start + DELIMITER.length(), end);
                }

                contextPropertyValue=(String) context.get(propertyToken);

                if (contextPropertyValue != null && contextPropertyValue.trim().length() > 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Replacing '" + DELIMITER + propertyToken + DELIMITER + "' in file path '" + interpolatedResult + "' with value '" + contextPropertyValue + "'.");
                    }

                    interpolatedResult=StringUtils.replace(interpolatedResult, DELIMITER + propertyToken + DELIMITER, contextPropertyValue);

                }
                else {
                    // Need to skip the undefined property
                    skipUndefinedPropertyIndex=end + DELIMITER.length() + 1;

                    log.warn("Property '" + propertyToken + "' was not specified, so the token in '" + interpolatedResult + "' is not being replaced.");
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Final interpolated file path: '" + interpolatedResult + "'");
        }

        return interpolatedResult;
    }

    private String getPackageInPathFormat(String aPackage) {
        return StringUtils.replace(aPackage, ".", "/");
    }

    private boolean isArchetypeConfigured(ArchetypeDescriptor descriptor, ArchetypeGenerationRequest request) {
        for (RequiredProperty property : descriptor.getRequiredProperties()) {
            if (StringUtils.isEmpty(request.getProperties().getProperty(property.getKey()))) {
                return false;
            }
        }

        return true;
    }

    private void setParentArtifactId(Context context, String artifactId) {
        context.put(Constants.PARENT_ARTIFACT_ID, artifactId);
    }

    private Context prepareVelocityContext(ArchetypeGenerationRequest request) {
        Context context=new VelocityContext();
        context.put(Constants.GROUP_ID, request.getGroupId());
        context.put(Constants.ARTIFACT_ID, request.getArtifactId());
        context.put(Constants.VERSION, request.getVersion());
        context.put(Constants.PACKAGE, request.getPackage());
        context.put(Constants.PACKAGE_IN_PATH_FORMAT, getPackageInPathFormat(request.getPackage()));

        for (Object obj : request.getProperties().keySet()) {
            String key = String.valueOf(obj);
            Object value=request.getProperties().getProperty(key);
            context.put(key, value);
        }

        return context;
    }

    private void processArchetypeTemplates(AbstractArchetypeDescriptor archetypeDescriptor, List<String> archetypeResources, ZipFile archetypeZipFile, String moduleOffset, Context context,
            String packageName, File outputDirectoryFile) throws Exception
    {
        processTemplates(packageName, outputDirectoryFile, context, archetypeDescriptor, archetypeResources, archetypeZipFile, moduleOffset, false);
    }

    private void processArchetypeTemplatesWithWarning(org.apache.maven.archetype.metadata.ArchetypeDescriptor archetypeDescriptor, List<String> archetypeResources, ZipFile archetypeZipFile,
            String moduleOffset, Context context, String packageName, File outputDirectoryFile) throws Exception
    {
        processTemplates(packageName, outputDirectoryFile, context, archetypeDescriptor, archetypeResources, archetypeZipFile, moduleOffset, true);
    }

    private void processFileSet(String directory, List<String> fileSetResources, boolean packaged, String packageName, Context context, File outputDirectoryFile, String moduleOffset,
            String archetypeEncoding, boolean failIfExists) throws Exception
    {
        for (String template : fileSetResources) {
            File outputFile=getOutputFile(template, directory, outputDirectoryFile, packaged, packageName, moduleOffset, context);
            processTemplate(outputFile, context, Constants.ARCHETYPE_RESOURCES + "/" + template, archetypeEncoding, failIfExists);
        }
    }

    private void processFilesetModule(String rootArtifactId, String artifactId, final List<String> archetypeResources, File pom, final ZipFile archetypeZipFile, String moduleOffset, File basedirPom,
            File outputDirectoryFile, final String packageName, final AbstractArchetypeDescriptor archetypeDescriptor, final Context context) throws Exception
    {
        outputDirectoryFile.mkdirs();
        log.debug("Processing module " + artifactId);
        log.debug("Processing module rootArtifactId " + rootArtifactId);
        log.debug("Processing module pom " + pom);
        log.debug("Processing module moduleOffset " + moduleOffset);
        log.debug("Processing module outputDirectoryFile " + outputDirectoryFile);

        processFilesetProject(archetypeDescriptor, StringUtils.replace(artifactId, "${rootArtifactId}", rootArtifactId), archetypeResources, pom, archetypeZipFile, moduleOffset, context, packageName,
                outputDirectoryFile, basedirPom);

        String parentArtifactId=(String) context.get(Constants.PARENT_ARTIFACT_ID);
        List<ModuleDescriptor> subProjects=archetypeDescriptor.getModules();

        if (!subProjects.isEmpty()) {
            log.debug(artifactId + " has modules (" + archetypeDescriptor.getModules() + ")");
            setParentArtifactId(context, StringUtils.replace(artifactId, "${rootArtifactId}", rootArtifactId));
        }

        for (ModuleDescriptor project : subProjects) {
            artifactId=project.getId();

            File moduleOutputDirectoryFile=new File(outputDirectoryFile, StringUtils.replace(project.getDir(), "__rootArtifactId__", rootArtifactId));

            context.put(Constants.ARTIFACT_ID, StringUtils.replace(project.getId(), "${rootArtifactId}", rootArtifactId));

            processFilesetModule(rootArtifactId, StringUtils.replace(project.getDir(), "__rootArtifactId__", rootArtifactId), archetypeResources, new File(moduleOutputDirectoryFile,
                    Constants.ARCHETYPE_POM), archetypeZipFile, (StringUtils.isEmpty(moduleOffset) ? "" : (moduleOffset + "/"))
                    + StringUtils.replace(project.getDir(), "${rootArtifactId}", rootArtifactId), pom, moduleOutputDirectoryFile, packageName, project, context);
        }
        restoreParentArtifactId(context, parentArtifactId);
        log.debug("Processed " + artifactId);
    }

    private void processFilesetProject(final AbstractArchetypeDescriptor archetypeDescriptor, final String moduleId, final List<String> archetypeResources, final File pom, final ZipFile archetypeZipFile,
            String moduleOffset, final Context context, final String packageName, final File outputDirectoryFile, final File basedirPom) throws Exception
    {
        log.debug("Processing fileset project moduleId " + moduleId);
        log.debug("Processing fileset project pom " + pom);
        log.debug("Processing fileset project moduleOffset " + moduleOffset);
        log.debug("Processing fileset project outputDirectoryFile " + outputDirectoryFile);
        log.debug("Processing fileset project basedirPom " + basedirPom);

        if (basedirPom.exists()) {
            processPomWithParent(context, pom, moduleOffset, basedirPom, moduleId);
        }
        else {
            processPom(context, pom, moduleOffset);
        }

        processArchetypeTemplates(archetypeDescriptor, archetypeResources, archetypeZipFile, moduleOffset, context, packageName, outputDirectoryFile);
    }

    private void processPom(Context context, File pom, String moduleOffset) throws Exception {
        log.debug("Processing pom " + pom);
        processTemplate(pom, context, Constants.ARCHETYPE_RESOURCES + getOffsetSeparator(moduleOffset) + Constants.ARCHETYPE_POM, getEncoding(null), true);
    }

    private void processPomWithMerge(Context context, File pom, String moduleOffset) throws Exception {
        log.debug("Processing pom " + pom + " with merge");

        File temporaryPom=getTemporaryFile(pom);

        processTemplate(temporaryPom, context, Constants.ARCHETYPE_RESOURCES + getOffsetSeparator(moduleOffset) + Constants.ARCHETYPE_POM, getEncoding(null), true);

        pomManager.mergePoms(pom, temporaryPom);

        // getTemporaryFile sets deleteOnExit. Lets try to delete and then make sure deleteOnExit is
        // still set. Windows has issues deleting files with certain JDKs.
        try {
            FileUtils.forceDelete(temporaryPom);
        }
        catch (IOException e) {
            temporaryPom.deleteOnExit();
        }
    }

    private void processPomWithParent(Context context, File pom, String moduleOffset, File basedirPom, String moduleId) throws Exception,
            IOException, InvalidPackaging, ArchetypeGenerationFailure
    {
        log.debug("Processing pom " + pom + " with parent " + basedirPom);
        processTemplate(pom, context, Constants.ARCHETYPE_RESOURCES + getOffsetSeparator(moduleOffset) + Constants.ARCHETYPE_POM, getEncoding(null), true);

        log.debug("Adding module " + moduleId);
        pomManager.addModule(basedirPom, moduleId);
        pomManager.addParent(pom, basedirPom);
    }

    private void processTemplate(File outFile, Context context, String templateFileName, String encoding, boolean failIfExists) throws Exception {
        templateFileName=templateFileName.replace(File.separatorChar, '/');

        if (!velocity.getEngine().templateExists(templateFileName) && velocity.getEngine().templateExists(templateFileName.replace('/', File.separatorChar))) {
            templateFileName=templateFileName.replace('/', File.separatorChar);
        }

        log.debug("Processing template " + templateFileName);

        if (failIfExists && outFile.exists()) {
            throw new OutputFileExists("Don't rewrite file " + outFile.getAbsolutePath());
        }
        else if (outFile.exists()) {
            log.warn("PT Don't override file " + outFile);
        }
        else {
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }

            log.debug("Merging into " + outFile);

            Writer writer=null;

            try {
                writer=new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outFile)), encoding);

                velocity.getEngine().mergeTemplate(templateFileName, encoding, context, writer);

                writer.flush();
            }
            catch (Exception e) {
                throw new ArchetypeGenerationFailure("Error merging velocity templates: " + e.getMessage(), e);
            }
            finally {
                IOUtil.close(writer);
            }
        }
    }

    private void processTemplates(String packageName, File outputDirectoryFile, Context context, AbstractArchetypeDescriptor archetypeDescriptor, List<String> archetypeResources, ZipFile archetypeZipFile,
            String moduleOffset, boolean failIfExists) throws Exception
    {
        for (FileSet fileSet : archetypeDescriptor.getFileSets()) {
            List<String> fileSetResources=archetypeFilesResolver.filterFiles(moduleOffset, fileSet, archetypeResources);

            // This creates an empty directory, even if there is no file to process
            // Fix for ARCHETYPE-57
            getOutputFile(moduleOffset, fileSet.getDirectory(), outputDirectoryFile, fileSet.isPackaged(), packageName, moduleOffset, context).mkdirs();

            if (fileSet.isFiltered()) {
                log.debug("Processing fileset " + fileSet + "\n\n\n\n" + fileSetResources + "\n\n" + archetypeResources + "\n\n");
                processFileSet(fileSet.getDirectory(), fileSetResources, fileSet.isPackaged(), packageName, context, outputDirectoryFile, moduleOffset, getEncoding(fileSet.getEncoding()),
                        failIfExists);
                log.debug("Processed " + fileSetResources.size() + " files");
            }
            else {
                log.debug("Copying fileset " + fileSet);
                copyFiles(fileSet.getDirectory(), fileSetResources, fileSet.isPackaged(), packageName, outputDirectoryFile, archetypeZipFile, moduleOffset, failIfExists, context);
                log.debug("Copied " + fileSetResources.size() + " files");
            }
        }
    }

    private void restoreParentArtifactId(Context context, String parentArtifactId) {
        if (StringUtils.isEmpty(parentArtifactId)) {
            context.remove(Constants.PARENT_ARTIFACT_ID);
        }
        else {
            context.put(Constants.PARENT_ARTIFACT_ID, parentArtifactId);
        }
    }

    private File getTemporaryFile(File file) {
        File tmp=FileUtils.createTempFile(file.getName(), Constants.TMP, file.getParentFile());
        tmp.deleteOnExit();
        return tmp;
    }
}
