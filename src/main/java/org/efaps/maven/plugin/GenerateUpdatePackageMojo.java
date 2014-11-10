/*
 * Copyright 2003 - 2014 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: GenerateUpdatePackageMojo.java 12861 2014-05-26 15:01:24Z
 *          jan@moxter.net $
 */
@Mojo(name = "generateUpdatePackage")
public class GenerateUpdatePackageMojo
    implements org.apache.maven.plugin.Mojo
{

    /**
     * The apache maven logger is stored in this instance variable.
     *
     * @see #getLog
     * @see #setLog
     */
    private Log log = null;

    /**
     * Path of a SVN base file.
     */
    @Parameter(property = "path", required = true)
    private File path;

    /**
     * The revision to use.
     */
    @Parameter(property = "rev", required = true)
    private String revision;

    /**
     * Replace the files if duplicated.
     */
    @Parameter(property = "replace")
    private final boolean replace = false;

    /**
     * Execute an svn update first before comparing.
     */
    @Parameter(property = "update")
    private final boolean update = true;

    /**
     * Root Directory with the XML installation files.
     */
    @Parameter(property = "check4overwrite")
    private final boolean check4overwrite = true;

    /**
     * The directory where the generated Class will be stored. The directory
     * will be registered as a compile source root of the project such that the
     * generated files will participate in later build phases like compiling and
     * packaging.
     */
    @Parameter(defaultValue = "${project.build.directory}/updatePackage")
    private File outputDirectory;

    /**
     * Location of the version file (defining all versions to install).
     */
    @Parameter(defaultValue = "${basedir}/src")
    private File baseDir;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        GenerateUpdatePackageMojo.this.log.info("path: " + this.path);

        final SVNClientManager clientManager = SVNClientManager.newInstance();
        final SVNDiffClient diffClient = clientManager.getDiffClient();

        final String[] revAr = this.revision.split(":");
        final SVNRevision rev1 = SVNRevision.parse(revAr[0]);
        final SVNRevision rev2;
        if (revAr.length > 1) {
            rev2 = SVNRevision.parse(revAr[1]);
        } else {
            rev2 = SVNRevision.HEAD;
        }
        if (this.update) {
            final SVNUpdateClient updateClient = clientManager.getUpdateClient();
            try {
                GenerateUpdatePackageMojo.this.log.info("updating to revision: " + rev2);
                updateClient.doUpdate(this.path, rev2, SVNDepth.INFINITY, true, true);
            } catch (final SVNException e) {
                this.log.error(e);
            }
        }
        final List<String> paths = new ArrayList<String>();

        final ISVNDiffStatusHandler handler = new ISVNDiffStatusHandler()
        {

            @Override
            public void handleDiffStatus(final SVNDiffStatus _diffStatus)
                throws SVNException
            {
                if (SVNStatusType.STATUS_DELETED.equals(_diffStatus.getModificationType())) {
                    GenerateUpdatePackageMojo.this.log.debug("DELETED: " + _diffStatus.getFile());
                } else {
                    GenerateUpdatePackageMojo.this.log.info("ADDED: " + _diffStatus.getFile());
                    paths.add(_diffStatus.getPath());
                }
            }
        };
        try {
            diffClient.getOperationsFactory().setPrimaryWcGeneration(SvnWcGeneration.V17);
            diffClient.doDiffStatus(this.path, rev1, this.path, rev2, SVNDepth.INFINITY, true, handler);
        } catch (final SVNException e) {
            this.log.error(e);
        }

        for (final String path : paths) {
            copyFile(path);
        }

    }

    /**
     * @param _file file to copy
     */
    private void copyFile(final String _path)
    {
        try {
            final File file = new File(this.path + StringUtils.removeStart(_path, "trunk"));
            File outDir;
            if (this.check4overwrite) {
                final Collection<File> files = FileUtils.listFiles(this.baseDir, new NameFileFilter(file.getName()),
                                TrueFileFilter.INSTANCE);
                if (files.isEmpty() || files.contains(file)) {
                    outDir = this.outputDirectory;
                } else {
                    outDir = new File(this.outputDirectory, "overwrite");
                }
            } else {
                outDir = this.outputDirectory;
            }
            if (file.exists()) {
                if (!this.replace) {
                    final File checkfile = new File(outDir.getAbsolutePath(), file.getName());
                    if (checkfile.exists()) {
                        checkfile.renameTo(new File(outDir.getAbsolutePath(), "_" + new Date().getTime()
                                        + file.getName()));
                        this.log.info("RENAMED: " + checkfile);
                    }
                }
                if (_path.endsWith("xml")) {
                    FileUtils.copyFileToDirectory(file, outDir);
                    this.log.info("COPIED: " + file);
                } else if (_path.endsWith("java")) {
                    FileUtils.copyFileToDirectory(file, outDir);
                    this.log.info("COPIED: " + file);
                } else if (_path.endsWith("properties")) {
                    for (final File aFile : FileUtils.listFiles(file.getParentFile(), new String[] { "xml",
                                    "properties" }, false)) {
                        FileUtils.copyFileToDirectory(aFile, outDir);
                        this.log.info("COPIED: " + aFile);
                    }
                }
            }
        } catch (final IOException e) {
            this.log.error("Catched IOException", e);
        }
    }

    @Override
    public void setLog(final Log _log)
    {
        this.log = _log;
    }

    @Override
    public Log getLog()
    {
        return this.log;
    }
}
