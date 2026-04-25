# UK Job Scraper

A Spring Boot application for scraping job listings from UK job boards (Adzuna and Reed).

## Prerequisites

- Java 21
- Gradle 8.x

## Building

```bash
./gradlew build
```

## Running

### Basic Usage

```bash
./gradlew bootRun
```

This will search for "Java Developer" jobs in "London" by default.

### With Custom Search Parameters

```bash
./gradlew bootRun --args="--keywords 'Software Engineer' --location 'Manchester'"
```

#### CLI Arguments

| Argument            | Description                      | Default             |
|--------------------|----------------------------------|--------------------|
| `--keywords`       | Job title or description keywords | Java Developer     |
| `--location`       | City or region in the UK           | London             |
| `--output-location`| Custom output directory path     | (from properties)  |

Arguments can be used with `=` or space-separated:
```bash
./gradlew bootRun --args="--keywords=Python Developer --location=Bristol"
./gradlew bootRun --args="--keywords Python Developer --location Bristol"
```

### Custom Output Location

Override the output directory:
```bash
./gradlew bootRun --args="--keywords 'Java Developer' --output-location '/custom/path/jobs'"
```

### Running the JAR

```bash
java -jar build/libs/scraper-1.0-SNAPSHOT.jar --keywords "Java Developer" --location "London"
```

## Configuration

All configuration is in `src/main/resources/application.properties`:

```properties
# Server
server.port=8086

# Output directory for scraped job files
output.base-path=/mnt/c/kmt/cvs/CV2026/scrapedJobs

# Reed API (https://www.reed.co.uk/api/1.0/search)
reed.api.key=YOUR_API_KEY_HERE
reed.api.base-url=https://www.reed.co.uk/api/1.0/search

# Adzuna API (http://api.adzuna.com/v1/api/jobs/gb/search/1)
adzuna.api.base-url=http://api.adzuna.com/v1/api/jobs/gb/search/1
adzuna.api.app-id=YOUR_APP_ID
adzuna.api.app-key=YOUR_APP_KEY
```

### Properties

| Property                | Description                              |
|------------------------|------------------------------------------|
| `server.port`          | HTTP server port                          |
| `output.base-path`      | Base directory for JSON output files     |
| `reed.api.key`         | Reed API key                             |
| `reed.api.base-url`     | Reed API endpoint                        |
| `adzuna.api.base-url`  | Adzuna API endpoint                    |
| `adzuna.api.app-id`    | Adzuna app ID                         |
| `adzuna.api.app-key`  | Adzuna app key                        |

## Output

Job listings are written to JSON files in the output directory:

```
output.base-path/
├── Adzuna/
│   └── adzuna_Java_Developer_London.json
└── Reed/
    └── reed_Java_Developer_London.json
```

Each JSON file contains an array of job postings with:
- title
- companyName
- location
- salary
- summary (description snippet)
- url (job posting URL)
- source ("Adzuna" or "Reed")

## Project Structure

```
src/main/java/com/mctait/
├── Main.java                    # Application entry point
├── model/
│   └── JobPosting.java          # Job data model
├── scraper/
│   ├── JobScraper.java         # Scraper interface
│   ├── AdzunaScraper.java      # Adzuna implementation
│   └── ReedScraper.java        # Reed implementation
└── service/
    └── JobAggregatorService.java # Aggregates results from scrapers
```

## Adding New Scrapers

1. Implement the `JobScraper` interface with `@Component`:

```java
@Component
public class ExampleScraper implements JobScraper {
    
    public ExampleScraper(
            @Value("${example.api.key}") String apiKey,
            @Value("${output.base-path}") String outputBasePath) {
        // ...
    }
    
    @Override
    public List<JobPosting> scrape(String keywords, String location, String outputLocation) {
        // Your scraping logic here
    }
    
    @Override
    public String getSourceName() {
        return "Example";
    }
}
```

2. Add properties to `application.properties`:

```properties
example.api.key=YOUR_KEY
example.api.base-url=https://api.example.com
```

3. Spring will auto-wire it in `JobAggregatorService`.