/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.maven.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author The eFaps Team
 */
@Mojo(name = "generate-UpdatePackage")
public class GenerateUpdatePackageMojo
    extends EFapsAbstractMojo
{

    private static final Logger LOG = LoggerFactory.getLogger(CompileMojo.class);

    @Parameter(property = "path")
    private File path;

    /**
     * The revision to use.
     */
    @Parameter(property = "rev")
    private String revision;

    /**
     * Replace the files if duplicated.
     */
    @Parameter(property = "replace")
    private final boolean replace = false;

    /*
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


    @Parameter(property = "repo")
    private File gitRepository;

    @Parameter(property = "gitFile")
    private File gitFile;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

    }

    /**
     * Execute git.
     */
    private void executeGit()
    {
        try {
            if (gitRepository != null && gitRepository.exists()) {
                final File gitDir = new File(gitRepository, ".git");
                final Repository repo = new FileRepository(gitDir);
                final ObjectId oldID = repo.resolve(revision + "^{tree}");
                final ObjectId newID = repo.resolve("HEAD^{tree}"); // HEAD^{tree}
                copyGit(repo, oldID, newID);
            } else if (gitFile != null && gitFile.exists()) {
                final StringBuilder sb = new StringBuilder();
                final FileInputStream fstream = new FileInputStream(gitFile);
                final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                // Read File Line By Line
                while ((strLine = br.readLine()) != null) {
                    final String[] strArrary = strLine.split(" ");
                    if (strArrary.length > 0) {
                        final File gitDir = new File(strArrary[0], ".git");
                        LOG.info("Using Repository: " + gitDir);
                        final Repository repo = new FileRepository(gitDir);
                        final ObjectId newID = repo.resolve("HEAD^{tree}");
                        if (strArrary.length > 1) {
                            final ObjectId oldID = repo.resolve(strArrary[1] + "^{tree}");
                            copyGit(repo, oldID, newID);
                        }
                        sb.append(strArrary[0]).append(" ").append(newID.name()).append("\n");
                    }
                }
                br.close();
                final FileWriter fwriter = new FileWriter(gitFile);
                final BufferedWriter bwriter = new BufferedWriter(fwriter);
                bwriter.write(sb.toString());
                bwriter.close();
            }
        } catch (final IOException e) {
            LOG.error("Catched", e);
        } catch (final GitAPIException e) {
            LOG.error("Catched",e);
        }
    }

    /**
     * Copy git.
     *
     * @param _repo the repo
     * @param _oldID the old id
     * @param _newID the new id
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitAPIException the git api exception
     */
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
        for (final DiffEntry entry : list) {
            LOG.info(entry.toString());
            switch (entry.getChangeType()) {
                case ADD:
                case MODIFY:
                case COPY:
                case RENAME:
                    final File file = new File(_repo.getDirectory().getParentFile(), entry.getNewPath());
                    copyFile(file.getPath(), false);
                    final FileInfo fileInfo = getFileInformation(file, true);
                    final StringBuilder line = new StringBuilder().append(file.getName()).append(" ")
                                    .append(fileInfo.getRev()).append(" ")
                                   .append(fileInfo.getDate()).append("\n") ;
                    FileUtils.writeStringToFile(new File(outputDirectory, "_revFile.txt"), line.toString(), true);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Prepare tree parser.
     *
     * @param _repository the repository
     * @param _objectId the object id
     * @return the canonical tree parser
     * @throws MissingObjectException the missing object exception
     * @throws IncorrectObjectTypeException the incorrect object type exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private CanonicalTreeParser prepareTreeParser(final Repository _repository,
                                                  final ObjectId _objectId)
        throws MissingObjectException, IncorrectObjectTypeException, IOException
    {
        final CanonicalTreeParser parser = new CanonicalTreeParser();
        final ObjectReader or = _repository.newObjectReader();
        parser.reset(or, new RevWalk(_repository).parseTree(_objectId));
        return parser;
    }


    /**
     * @param _file file to copy
     */
    private void copyFile(final String _path,
                          final boolean _correctPath)
    {
        try {
            final File file = _correctPath
                            ? new File(path + StringUtils.removeStart(_path, "trunk"))
                            : new File(_path);
            File outDir;
            if (check4overwrite) {
                final Collection<File> files = FileUtils.listFiles(baseDir, new NameFileFilter(file.getName()),
                                TrueFileFilter.INSTANCE);
                if (files.isEmpty() || files.contains(file)) {
                    outDir = outputDirectory;
                } else {
                    outDir = new File(outputDirectory, "overwrite");
                }
            } else {
                outDir = outputDirectory;
            }
            if (file.exists()) {
                if (!replace) {
                    final File checkfile = new File(outDir.getAbsolutePath(), file.getName());
                    if (checkfile.exists()) {
                        checkfile.renameTo(new File(outDir.getAbsolutePath(), "_" + new Date().getTime()
                                        + file.getName()));
                        LOG.info("RENAMED: " + checkfile);
                    }
                }
                if (_path.endsWith("xml")) {
                    FileUtils.copyFileToDirectory(file, outDir);
                    LOG.info("COPIED: " + file);
                } else if (_path.endsWith("java")) {
                    FileUtils.copyFileToDirectory(file, outDir);
                    LOG.info("COPIED: " + file);
                } else if (_path.endsWith("css")) {
                    FileUtils.copyFileToDirectory(file, outDir);
                    LOG.info("COPIED: " + file);
                } else if (_path.endsWith("properties")) {
                    for (final File aFile : FileUtils.listFiles(file.getParentFile(), new String[] { "xml",
                                    "properties" }, false)) {
                        FileUtils.copyFileToDirectory(aFile, outDir);
                        LOG.info("COPIED: " + aFile);
                    }
                }
            }
        } catch (final IOException e) {
            LOG.error("Catched IOException", e);
        }
    }
}
