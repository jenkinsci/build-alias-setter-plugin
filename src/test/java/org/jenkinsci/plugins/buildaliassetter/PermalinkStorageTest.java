package org.jenkinsci.plugins.buildaliassetter;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import hudson.model.AbstractBuild;
import hudson.model.PermalinkProjectAction.Permalink;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PermalinkStorageTest {

    @Mock private AbstractBuild<?, ?> someBuild;
    @Mock private AbstractBuild<?, ?> someOtherBuild;

    private final PermalinkStorage storage = new PermalinkStorage();

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        Mockito.when(someBuild.getNumber()).thenReturn(42);
        Mockito.when(someOtherBuild.getNumber()).thenReturn(43);
    }

    @Test
    public void shouldBeEmptyAfterInitialization() {

        assertThat(storage.getPermalinks().size(), equalTo(0));
    }

    @Test
    public void shouldContainInsertedElements() {

        storage.addAliases(someBuild, aliases("a"));
        storage.addAliases(someBuild, aliases("b"));
        storage.addAliases(someOtherBuild, aliases("c"));

        assertThat(storage.getPermalinks(), contains(alias(42, "a"), alias(42, "b"), alias(43, "c")));
    }

    @Test
    public void shouldNotAddSameAliases() {

        storage.addAliases(someBuild, aliases("a", "a"));
        storage.addAliases(someBuild, aliases("a"));

        assertThat(storage.getPermalinks().size(), equalTo(1));
        assertThat(storage.getPermalinks(), contains(alias(42, "a")));
    }

    @Test
    public void deletedAliasesShouldNotExists() {

        storage.addAliases(someBuild, aliases("a"));
        storage.addAliases(someBuild, aliases("b"));
        storage.addAliases(someOtherBuild, aliases("c"));

        storage.deleteAliases(someOtherBuild);

        assertThat(storage.getPermalinks(), contains(alias(42, "a"), alias(42, "b")));

        storage.deleteAliases(someBuild);

        assertThat(storage.getPermalinks().size(), equalTo(0));
    }

    private LinkedHashSet<String> aliases(final String... aliases) {

        return new LinkedHashSet<String>(Arrays.asList(aliases));
    }

    private Permalink alias(final int buildNumber, final String name) {

        return new Alias(buildNumber, name);
    }
}
