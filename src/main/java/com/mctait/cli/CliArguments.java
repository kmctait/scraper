package com.mctait.cli;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed command line options for a scraper run.
 *
 * <p>Values may be supplied either inline ({@code --keywords=Python Developer}) or as one or
 * more tokens following the flag ({@code --keywords Python Developer}). Every token up to the
 * next flag is consumed, so unquoted multi-word values are no longer silently truncated.
 *
 * @param keywords       job title/description keywords
 * @param location       UK location to search in
 * @param outputLocation optional override for the output directory
 * @param help           whether usage help was requested
 */
public record CliArguments(String keywords, String location, String outputLocation, boolean help) {

    public static final String DEFAULT_KEYWORDS = "Java Developer";
    public static final String DEFAULT_LOCATION = "London";

    private static final String KEYWORDS = "--keywords";
    private static final String LOCATION = "--location";
    private static final String OUTPUT_LOCATION = "--output-location";
    private static final String HELP = "--help";
    private static final String HELP_SHORT = "-h";

    private static final String USAGE = """

            Usage: java -jar scraper-1.0-SNAPSHOT.jar [options]

              --keywords <text>          Job title or description keywords
                                         (default: %s)
              --location <text>          City or region in the UK
                                         (default: %s)
              --output-location <path>   Directory for the generated HTML files
                                         (default: output.base-path)
              -h, --help                 Show this message and exit

            Values may use '=' or be supplied as the tokens that follow the flag:
              --keywords Python Developer --location Bristol
            """.formatted(DEFAULT_KEYWORDS, DEFAULT_LOCATION);

    public CliArguments {
        keywords = keywords == null || keywords.isBlank() ? DEFAULT_KEYWORDS : keywords;
        location = location == null || location.isBlank() ? DEFAULT_LOCATION : location;
    }

    public static String usage() {
        return USAGE;
    }

    /**
     * @throws IllegalArgumentException if the arguments are malformed
     */
    public static CliArguments parse(String[] args) {
        String keywords = DEFAULT_KEYWORDS;
        String location = DEFAULT_LOCATION;
        String outputLocation = null;
        boolean help = false;

        int index = 0;
        while (index < args.length) {
            String arg = args[index];

            if (HELP_SHORT.equals(arg)) {
                help = true;
                index++;
                continue;
            }

            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException(
                        "Unexpected argument '" + arg + "'; values must follow a flag such as --keywords");
            }

            int separator = arg.indexOf('=');
            String option = separator >= 0 ? arg.substring(0, separator) : arg;
            String inlineValue = separator >= 0 ? arg.substring(separator + 1) : null;

            if (HELP.equals(option)) {
                help = true;
                index++;
                continue;
            }

            if (!KEYWORDS.equals(option) && !LOCATION.equals(option) && !OUTPUT_LOCATION.equals(option)) {
                if (isFrameworkArgument(option)) {
                    index += (inlineValue == null && index + 1 < args.length && !args[index + 1].startsWith("--"))
                            ? 2
                            : 1;
                    continue;
                }
                throw new IllegalArgumentException("Unknown option '" + option + "'");
            }

            List<String> tokens = new ArrayList<>();
            if (inlineValue != null) {
                tokens.add(inlineValue);
            }
            int next = index + 1;
            while (next < args.length && !args[next].startsWith("-")) {
                tokens.add(args[next]);
                next++;
            }

            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("Missing value for " + option);
            }

            String value = String.join(" ", tokens).trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Blank value for " + option);
            }

            switch (option) {
                case KEYWORDS -> keywords = value;
                case LOCATION -> location = value;
                default -> outputLocation = value;
            }

            index = next;
        }

        return new CliArguments(keywords, location, outputLocation, help);
    }

    /**
     * Spring Boot's own options (for example {@code --spring.profiles.active}) are left to the
     * framework instead of being reported as unknown scraper options.
     */
    private static boolean isFrameworkArgument(String option) {
        return option.startsWith("--spring.")
                || option.startsWith("--logging.")
                || option.startsWith("--server.")
                || option.startsWith("--management.")
                || "--debug".equals(option)
                || "--trace".equals(option);
    }
}
