package com.mctait.cli;

import com.mctait.model.JobPosting;
import com.mctait.service.AggregationResult;
import com.mctait.service.JobAggregatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSearchRunnerTest {

    private static final JobPosting JOB =
            new JobPosting("Java Dev", "Company A", "London", "£50k", "Summary", "https://example.com", "Adzuna");

    @Mock
    private JobAggregatorService aggregatorService;

    @Test
    void run_withSuccessfulScrape_exitsZero() {
        when(aggregatorService.aggregateJobs("Java Developer", "London", null))
                .thenReturn(new AggregationResult(List.of(JOB), List.of()));

        JobSearchRunner runner = new JobSearchRunner(aggregatorService);
        runner.run(new DefaultApplicationArguments());

        assertEquals(JobSearchRunner.EXIT_SUCCESS, runner.getExitCode());
    }

    @Test
    void run_withFailedSource_exitsNonZero() {
        when(aggregatorService.aggregateJobs("Java Developer", "London", null))
                .thenReturn(new AggregationResult(List.of(), List.of("Reed")));

        JobSearchRunner runner = new JobSearchRunner(aggregatorService);
        runner.run(new DefaultApplicationArguments());

        assertEquals(JobSearchRunner.EXIT_SCRAPE_FAILED, runner.getExitCode());
    }

    @Test
    void run_withInvalidArguments_exitsUsageErrorWithoutScraping() {
        JobSearchRunner runner = new JobSearchRunner(aggregatorService);
        runner.run(new DefaultApplicationArguments("--keywords"));

        assertEquals(JobSearchRunner.EXIT_USAGE_ERROR, runner.getExitCode());
        verify(aggregatorService, never()).aggregateJobs(any(), any(), any());
    }

    @Test
    void run_withHelpArgument_doesNotScrape() {
        JobSearchRunner runner = new JobSearchRunner(aggregatorService);
        runner.run(new DefaultApplicationArguments("--help"));

        assertEquals(JobSearchRunner.EXIT_SUCCESS, runner.getExitCode());
        verify(aggregatorService, never()).aggregateJobs(any(), any(), any());
    }

    @Test
    void run_withMultiWordArguments_passesWholeValueToAggregator() {
        when(aggregatorService.aggregateJobs("Python Developer", "Milton Keynes", null))
                .thenReturn(new AggregationResult(List.of(), List.of()));

        JobSearchRunner runner = new JobSearchRunner(aggregatorService);
        runner.run(new DefaultApplicationArguments(
                "--keywords", "Python", "Developer", "--location", "Milton", "Keynes"));

        assertEquals(JobSearchRunner.EXIT_SUCCESS, runner.getExitCode());
        verify(aggregatorService).aggregateJobs("Python Developer", "Milton Keynes", null);
    }
}
