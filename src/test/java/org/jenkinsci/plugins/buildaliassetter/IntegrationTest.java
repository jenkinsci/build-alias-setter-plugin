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
package org.jenkinsci.plugins.buildaliassetter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;

import org.jenkinsci.plugins.buildaliassetter.util.DummyProvider;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.PermalinkProjectAction.Permalink;

public class IntegrationTest {

    public @Rule JenkinsRule j = new JenkinsRule();

    @Test @Bug(21734)
    public void doNotCopyAliasesWithJob() throws Exception {
        MatrixProject src = j.jenkins.createProject(MatrixProject.class, "src");
        PermalinkStorage storage = new PermalinkStorage();

        src.addProperty(storage);
        MatrixProject dst = (MatrixProject) j.jenkins.copy((TopLevelItem) src, "dst");

        assertNull("Permalinks copied with job", dst.getProperty(PermalinkStorage.class));
    }

    @Test @Bug(20405)
    public void doNotShowAliasPermalinkTwice() throws Exception {
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");

        p.getBuildWrappersList().add(DummyProvider.buildWrapper("duplicated"));

        p.scheduleBuild2(0).get();
        List<Permalink> aliases = p.getAction(PermalinkStorage.class).getPermalinks();
        assertEquals(1, aliases.size());
        assertEquals(new Alias(1, "duplicated"), aliases.get(0));

        p.getBuildWrappersList().remove(BuildAliasSetter.class);
        p.getBuildWrappersList().add(DummyProvider.buildWrapper("duplicated", "original"));

        p.scheduleBuild2(0).get();
        aliases = p.getAction(PermalinkStorage.class).getPermalinks();
        assertEquals(2, aliases.size());
        assertEquals(new Alias(2, "duplicated"), aliases.get(0));
        assertEquals(new Alias(2, "original"), aliases.get(1));
    }

    @Test
    public void pointToLastBuildWithAlias() throws Exception {
        String tag = UUID.randomUUID().toString();

        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");
        p.getBuildWrappersList().add(DummyProvider.buildWrapper(tag));

        for (int i = 1; i < 50; i++) {
            Build expected = j.buildAndAssertSuccess(p);
            Run<?, ?> actual = resolve(p, tag);
            assertEquals(expected.getNumber(), actual.getNumber());
        }
    }

    private Run<?, ?> resolve(AbstractProject<?, ?> job, String alias) {
        for (Permalink p : job.getPermalinks()) {
            if(p.getId().equals(alias))
                return p.resolve(job);
        }

        throw new AssertionError();
    }
}
