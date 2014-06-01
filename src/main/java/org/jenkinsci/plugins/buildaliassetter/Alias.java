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

import hudson.model.Job;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Run;

/**
 * An Alias for a build
 *
 * @author ogondza
 */
/*package*/ class Alias extends Permalink {

    private final String name;
    private final int buildNumber;

    public Alias(final int buildNumber, final String name) {

        if (name == null) throw new IllegalArgumentException("No name provided");

        this.buildNumber = buildNumber;
        this.name = name;
    }

    @Override
    public Run<?, ?> resolve(final Job<?, ?> job) {

        return job.getBuildByNumber(buildNumber);
    }

    @Override
    public String getId() {

        return name;
    }

    @Override
    public String getDisplayName() {

        return name;
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + buildNumber;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) return true;

        if (obj == null) return false;

        if (getClass() != obj.getClass()) return false;
        final Alias other = (Alias) obj;

        return buildNumber == other.buildNumber && name.equals(other.name);
    }

    @Override
    public String toString() {
        return String.format("Build alias '%s' for #%d", name, buildNumber);
    }
}
