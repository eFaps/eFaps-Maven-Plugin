/*
 * Copyright 2003 - 2009 The eFaps Team
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

import org.efaps.maven_java5.org.apache.maven.tools.plugin.Goal;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Parameter;
import org.efaps.update.schema.program.esjp.ESJPCompiler;
import org.efaps.update.schema.program.jasperreport.JasperReportCompiler;
import org.efaps.update.schema.program.staticsource.CSSCompiler;
import org.efaps.update.schema.program.staticsource.JavaScriptCompiler;
import org.efaps.update.schema.program.staticsource.WikiCompiler;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.efaps.util.EFapsException;

/**
 * Compiles all ESPJ's and Cascade Style Sheets within eFaps.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@Goal(name = "compile", requiresDependencyResolutionScope = "compile")
public final class CompileMojo
    extends EFapsAbstractMojo
{

    /**
     * Number of UUID's to generate.
     */
    @Parameter(expression = "${target}", defaultValue = "all")
    private String target;

    /**
     * Executes the compile goal.
     */
    public void execute()
    {
        init();
        boolean abort = true;
        try {
            if ("all".equalsIgnoreCase(this.target)) {
                getLog().info("==Compiling all Elements==");
                Application.compileAll(getUserName(), getClasspathElements(), true);
            } else {
                reloadCache();
                startTransaction();
                if ("java".equalsIgnoreCase(this.target)) {
                    getLog().info("==Compiling Java==");
                    (new ESJPCompiler(getClasspathElements())).compile(null, true);
                } else if ("css".equalsIgnoreCase(this.target)) {
                    getLog().info("==Compiling CSS==");
                    new CSSCompiler().compile();
                } else if ("js".equalsIgnoreCase(this.target)) {
                    getLog().info("==Compiling Javascript==");
                    new JavaScriptCompiler().compile();
                } else if ("wiki".equalsIgnoreCase(this.target)) {
                    getLog().info("==Compiling Wiki==");
                    new WikiCompiler().compile();
                } else if ("jasper".equalsIgnoreCase(this.target)) {
                    getLog().info("==Compiling JasperReports==");
                    new JasperReportCompiler(getClasspathElements()).compile();
                } else {
                    getLog().error("target: " + this.target + "' not found");
                }
                commitTransaction();
            }
            abort = false;
        } catch (final InstallationException e) {
            getLog().error(e);
        } catch (final EFapsException e) {
            getLog().error(e);
        } finally {
            try {
                if (abort) {
                    abortTransaction();
                }
            } catch (final EFapsException e) {
                getLog().error(e);
            }
        }
    }
}
