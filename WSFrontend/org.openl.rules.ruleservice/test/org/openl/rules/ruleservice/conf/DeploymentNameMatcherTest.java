package org.openl.rules.ruleservice.conf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeploymentNameMatcherTest {

    @Test
    public void shouldMatch_whenSourcePatternIsNull() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher(null);
        assertTrue(matcher.hasMatches("foo"));
        assertTrue(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatch_whenSourcePatternIsBlank() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher(" ");
        assertTrue(matcher.hasMatches("foo"));
        assertTrue(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatchByFullName() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher("foo");
        assertTrue(matcher.hasMatches("foo"));
        assertFalse(matcher.hasMatches("foo-bar"));
        assertFalse(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatchByPrefixPattern() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher("foo*");
        assertTrue(matcher.hasMatches("foo"));
        assertTrue(matcher.hasMatches("foo-bar"));
        assertFalse(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatchByPostfixPattern() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher("*foo");
        assertTrue(matcher.hasMatches("foo"));
        assertTrue(matcher.hasMatches("bar-foo"));
        assertFalse(matcher.hasMatches("foo-bar"));
        assertFalse(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatchByPostfixOrPrefixPattern() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher("*foo, bar*");
        assertTrue(matcher.hasMatches("foo"));
        assertTrue(matcher.hasMatches("buz-foo"));
        assertFalse(matcher.hasMatches("foo-bar"));
        assertTrue(matcher.hasMatches("bar"));
    }

    @Test
    public void shouldMatchByPrefixPattern_NotMatchByPostfixPattern_whenPostfixPatternNotCorrect() {
        DeploymentNameMatcher matcher = new DeploymentNameMatcher("**foo, bar*");
        assertFalse(matcher.hasMatches("foo"));
        assertFalse(matcher.hasMatches("buz-foo"));
        assertFalse(matcher.hasMatches("foo-bar"));
        assertTrue(matcher.hasMatches("bar"));
    }

}
