package com.mctait.service;

import com.mctait.model.JobPosting;

import java.util.List;

/**
 * Outcome of an aggregation run. Carries the jobs that were found together with the sources
 * that failed, so callers can tell "nothing matched" apart from "the run was broken".
 *
 * @param jobs          all job postings collected from the sources that succeeded
 * @param failedSources names of the sources that could not be scraped
 */
public record AggregationResult(List<JobPosting> jobs, List<String> failedSources) {

    public AggregationResult {
        jobs = List.copyOf(jobs);
        failedSources = List.copyOf(failedSources);
    }

    public boolean hasFailures() {
        return !failedSources.isEmpty();
    }
}
