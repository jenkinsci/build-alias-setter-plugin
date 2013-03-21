/*
 * The MIT License
 *
 * Copyright (c) 2013 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.buildaliassetter;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author ogondza
 */
public class BuildAliasSetter extends BuildWrapper implements MatrixAggregatable {

    public final String template;
    
    @DataBoundConstructor
    public BuildAliasSetter(String template) {
        this.template = template;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        
        setAlias(build, listener);

        return new Environment() {};
    }
    
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build,launcher,listener) {
            @Override
            public boolean startBuild() throws InterruptedException, IOException {
                setAlias(build, listener);
                return super.startBuild();
            }
        };
    }

    private void setAlias(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        
        final String name = getName(build, listener);
        if (name == null) return;
        
        final AbstractProject<?, ?> project = build.getProject();
        
        getStorage(project).addAlias(build.getNumber(), name);
        
        project.save();
    }
    
    private String getName(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        
        final String name = expandMacro(build, listener);
        if (name == null) return null;

        try {

            Integer.parseInt(name);
            logInvalidName(listener, name);
            return null;
        } catch (NumberFormatException ex) {/* not an int */}

        for (final Permalink buildin: Permalink.BUILTIN) {

            if (name.equalsIgnoreCase(buildin.getId())) {

                logInvalidName(listener, name);
                return null;
            }
        }

        return name;
    }

    private void logInvalidName(BuildListener listener, String name) {

        listener.getLogger().println(String.format(
                "BuildAliasSetter: Unable to use '%s' as a custom build alias.",
                name
        ));
    }

    private String expandMacro(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {

        try {

            return TokenMacro.expand(build, listener, template);
        } catch (MacroEvaluationException e) {

            listener.getLogger().println(e.getMessage());
        }

        return null;
    }

    private PermalinkStorage getStorage(final AbstractProject<?, ?> project) throws IOException {

        PermalinkStorage storage = project.getProperty(PermalinkStorage.class);
        if (storage != null) {

            storage = new PermalinkStorage();
            project.addProperty(storage);
        }
        return storage;
    }

    @Extension
    public static class DanglingAliasDeleter extends RunListener<AbstractBuild<?, ?>> {

        private final static Logger LOGGER = Logger.getLogger(BuildAliasSetter.class.getName());

        /**
         * Delete aliases for builds that does not longer exists
         */
        @Override
        public void onDeleted(final AbstractBuild<?, ?> build) {

            final AbstractProject<?, ?> project = build.getProject();
            final PermalinkStorage storage = project.getProperty(PermalinkStorage.class);

            if (storage == null) return;

            storage.deleteAliases(build);
            try {

                project.save();
            } catch (IOException ex) {

                final String msg = "Unable to save project after deleting dangling aliases for job " + build.getDisplayName();
                LOGGER.log(Level.SEVERE, msg, ex);
            }
        }
    }
    
    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Set Build Alias";
        }
    }
}
