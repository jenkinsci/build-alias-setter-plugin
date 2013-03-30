package org.jenkinsci.plugins.buildaliassetter;

import org.junit.Test;

public class AliasTest {

    @Test(expected = IllegalArgumentException.class)
    public void disallowNullName() {

        new Alias(0, null);
    }
}
