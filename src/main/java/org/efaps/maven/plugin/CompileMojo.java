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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.efaps.update.schema.program.esjp.ESJPCompiler;
import org.efaps.update.schema.program.jasperreport.JasperReportCompiler;
import org.efaps.update.schema.program.staticsource.CSSCompiler;
import org.efaps.update.schema.program.staticsource.JavaScriptCompiler;
import org.efaps.update.schema.program.staticsource.WikiCompiler;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compiles all ESPJ's and Cascade Style Sheets within eFaps.
 *
 * @author The eFaps Team
 */
@Mojo(name = "compile", requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo
    extends EFapsAbstractMojo
{

    private static final Logger LOG = LoggerFactory.getLogger(CompileMojo.class);
    /**
     * Number of UUID's to generate.
     */
    @Parameter(property = "target", defaultValue = "all")
    private String target;

    /**
     * Executes the compile goal.
     */
    @Override
    public void execute()
    {
        init(true);
        boolean abort = true;
        try {
            if ("all".equalsIgnoreCase(target)) {
                LOG.info("==Compiling all Elements==");
                Application.compileAll(getUserName(), getClasspathElements(), true);
            } else {
                reloadCache();
                startTransaction();
                if ("java".equalsIgnoreCase(target)) {
                    LOG.info("==Compiling Java==");
                    new ESJPCompiler(getClasspathElements()).compile(null, true);
                } else if ("css".equalsIgnoreCase(target)) {
                    LOG.info("==Compiling CSS==");
                    new CSSCompiler().compile();
                } else if ("js".equalsIgnoreCase(target)) {
                    LOG.info("==Compiling Javascript==");
                    new JavaScriptCompiler().compile();
                } else if ("wiki".equalsIgnoreCase(target)) {
                    LOG.info("==Compiling Wiki==");
                    new WikiCompiler().compile();
                } else if ("jasper".equalsIgnoreCase(target)) {
                    LOG.info("==Compiling JasperReports==");
                    new JasperReportCompiler(getClasspathElements()).compile();
                } else {
                    LOG.error("target: " + target + "' not found");
                }
                commitTransaction();
            }
            abort = false;
        } catch (final InstallationException e) {
            LOG.error("Catched", e);
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        } finally {
            try {
                if (abort) {
                    abortTransaction();
                }
            } catch (final EFapsException e) {
                LOG.error("Catched", e);
            }
        }
    }
}
