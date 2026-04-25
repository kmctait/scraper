package com.mctait.scraper;

import com.mctait.model.JobPosting;
import java.util.List;

public interface JobScraper {
    List<JobPosting> scrape(String keywords, String location, String outputLocation);
    String getSourceName();
}
