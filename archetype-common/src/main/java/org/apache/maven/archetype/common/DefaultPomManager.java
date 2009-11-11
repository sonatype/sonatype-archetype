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

package org.apache.maven.archetype.common;

import org.apache.maven.archetype.common.util.FileCharsetDetector;
import org.apache.maven.archetype.common.util.Format;
import org.apache.maven.archetype.exception.InvalidPackaging;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Build;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

@Component(role=PomManager.class)
public class DefaultPomManager
    extends AbstractLogEnabled
    implements PomManager
{
    public void addModule( File pom,
                           String artifactId )
        throws
        IOException,
        XmlPullParserException,
        DocumentException,
        InvalidPackaging
    {
        boolean found = false;

        StringWriter writer = new StringWriter();
        Reader fileReader = new FileReader( pom );

        try
        {
            fileReader = new FileReader( pom );

            SAXReader reader = new SAXReader();
            Document document = reader.read( fileReader );
            Element project = document.getRootElement();

            String packaging = null;
            Element packagingElement = project.element( "packaging" );
            if ( packagingElement != null )
            {
                packaging = packagingElement.getStringValue();
            }
            if ( !"pom".equals( packaging ) )
            {
                throw new InvalidPackaging(
                    "Unable to add module to the current project as it is not of packaging type 'pom'"
                );
            }

            Element modules = project.element( "modules" );
            if ( modules == null )
            {
                modules = project.addText( "  " ).addElement( "modules" );
                modules.setText( "\n  " );
                project.addText( "\n" );
            }
            // TODO: change to while loop
            for ( Iterator i = modules.elementIterator( "module" ); i.hasNext() && !found; )
            {
                Element module = (Element) i.next();
                if ( module.getText().equals( artifactId ) )
                {
                    found = true;
                }
            }
            if ( !found )
            {
                Node lastTextNode = null;
                for ( Iterator i = modules.nodeIterator(); i.hasNext(); )
                {
                    Node node = (Node) i.next();
                    if ( node.getNodeType() == Node.ELEMENT_NODE )
                    {
                        lastTextNode = null;
                    }
                    else if ( node.getNodeType() == Node.TEXT_NODE )
                    {
                        lastTextNode = node;
                    }
                }

                if ( lastTextNode != null )
                {
                    modules.remove( lastTextNode );
                }

                modules.addText( "\n    " );
                modules.addElement( "module" ).setText( artifactId );
                modules.addText( "\n  " );

                XMLWriter xmlWriter = new XMLWriter( writer );
                xmlWriter.write( document );

                FileUtils.fileWrite( pom.getAbsolutePath(), writer.toString() );
            } // end if
        }
        finally
        {
            IOUtil.close( fileReader );
        }
    }

    public void addParent( File pom,
                           File parentPom )
        throws
        IOException,
        XmlPullParserException
    {
        Model generatedModel = readPom( pom );
        if( null != generatedModel.getParent() )
        {
            getLogger().info( "Parent element not overwrited in " + pom );
            return;
        }

        Model parentModel = readPom( parentPom );

        Parent parent = new Parent();
        parent.setGroupId( parentModel.getGroupId() );
        if ( parent.getGroupId() == null )
        {
            parent.setGroupId( parentModel.getParent().getGroupId() );
        }
        parent.setArtifactId( parentModel.getArtifactId() );
        parent.setVersion( parentModel.getVersion() );
        if ( parent.getVersion() == null )
        {
            parent.setVersion( parentModel.getParent().getVersion() );
        }
        generatedModel.setParent( parent );

        writePom( generatedModel, pom, pom );
    }

    public void mergePoms( File pom,
                           File temporaryPom )
        throws
        IOException,
        XmlPullParserException
    {
        Model model = readPom( pom );
        Model generatedModel = readPom( temporaryPom );

        model.getProperties().putAll( generatedModel.getProperties() );

        mergeModelBase( model, generatedModel );
        mergeModelBuild( model, generatedModel );
        mergeProfiles( model, generatedModel );
        mergeReportPlugins( model, generatedModel );

//
//        // Potential merging
//
//        model.getModelEncoding ();
//        model.getModelVersion ();
//
//        model.getGroupId ();
//        model.getArtifactId ();
//        model.getVersion ();
//        model.getParent ();
//
//        model.getId ();
//        model.getName ();
//        model.getInceptionYear ();
//        model.getDescription ();
//        model.getUrl ();
//        model.getLicenses ();
//        model.getProperties ();
//
//        model.getOrganization ();
//        model.getMailingLists ();
//        model.getContributors ();
//        model.getDevelopers ();
//
//        model.getScm ();
//        model.getCiManagement ();
//        model.getDistributionManagement ();
//        model.getIssueManagement ();
//
//        model.getPackaging ();
////        model.getDependencies (); // done
//        model.getDependencyManagement ();
//        model.getPrerequisites ().getMaven ();
//        model.getPrerequisites ().getModelEncoding ();
//
//        model.getProfiles ();
//        model.getModules ();
//        model.getRepositories ();
//        model.getPluginRepositories ();
//
//        model.getBuild ().getDefaultGoal ();
//        model.getBuild ().getFinalName ();
//        model.getBuild ().getModelEncoding ();
//        model.getBuild ().getFilters ();
//        model.getBuild ().getDirectory ();
//        model.getBuild ().getOutputDirectory ();
//        model.getBuild ().getSourceDirectory ();
//        model.getBuild ().getResources ();
//        model.getBuild ().getScriptSourceDirectory ();
//        model.getBuild ().getTestOutputDirectory ();
//        model.getBuild ().getTestResources ();
//        model.getBuild ().getTestSourceDirectory ();
//        model.getBuild ().getPluginManagement ();
//        model.getBuild ().getExtensions ();
////        model.getBuild ().getPluginsAsMap (); // done
//
//        model.getReporting ().getModelEncoding ();
//        model.getReporting ().getOutputDirectory ();
////        model.getReporting ().getReportPluginsAsMap (); // done
//

        writePom( model, pom, pom );
    }

    public Model readPom( final File pomFile )
        throws
        IOException,
        XmlPullParserException
    { // TODO ensure correct encoding by using default one from method argument !!!

        Model model;
        Reader pomReader = null;
        try
        {
            FileCharsetDetector detector = new FileCharsetDetector( pomFile );

            String fileEncoding = detector.isFound() ? detector.getCharset() : "UTF-8";

            pomReader = new InputStreamReader( new FileInputStream( pomFile ), fileEncoding );

            MavenXpp3Reader reader = new MavenXpp3Reader();

            model = reader.read( pomReader );

            if ( StringUtils.isEmpty( model.getModelEncoding() ) )
            {
                model.setModelEncoding( fileEncoding );
            }
        }
        finally
        {
            IOUtil.close( pomReader );
            pomReader = null;
        }
        return model;
    }


    public Model readPom( InputStream pomStream )
        throws
        IOException,
        XmlPullParserException
    { // TODO ensure correct encoding by using default one from method argument !!!

        Model model;
        Reader pomReader = null;
        try
        {
//            FileCharsetDetector detector = new FileCharsetDetector( pomStream );

            String fileEncoding = /*detector.isFound() ? detector.getCharset() :*/ "UTF-8";

            pomReader = new InputStreamReader( pomStream, fileEncoding );

            MavenXpp3Reader reader = new MavenXpp3Reader();

            model = reader.read( pomReader );

            if ( StringUtils.isEmpty( model.getModelEncoding() ) )
            {
                model.setModelEncoding( fileEncoding );
            }
        }
        finally
        {
            IOUtil.close( pomReader );
            pomReader = null;
        }
        return model;
    }

    public void writePom( final Model model,
                          final File pomFile,
                          final File initialPomFile )
        throws
        IOException
    {
        InputStream inputStream = null;
        Writer outputStreamWriter = null;
//        FileCharsetDetector detector = new FileCharsetDetector ( pomFile );

        String fileEncoding =
            StringUtils.isEmpty( model.getModelEncoding() ) ? model.getModelEncoding() : "UTF-8";

        try
        {
            inputStream = new FileInputStream( initialPomFile );

            SAXBuilder builder = new SAXBuilder();
            org.jdom.Document doc = builder.build( inputStream );
            inputStream.close();
            inputStream = null;

            // The cdata parts of the pom are not preserved from initial to target
            MavenJDOMWriter writer = new MavenJDOMWriter();

            outputStreamWriter =
                new OutputStreamWriter( new FileOutputStream( pomFile ), fileEncoding );

            Format form = Format.getRawFormat().setEncoding( fileEncoding );
            writer.write( model, doc, outputStreamWriter, form );
            outputStreamWriter.close();
            outputStreamWriter = null;
        }
        catch ( JDOMException exc )
        {
            throw (IOException) new IOException( "Cannot parse the POM by JDOM." );
        }
        catch ( FileNotFoundException e )
        {
            getLogger().debug( "Creating pom file " + pomFile );

            Writer pomWriter = null;

            try
            {
//                pomWriter = new FileWriter ( pomFile );
                pomWriter =
                    new OutputStreamWriter( new FileOutputStream( pomFile ), fileEncoding );

                MavenXpp3Writer writer = new MavenXpp3Writer();
                writer.write( pomWriter, model );
                pomWriter.close();
                pomWriter = null;
            }
            finally
            {
                IOUtil.close( pomWriter );
            }
        }
        finally
        {
            IOUtil.close( inputStream );
            IOUtil.close( outputStreamWriter );
        }
    }

    private Map createDependencyMap( List dependencies )
    {
        Map dependencyMap = new HashMap();
        Iterator dependenciesIterator = dependencies.iterator();
        while ( dependenciesIterator.hasNext() )
        {
            Dependency dependency = (Dependency) dependenciesIterator.next();

            dependencyMap.put(
                dependency.getManagementKey(),
                dependency
            );
        }

        return dependencyMap;
    }

    private void mergeModelBuild( Model model, Model generatedModel )
    {
        if ( generatedModel.getBuild() != null )
        {
            if ( model.getBuild() == null )
            {
                model.setBuild( new Build() );
            }

            mergeBuildPlugins( model.getBuild(), generatedModel.getBuild() );
        }
    }

    private void mergeProfiles( Model model, Model generatedModel )
    {
        List generatedProfiles = generatedModel.getProfiles();
        if ( generatedProfiles != null && generatedProfiles.size() > 0 )
        {
            List modelProfiles = model.getProfiles();
            Map modelProfileIdMap = new HashMap();
            if ( modelProfiles == null )
            {
                modelProfiles = new ArrayList();
                model.setProfiles( modelProfiles );
            }
            else if ( modelProfiles.size() > 0 )
            {
                // add profile ids from the model for later lookups to the modelProfileIds set
                Iterator modelProfilesIterator = modelProfiles.iterator();
                while ( modelProfilesIterator.hasNext() )
                {
                    Profile modelProfile = (Profile) modelProfilesIterator.next();
                    modelProfileIdMap.put( modelProfile.getId(), modelProfile );
                }
            }

            Iterator generatedProfilesIterator = generatedProfiles.iterator();
            while ( generatedProfilesIterator.hasNext() )
            {
                Profile generatedProfile = (Profile) generatedProfilesIterator.next();
                String generatedProfileId = generatedProfile.getId();
                if ( !modelProfileIdMap.containsKey( generatedProfileId ) )
                {
                    model.addProfile( generatedProfile );
                }
                else
                {
                    getLogger().warn( "Try to merge profiles with id " + generatedProfileId );
                    mergeModelBase( (Profile) modelProfileIdMap.get( generatedProfileId ), generatedProfile );
                    mergeProfileBuild( (Profile) modelProfileIdMap.get( generatedProfileId ), generatedProfile );
                }
            }
        }
    }

    private void mergeProfileBuild( Profile modelProfile, Profile generatedProfile )
    {
        if ( generatedProfile.getBuild() != null )
        {
            if ( modelProfile.getBuild() == null )
            {
                modelProfile.setBuild( new Build() );
            }
            mergeBuildPlugins( modelProfile.getBuild(), generatedProfile.getBuild() );
            // TODO: merge more than just plugins in the profile...
        }
    }

    private void mergeModelBase( ModelBase model, ModelBase generatedModel )
    {
        // ModelBase can be a Model or a Profile...

        Map dependenciesByIds = createDependencyMap( model.getDependencies() );
        Map generatedDependenciesByIds = createDependencyMap( generatedModel.getDependencies() );

        Iterator generatedDependencyIds = generatedDependenciesByIds.keySet().iterator();
        while ( generatedDependencyIds.hasNext() )
        {
            String generatedDependencyId = (String) generatedDependencyIds.next();

            if ( !dependenciesByIds.containsKey( generatedDependencyId ) )
            {
                model.addDependency(
                    (Dependency) generatedDependenciesByIds.get( generatedDependencyId )
                );
            }
            else
            {
                getLogger().warn( "Can not override property: " + generatedDependencyId );
            }

        // TODO: maybe warn, if a property key gets overriden?
        model.getProperties().putAll( generatedModel.getProperties() );

        // TODO: maybe merge more than just dependencies and properties...
        }
    }

    private void mergeReportPlugins( Model model,
                                     Model generatedModel )
    {
        if ( generatedModel.getReporting() != null )
        {
            if ( model.getReporting() == null )
            {
                model.setReporting( new Reporting() );
            }

            Map reportPluginsByIds = model.getReporting().getReportPluginsAsMap();
            Map generatedReportPluginsByIds =
                generatedModel.getReporting().getReportPluginsAsMap();

            Iterator generatedReportPluginsIds = generatedReportPluginsByIds.keySet().iterator();
            while ( generatedReportPluginsIds.hasNext() )
            {
                String generatedReportPluginsId = (String) generatedReportPluginsIds.next();

                if ( !reportPluginsByIds.containsKey( generatedReportPluginsId ) )
                {
                    model.getReporting().addPlugin(
                        (ReportPlugin) generatedReportPluginsByIds.get( generatedReportPluginsId )
                    );
                }
                else
                {
                    getLogger().warn( "Can not override report: " + generatedReportPluginsId );
                }
            }
        }
    }
    private void mergeBuildPlugins( BuildBase modelBuild, BuildBase generatedModelBuild )
    {
        Map pluginsByIds = modelBuild.getPluginsAsMap();
        List generatedPlugins = generatedModelBuild.getPlugins();

        Iterator generatedPluginsIterator = generatedPlugins.iterator();
        while ( generatedPluginsIterator.hasNext() )
        {
            Plugin generatedPlugin = (Plugin) generatedPluginsIterator.next();
            String generatedPluginsId = generatedPlugin.getKey();

            if ( !pluginsByIds.containsKey( generatedPluginsId ) )
            {
                modelBuild.addPlugin( generatedPlugin );
            }
            else
            {
                getLogger().info( "Try to merge plugin configuration of plugins with id: " + generatedPluginsId );
                Plugin modelPlugin = (Plugin) pluginsByIds.get( generatedPluginsId );

                modelPlugin.setConfiguration( Xpp3DomUtils.mergeXpp3Dom( (Xpp3Dom) generatedPlugin.getConfiguration(),
                                                                         (Xpp3Dom) modelPlugin.getConfiguration() ) );
            }
        }
    }
}
