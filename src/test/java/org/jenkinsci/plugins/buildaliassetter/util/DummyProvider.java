/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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
package org.jenkinsci.plugins.buildaliassetter.util;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.buildaliassetter.AliasProvider;
import org.jenkinsci.plugins.buildaliassetter.BuildAliasSetter;

public class DummyProvider extends AliasProvider {

    private final List<String> aliases;

    public static BuildAliasSetter buildWrapper(final String... aliases) throws Exception {

        return new BuildAliasSetter(describableList(aliases));
    }

    public static DescribableList<AliasProvider, Descriptor> describableList(final String... aliases) {

        return new DescribableList<AliasProvider, AliasProvider.Descriptor>(
                null, Arrays.asList(new DummyProvider(aliases))
        );
    }

    public DummyProvider(final String... aliases) {

        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public List<String> names(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {

        return aliases;
    }
}
