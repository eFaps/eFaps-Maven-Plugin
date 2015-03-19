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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
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

    /**
     * SCM to use "scm" or "git".
     */
    @Parameter(defaultValue = "git")
    private String scm;

    /**
     * Path of a SVN base file.
     */
    @Parameter(property = "repo")
    private File gitRepository;

    /**
     * Path of a SVN base file.
     */
    @Parameter(property = "gitFile")
    private File gitFile;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        switch (this.scm) {
            case "svn":
                executeSVN();
                break;
            default:
                executeGit();
                break;
        }
    }

    private void executeGit()
    {
        try {
            if (this.gitRepository != null && this.gitRepository.exists()) {
                final File gitDir = new File(this.gitRepository, ".git");
                final Repository repo = new FileRepository(gitDir);
                final ObjectId oldID = repo.resolve(this.revision + "^{tree}");
                final ObjectId newID = repo.resolve("HEAD" + "^{tree}");
                copyGit(repo, oldID, newID);
            } else if (this.gitFile != null && this.gitFile.exists()) {
                final StringBuilder sb = new StringBuilder();
                final FileInputStream fstream = new FileInputStream(this.gitFile);
                final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                // Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    final String[] strArrary = strLine.split(" ");
                    if (strArrary.length > 0) {
                        final File gitDir = new File(strArrary[0], ".git");
                        final Repository repo = new FileRepository(gitDir);
                        final ObjectId newID = repo.resolve("HEAD" + "^{tree}");
                        if (strArrary.length > 1) {
                            final ObjectId oldID = repo.resolve(strArrary[1] + "^{tree}");
                            copyGit(repo, oldID, newID);
                        }
                        sb.append(strArrary[0]).append(" ").append(newID.name()).append("\n");
                    }
                }
                br.close();
                final FileWriter fwriter = new FileWriter(this.gitFile);
                final BufferedWriter bwriter = new BufferedWriter(fwriter);
                bwriter.write(sb.toString());
                bwriter.close();
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final GitAPIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void copyGit(final Repository _repo,
                         final ObjectId _oldID,
                         final ObjectId _newID)
        throws IOException, GitAPIException
    {

        final List<DiffEntry> list = new Git(_repo)
                        .diff()
                        .setShowNameAndStatusOnly(true)
                        .setOldTree(prepareTreeParser(_repo, _oldID))
                        .setNewTree(prepareTreeParser(_repo, _newID))
                        .call();
        for (final DiffEntry entyr : list) {
            GenerateUpdatePackageMojo.this.log.info(entyr.toString());
            switch (entyr.getChangeType()) {
                case ADD:
                case MODIFY:
                case COPY:
                case RENAME:
                    copyFile(new File(_repo.getDirectory().getParentFile(), entyr.getNewPath()).getPath(), false);
                    break;
                default:
                    break;
            }
        }
    }


    private CanonicalTreeParser prepareTreeParser(final Repository _repository,
                                                  final ObjectId _objectId)
        throws MissingObjectException, IncorrectObjectTypeException, IOException
    {
        final CanonicalTreeParser parser = new CanonicalTreeParser();
        final ObjectReader or = _repository.newObjectReader();
        parser.reset(or, new RevWalk(_repository).parseTree(_objectId));
        return parser;
    }

    private void executeSVN()
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
                updateClient.setIgnoreExternals(true);

                final ISVNEventHandler handler = new ISVNEventHandler()
                {

                    @Override
                    public void checkCancelled()
                        throws SVNCancelException
                    {
                    }

                    @Override
                    public void handleEvent(final SVNEvent _event,
                                            final double _progress)
                        throws SVNException
                    {
                        if (SVNEventAction.UPDATE_UPDATE.equals(_event.getAction())) {
                            GenerateUpdatePackageMojo.this.log.info("updating: " + _event.getFile());
                        }
                    }
                };

                updateClient.setEventHandler(handler);
                GenerateUpdatePackageMojo.this.log.info("updating to revision: " + rev2);
                final long rev = updateClient.doUpdate(this.path, rev2, SVNDepth.INFINITY, true, true);
                GenerateUpdatePackageMojo.this.log.info("updated to revision: " + rev);
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
            copyFile(path, true);
        }
    }

    /**
     * @param _file file to copy
     */
    private void copyFile(final String _path,
                          final boolean _correctPath)
    {
        try {
            final File file = _correctPath ? new File(this.path + StringUtils.removeStart(_path, "trunk")) : new File(
                            _path);
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
