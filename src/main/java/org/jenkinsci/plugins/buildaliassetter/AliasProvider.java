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

import java.io.IOException;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildListener;
import hudson.util.FormValidation;

/**
 * Provide custom aliases to be attached to the build.
 *
 * This extension point allows plugin to create new sources of build aliases.
 *
 * @author ogondza
 */
public abstract class AliasProvider extends AbstractDescribableImpl<AliasProvider> implements ExtensionPoint {

    /**
     * Get build aliases to be attached to the build
     *
     * Aliases not conforming to the BuildAliasSetter#validateAlias(String) are
     * not going to be attached and an error will be reported.
     *
     * @return A list of aliases. Possibly empty, never null.
     * @see AliasProvider#validateAlias(String)
     */
    public abstract List<String> names(
            final AbstractBuild<?, ?> build, final BuildListener listener
    ) throws IOException, InterruptedException;

    /**
     * Validate custom alias
     *
     * Aliases that does not conform to this contract will not be attached to the
     * build. {@link AliasProvider} implementations might use this method from
     * their doCheckXXX methods to provide early feedback.
     *
     * This implementation ensures that an alias can not possibly collide with
     * the build number (it must not be an integer) and buildin permalink
     * ("lastBuild", "lastSuccessfulBuild", etc.).
     *
     * @return null if valid, {@link FormValidation} describing the cause otherwise.
     */
    public static FormValidation validateAlias(final String aliasCandidate) {

        if (aliasCandidate.isEmpty()) return FormValidation.error(
                "Custom build alias is empty"
        );

        try {

            Integer.parseInt(aliasCandidate);

            return FormValidation.error(
                    "Custom build alias '" + aliasCandidate + "' might collide with build number"
            );
        } catch (final NumberFormatException ex) {/* not an int */}

        for (final Permalink buildin: Permalink.BUILTIN) {

            if (aliasCandidate.equalsIgnoreCase(buildin.getId())) {

                return FormValidation.error(
                        "Custom build alias '" + aliasCandidate + "' collide with buildin permalink"
                );
            }
        }

        return null;
    }

    public static abstract class Descriptor extends hudson.model.Descriptor<AliasProvider> {

        @Override
        public AliasProvider newInstance(final StaplerRequest req, final JSONObject formData) throws FormException {

            return req.bindJSON(clazz, formData);
        }
    }
}
