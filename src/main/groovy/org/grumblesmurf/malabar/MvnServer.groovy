/**
 * Copyright (c) 2009 Espen Wiborg <espenhw@grumblesmurf.org>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */ 
package org.grumblesmurf.malabar;

import org.apache.maven.MavenTransferListener;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.AbstractMavenEmbedderLogger;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.embedder.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;

import java.util.Arrays;
import java.util.Properties;

import java.io.File;

public class MvnServer
{
    private Configuration configuration;
    private MavenEmbedder mavenEmbedder;
    private MavenEmbedderLogger logger;
    private MavenTransferListener transferListener;

    def plexus

    private MvnServer() {
        configuration = buildEmbedderConfiguration();
        logger = new MvnServerLogger();
        transferListener = new MvnServerTransferListener();
        
        if (validateConfiguration()) {
            try {
                org.apache.maven.embedder.MavenEmbedderLoggerManager.metaClass.debug =
                    Utils.&println;
                
                mavenEmbedder = new MavenEmbedder(configuration);
                mavenEmbedder.setLogger(logger);
                plexus = mavenEmbedder.plexusContainer
            } catch (MavenEmbedderException e) {
                CLIReportingUtils.showError(logger, "Unable to start the embedder: ", e, false);
                throw new RuntimeException("Unabled to start the embedder", e);
            }
        }
    }

    public MavenEmbedder getEmbedder() {
        return mavenEmbedder;
    }

    public MavenExecutionRequest newRequest(basedir, profiles) {
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        req.baseDirectory = basedir
        req.transferListener = transferListener;
        profiles.each {
            req.addActiveProfile(it);
        }
        plexus.lookup(MavenExecutionRequestPopulator.class).populateDefaults(req);
        return req;
    }

    private Configuration buildEmbedderConfiguration() {
        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE)
            .setMavenEmbedderLogger(logger);
        return configuration;
    }

    private boolean validateConfiguration() {
        ConfigurationValidationResult cvr =
            MavenEmbedder.validateConfiguration(configuration);
        if (!cvr.isValid()) {
            if (cvr.getUserSettingsException() != null) { 
                CLIReportingUtils.showError(logger, "Error reading user settings: ",
                                            cvr.getUserSettingsException(),
                                            false);
            }
            if (cvr.getGlobalSettingsException() != null) { 
                CLIReportingUtils.showError(logger, "Error reading global settings: ",
                                            cvr.getGlobalSettingsException(),
                                            false);
            }
            return false;
        }
        return true;
    }
    
    public boolean run(String pomFileName, String... goals) {
        return run(pomFileName, false, goals).run();
    }

    public RunDescriptor run(String pomFileName, boolean recursive, String... goals) {
        RunDescriptor run = new RunDescriptor(this);
        run.setPom(new File(pomFileName));
        run.setRecursive(recursive);
        run.setGoals(goals);
        return run;
    }
}

class RunDescriptor 
{
    File pom;
    boolean recursive;
    String[] goals;
    String[] profiles = new String[0];
    Properties properties = new Properties();
    
    MvnServer mvnServer;

    RunDescriptor(mvnServer) {
        this.mvnServer = mvnServer;
    }
        
    public void setPom(File pom) {
        this.pom = pom;
    }
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
    public void setGoals(String[] goals) {
        this.goals = goals;
    }
    public void setProfiles(String[] profiles) {
        this.profiles = profiles;
    }
    public RunDescriptor addProperty(String key, String value) {
        properties.put(key, value);
        return this;
    }
    public boolean run() {
        MavenExecutionRequest request =
            mvnServer.newRequest(pom.parentFile, profiles)
        request.setGoals(Arrays.asList(goals))
            .setRecursive(recursive)
            .setUserProperties(properties);

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        try {
            if (Utils._io.get()) {
                System.setOut(new PrintStream(Utils._io.get().outputStream));
                System.setErr(new PrintStream(Utils._io.get().errorStream));
            }
                    
            MavenExecutionResult result = mvnServer.mavenEmbedder.execute(request);
            // TODO: Fix
            // CLIReportingUtils.logResult(request, result, mvnServer.logger);
            return !result.hasExceptions();
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}

public class MvnServerLogger
    extends AbstractMavenEmbedderLogger
{
    private void log(String level, String message, Throwable t) {
        Utils.print("[${level}] ");
        Utils.println(message);
        if (t)
            t.printStackTrace(Utils.getOut());
    }
    
    public void debug(String message, Throwable t) {
        if (isDebugEnabled()) {
            log("DEBUG", message, t);
        }
    }

    public void info(String message, Throwable t) {
        if (isInfoEnabled()) {
            log("INFO", message, t);
        }
    }

    public void warn(String message, Throwable t) {
        if (isWarnEnabled()) {
            log("WARNING", message, t);
        }
    }

    public void error(String message, Throwable t) {
        if (isErrorEnabled()) {
            log("ERROR", message, t);
        }
    }

    public void fatalError(String message, Throwable t) {
        error(message, t);
    }

    public void close() {
    }
}
