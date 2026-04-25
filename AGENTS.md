# AGENTS.md

## Build & Run Commands

```bash
./gradlew build          # Build JAR
./gradlew bootRun        # Run with defaults (Java Developer in London)
./gradlew bootRun --args="--keywords 'Python Developer' --location 'Manchester'"
./gradlew test           # Run tests
```

## Project Structure

Single-module Gradle project (Java 21, Spring Boot 3.2.0):
- Entry: `src/main/java/com/mctait/Main.java`
- Scrapers: `src/main/java/com/mctait/scraper/` (AdzunaScraper, ReedScraper)
- Service: `src/main/java/com/mctait/service/JobAggregatorService.java`
- Config: `src/main/resources/application.properties`

## Configuration

`application.properties` contains API keys for both Adzuna and Reed (already configured). Output directory is `/mnt/c/kmt/cvs/CV2026/scrapedJobs`.

## Server

Runs on port 8086 by default.