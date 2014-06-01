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
import hudson.model.Hudson;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Set aliases to the build
 *
 * This implementation sets the aliases twice, before and after the run. It is
 * desirable to set aliases as soon as possible to reach the build before it's
 * competition. On the other hand some data necessary to get the aliases might
 * not be available before the build has finished. No alias will be set twice
 * for the same build.
 *
 * @author ogondza
 */
public class BuildAliasSetter extends BuildWrapper implements MatrixAggregatable {

    private /*final*/ @Nonnull DescribableList<AliasProvider, AliasProvider.Descriptor> providers;

    public BuildAliasSetter(@Nonnull DescribableList<AliasProvider, AliasProvider.Descriptor> providers) {
        this.providers = providers;
    }

    public @Nonnull DescribableList<AliasProvider, AliasProvider.Descriptor> configuredProviders() {
        return providers;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Environment setUp(
            final AbstractBuild build, final Launcher launcher, final BuildListener listener
    ) throws IOException, InterruptedException {

        setAliases(build, listener);

        return new Environment() {

            @Override
            public boolean tearDown(
                    final AbstractBuild build, final BuildListener listener
            ) throws IOException, InterruptedException {

                setAliases(build, listener);
                return super.tearDown(build, listener);
            }
        };
    }

    public MatrixAggregator createAggregator(
            final MatrixBuild build, final Launcher launcher, final BuildListener listener
    ) {

        return new MatrixAggregator(build, launcher, listener) {

            @Override
            public boolean startBuild() throws InterruptedException, IOException {

                setAliases(build, listener);
                return super.startBuild();
            }

            @Override
            public boolean endBuild() throws InterruptedException, IOException {

                setAliases(build, listener);
                return super.endBuild();
            }
        };
    }

    private void setAliases(
            final AbstractBuild<?, ?> build, final BuildListener listener
    ) throws IOException, InterruptedException {

        final LinkedHashSet<String> aliases = aliases(build, listener);

        if (aliases.isEmpty()) {

            printToConsole(listener, "no build aliases set");
            return;
        } else {

            printToConsole(listener, "setting build aliases " + aliases.toString());
        }

        final AbstractProject<?, ?> project = build.getProject();

        getStorage(project).addAliases(build, aliases);

        project.save();
    }

    private LinkedHashSet<String> aliases(
            final AbstractBuild<?, ?> build, final BuildListener listener
    ) throws IOException, InterruptedException {

        final LinkedHashSet<String> aliases = new LinkedHashSet<String>(providers.size());
        for(final AliasProvider provider: providers) {

            final List<String> names = provider.names(build, listener);
            aliases.addAll(names);
        }

        return filterAliases(aliases, listener);
    }

    private PermalinkStorage getStorage(final AbstractProject<?, ?> project) throws IOException {

        PermalinkStorage storage = project.getProperty(PermalinkStorage.class);
        if (storage == null) {

            storage = new PermalinkStorage();
            project.addProperty(storage);
        }

        return storage;
    }

    private LinkedHashSet<String> filterAliases(
            final LinkedHashSet<String> aliasCandidates, final BuildListener listener
    ) {

        // make sure there is no null
        aliasCandidates.remove(null);

        final LinkedHashSet<String> aliases = new LinkedHashSet<String>(aliasCandidates.size());
        for (final String aliasCandidate: aliasCandidates) {

            final FormValidation validation = AliasProvider.validateAlias(aliasCandidate);
            if (validation != null) {

                printToConsole(listener, validation.getMessage());
                continue;
            }

            aliases.add(aliasCandidate);
        }

        return aliases;
    }

    private void printToConsole(final BuildListener listener, final String message) {

        listener.getLogger().println("BuildAliasSetter: " + message);
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    // JENKINS-23264
    private Object readResolve() {
        DescriptorImpl d = getDescriptor();
        if (providers == null) {
            providers = d.builders;
        }

        if (providers == null) {
            providers = d.emptyProviders();
        }

        return this;
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Deprecated // JENKINS-23264
        private DescribableList<AliasProvider, AliasProvider.Descriptor> builders;
        public DescriptorImpl() {
            load(); // JENKINS-23264
        }

        @Override
        public BuildAliasSetter newInstance(
                final StaplerRequest req, final JSONObject formData
        ) throws FormException {

            final DescribableList<AliasProvider, AliasProvider.Descriptor> providers = emptyProviders();
            try {

                providers.rebuildHetero(req, formData, providerKinds(), "providers");
            } catch (final IOException ex) {

                throw new FormException("rebuildHetero failed", ex, "none");
            }

            return new BuildAliasSetter(providers);
        }

        @Override
        public boolean isApplicable(final AbstractProject<?, ?> item) {

            return true;
        }

        @Override
        public String getDisplayName() {

            return "Set Build Alias";
        }

        public List<AliasProvider.Descriptor> providerKinds() {

            return Hudson.getInstance().getDescriptorList(AliasProvider.class);
        }

        private DescribableList<AliasProvider, AliasProvider.Descriptor> emptyProviders() {

            return new DescribableList<AliasProvider, AliasProvider.Descriptor>(this);
        }
    }

    @Extension
    public static class DanglingAliasDeleter extends RunListener<AbstractBuild<?, ?>> {

        private final static Logger LOGGER = Logger.getLogger(BuildAliasSetter.class.getName());

        /**
         * Delete aliases for builds that are being deleted
         */
        @Override
        public void onDeleted(final AbstractBuild<?, ?> build) {

            final AbstractProject<?, ?> project = build.getProject();
            final PermalinkStorage storage = project.getProperty(PermalinkStorage.class);

            if (storage == null) return;

            storage.deleteAliases(build);
            try {

                project.save();
            } catch (final IOException ex) {

                final String msg = "Unable to save project after deleting dangling aliases for job " + build.getDisplayName();
                LOGGER.log(Level.SEVERE, msg, ex);
            }
        }
    }
}
