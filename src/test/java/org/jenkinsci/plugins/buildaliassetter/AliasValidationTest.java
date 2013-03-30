package org.jenkinsci.plugins.buildaliassetter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import hudson.util.FormValidation;

import org.junit.Test;

public class AliasValidationTest {

    @Test
    public void disalowIntegralAlias() {

        final FormValidation validation = AliasProvider.validateAlias("42");

        assertThat(validation.getMessage(), containsString("42"));
    }

    @Test
    public void disallowAliasThatCollidesWithABuildinOne() {

        final FormValidation validation = AliasProvider.validateAlias("lastBuild");

        assertThat(validation.getMessage(), containsString("lastBuild"));
    }

    @Test
    public void disallowEmptyAliases() {

        final FormValidation validation = AliasProvider.validateAlias("");

        assertThat(validation.getMessage(), containsString("is empty"));
    }

    @Test
    public void validateValidAlias() {

        final FormValidation validation = AliasProvider.validateAlias("version-1.480.3-SNAPSHOT");

        assertThat(validation, nullValue());
    }
}
