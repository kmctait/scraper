# AGENTS.md

## Build & Run Commands

```bash
./gradlew build          # Build JAR
./gradlew bootRun        # Run with defaults (Java Developer in London)
./gradlew bootRun --args="--keywords Python Developer --location Manchester"
./gradlew test           # Run tests (offline, no API calls)
```

Exit codes: `0` success, `1` a scraper failed, `2` invalid arguments.

## Project Structure

Single-module Gradle project (Java 21, Spring Boot 3.2.0):
- Entry: `src/main/java/com/mctait/Main.java`
- CLI: `src/main/java/com/mctait/cli/` (CliArguments, JobSearchRunner)
- Config: `src/main/java/com/mctait/config/` (HttpClientConfig, ScrapingProperties)
- Scrapers: `src/main/java/com/mctait/scraper/` (AbstractJobScraper, AdzunaScraper, ReedScraper)
- Service: `src/main/java/com/mctait/service/` (JobAggregatorService, AggregationResult)
- Settings: `src/main/resources/application.properties` (untracked; see `application.properties.example`)

## Configuration

`application.properties` is git-ignored because it holds API keys. Create it with
`cp src/main/resources/application.properties.example src/main/resources/application.properties`,
or set `REED_API_KEY`, `ADZUNA_APP_ID` and `ADZUNA_APP_KEY` (plus optional `OUTPUT_BASE_PATH`).
Output goes to `<output.base-path>/<Source>/*.html` (default `scrapedJobs` relative to the working
directory).

## Application Type

This is a CLI application, not a web server: no `spring-boot-starter-web`, no embedded Tomcat and
no listening port.
