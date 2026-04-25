# Implementation Plan - UK Job Scraper

## Iteration 1 - Foundation

### 1. Project Setup
- [x] Upgrade Java version from 17 to **21** in `build.gradle`
- [x] Add dependencies: `jsoup` (HTML parsing), `OkHttp` (HTTP client)

### 2. Core Domain Model
Create `JobPosting.java` in `src/main/java/com/mctait/model/`:
- Fields: title, companyName, location, salary, summary, url, sourceSite

### 3. Scraper Interface
Create `JobScraper.java` in `src/main/java/com/mctait/scraper/`:
```java
public interface JobScraper {
    List<JobPosting> scrape(String keywords, String location);
    String getSourceName();
}
```

### 4. Indeed Scraper Implementation
Create `IndeedScraper.java`:
- Fetch `https://uk.indeed.com/jobs?q={keywords}&l={location}` using JSoup
- Parse job cards, extract job URLs
- Anti-scraping: User-Agent header, rate limiting

### 5. Aggregator Service
Create `JobAggregatorService.java`:
- Inject all `JobScraper` implementations
- Aggregate results, apply filters

### 6. CLI Runner
- Accept args: `--keywords "Java Developer" --location "London"`
- Output results to console

### 7. Verification
- Test with "Java Developer" + "London"

> **Note:** Indeed blocks automated requests (HTTP 403). For production use, consider:
> - Selenium/Playwright for browser automation
> - Using job board APIs
> - Proxy rotation services

---

**Future:** Greenhouse scraper, file output (JSON/CSV), REST API, Selenium/Playwright for JS-rendered sites.
