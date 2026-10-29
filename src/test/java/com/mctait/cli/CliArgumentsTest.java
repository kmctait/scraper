package com.mctait.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliArgumentsTest {

    @Test
    void parse_withNoArguments_usesDefaults() {
        CliArguments args = CliArguments.parse(new String[0]);

        assertEquals(CliArguments.DEFAULT_KEYWORDS, args.keywords());
        assertEquals(CliArguments.DEFAULT_LOCATION, args.location());
        assertNull(args.outputLocation());
        assertFalse(args.help());
    }

    @Test
    void parse_spaceSeparatedValues_consumesAllTokensUntilNextFlag() {
        CliArguments args = CliArguments.parse(new String[]{
                "--keywords", "Python", "Developer", "--location", "Milton", "Keynes"});

        assertEquals("Python Developer", args.keywords());
        assertEquals("Milton Keynes", args.location());
    }

    @Test
    void parse_inlineValues_keepsMultiWordValue() {
        CliArguments args = CliArguments.parse(new String[]{
                "--keywords=Python Developer", "--location=Bristol"});

        assertEquals("Python Developer", args.keywords());
        assertEquals("Bristol", args.location());
    }

    @Test
    void parse_quotedValueIsSingleToken_keepsWholeValue() {
        CliArguments args = CliArguments.parse(new String[]{
                "--keywords", "Software Engineer", "--location", "Manchester"});

        assertEquals("Software Engineer", args.keywords());
        assertEquals("Manchester", args.location());
    }

    @Test
    void parse_mixedInlineAndTrailingTokens_joinsAllTokens() {
        CliArguments args = CliArguments.parse(new String[]{
                "--keywords=Senior Java", "Developer", "--location", "Leeds"});

        assertEquals("Senior Java Developer", args.keywords());
        assertEquals("Leeds", args.location());
    }

    @Test
    void parse_outputLocation_isOptional() {
        CliArguments defaults = CliArguments.parse(new String[]{"--keywords", "Java"});
        CliArguments override = CliArguments.parse(new String[]{"--output-location", "/tmp/jobs"});

        assertNull(defaults.outputLocation());
        assertEquals("/tmp/jobs", override.outputLocation());
    }

    @Test
    void parse_missingValue_throws() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CliArguments.parse(new String[]{"--keywords"}));

        assertTrue(error.getMessage().contains("Missing value"));
    }

    @Test
    void parse_unknownOption_throws() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CliArguments.parse(new String[]{"--keywrods", "Java"}));

        assertTrue(error.getMessage().contains("Unknown option"));
    }

    @Test
    void parse_positionalArgument_throws() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CliArguments.parse(new String[]{"Python", "Developer"}));

        assertTrue(error.getMessage().contains("Unexpected argument"));
    }

    @Test
    void parse_help_isRecognised() {
        assertTrue(CliArguments.parse(new String[]{"--help"}).help());
        assertTrue(CliArguments.parse(new String[]{"-h"}).help());
    }

    @Test
    void parse_frameworkArguments_areIgnored() {
        CliArguments args = CliArguments.parse(new String[]{
                "--spring.profiles.active=test", "--keywords", "Java", "Developer"});

        assertEquals("Java Developer", args.keywords());
    }
}
