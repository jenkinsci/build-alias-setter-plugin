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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;

public class TokenMacroAliasProvider extends AliasProvider {

    public final String template;

    @DataBoundConstructor
    public TokenMacroAliasProvider(final String template) {

        this.template = template;
    }

    @Override
    public List<String> names(
            final AbstractBuild<?, ?> build, final BuildListener listener
    ) throws IOException, InterruptedException {

        try {

            return Arrays.asList(
                    TokenMacro.expand(build, listener, template)
            );
        } catch (final MacroEvaluationException e) {

            listener.getLogger().println(e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(TokenMacroAliasProvider.class);
    }

    @Extension
    public static class DescriptorImpl extends AliasProvider.Descriptor {

        @Override
        public String getDisplayName() {

            return "Token macro alias";
        }
    }
}
