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
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor.FormException;
import hudson.model.Job;
import hudson.model.PermalinkProjectAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * List of {@link Alias}es attached to particular {@link Job}
 *
 * @author ogondza
 */
public class PermalinkStorage extends JobProperty<Job<?,?>> implements PermalinkProjectAction {

    private final Map<Integer, LinkedHashSet<Alias>> permalinks;

    @DataBoundConstructor
    public PermalinkStorage() {

        permalinks = new HashMap<Integer, LinkedHashSet<Alias>>();
    }

    public List<Permalink> getPermalinks() {

        final List<Permalink> links = new ArrayList<Permalink>(permalinks.size());
        for (final LinkedHashSet<Alias> aliases: permalinks.values()) {

            links.addAll(aliases);
        }

        return links;
    }

    /*package*/ void addAliases(final int buildNumber, final LinkedHashSet<String> aliases) {

        LinkedHashSet<Alias> bucket = permalinks.get(buildNumber);
        if (bucket == null) {
            bucket = new LinkedHashSet<Alias>(aliases.size());
            permalinks.put(buildNumber, bucket);
        }

        for (final String alias: aliases) {

            final Alias newAlias = new Alias(buildNumber, alias);
            if (bucket.contains(newAlias)) continue;
            bucket.add(newAlias);
        }
    }

    /*package*/ void deleteAliases(final AbstractBuild<?, ?> build) {

        permalinks.remove(build.getNumber());
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

    @Override
    public JobProperty<?> reconfigure(final StaplerRequest req, final JSONObject form) throws FormException {

        // Do not reconfigure - keep same instance
        return this;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {

            return "Permalink storage";
        }
    }
}