package com.mctait.scraper;

/**
 * Signals that a scraper could not complete its work (network failure, unusable response,
 * or an output file that could not be written). Callers use this to distinguish a genuinely
 * empty result set from a broken run.
 */
public class ScrapeException extends RuntimeException {

    public ScrapeException(String message) {
        super(message);
    }

    public ScrapeException(String message, Throwable cause) {
        super(message, cause);
    }
}
