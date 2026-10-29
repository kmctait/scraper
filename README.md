# UK Job Scraper

A Spring Boot application for scraping UK job listings from the Adzuna and Reed job boards and
writing each source's results to a standalone HTML report.

## Prerequisites

- Java 21
- Gradle 8.x
- API credentials for [Reed](https://www.reed.co.uk/developers) and
  [Adzuna](https://developer.adzuna.com)

## Configuration

Credentials must never be committed. The recommended setup keeps the properties file **outside**
the repository and loads it on top of the packaged defaults:

```bash
mkdir -p ~/.config/jobscraper
cp src/main/resources/application.properties.example ~/.config/jobscraper/application.properties
chmod 600 ~/.config/jobscraper/application.properties

# add to ~/.bashrc so every shell picks it up
export SPRING_CONFIG_ADDITIONAL_LOCATION=file:$HOME/.config/jobscraper/application.properties
```

`src/main/resources/application.properties` is git-ignored, so keeping the file in the tree also
works, but every build then copies the secrets into `build/resources/main/`.

Every value can also be supplied through an environment variable, which is the preferred way to
provide secrets in CI or on a shared machine:

| Environment variable        | Property                 | Default |
|-----------------------------|--------------------------|---------|
| `REED_API_KEY`              | `reed.api.key`           | (empty) |
| `REED_API_BASE_URL`         | `reed.api.base-url`      | `https://www.reed.co.uk/api/1.0/search` |
| `ADZUNA_APP_ID`             | `adzuna.api.app-id`      | (empty) |
| `ADZUNA_APP_KEY`            | `adzuna.api.app-key`     | (empty) |
| `ADZUNA_API_BASE_URL`       | `adzuna.api.base-url`    | `https://api.adzuna.com/v1/api/jobs/gb/search` |
| `OUTPUT_BASE_PATH`          | `output.base-path`       | `scrapedJobs` |
| `SCRAPER_LOG_PATH`          | `logging.file.path`      | `/tmp` |
| `SCRAPING_MAX_PAGES`        | `scraping.max-pages`     | `3` |
| `SCRAPING_RESULTS_PER_PAGE` | `scraping.results-per-page` | `50` |
| `SCRAPING_MAX_RETRIES`      | `scraping.max-retries`   | `2` |
| `SCRAPING_RETRY_BACKOFF`    | `scraping.retry-backoff` | `500ms` |

Notes:

- `adzuna.api.base-url` must **not** contain a page number; the page is appended per request
  (`.../search/1`, `.../search/2`, ...). A trailing page segment is tolerated and replaced.
- Adzuna caps `results_per_page` at 50, so larger `scraping.results-per-page` values are clamped
  for that source.
- Both APIs are called over HTTPS only.
- Pagination stops as soon as a source returns a short page, or when `scraping.max-pages` is
  reached.
- Transient failures (HTTP 429/5xx, I/O errors) are retried with exponential backoff; explicit
  connect/read/write/call timeouts are configured in `HttpClientConfig`.

## Building

```bash
./gradlew build
```

## Running

### Basic Usage

```bash
./gradlew bootRun
```

This searches for "Java Developer" jobs in "London".

### With Custom Search Parameters

```bash
./gradlew bootRun --args="--keywords Software Engineer --location Manchester"
```

### CLI Arguments

| Argument                   | Description                        | Default             |
|----------------------------|------------------------------------|---------------------|
| `--keywords`               | Job title or description keywords  | `Java Developer`    |
| `--location`               | City or region in the UK           | `London`            |
| `--output-location`        | Custom output directory path       | `output.base-path`  |
| `-h`, `--help`             | Print usage and exit               |                     |

Values may use `=` or be passed as the tokens that follow the flag. **All** tokens up to the next
flag belong to the value, so unquoted multi-word values work as expected:

```bash
./gradlew bootRun --args="--keywords=Python Developer --location=Bristol"
./gradlew bootRun --args="--keywords Python Developer --location Bristol"
./gradlew bootRun --args="--keywords 'Java Developer' --output-location '/custom/path/jobs'"
```

Unknown options, missing values and stray positional arguments are rejected with a usage message
instead of being silently ignored.

### Running the JAR

```bash
java -jar build/libs/scraper-1.0-SNAPSHOT.jar --keywords "Java Developer" --location "London"
```

### Exit Codes

| Code | Meaning                                                        |
|------|----------------------------------------------------------------|
| `0`  | Run completed and every source succeeded (possibly 0 results)  |
| `1`  | At least one source failed (HTTP error, bad response, write error, missing credentials) |
| `2`  | Invalid command line arguments                                 |

A broken run no longer looks like an empty search, so scripts and CI can detect failures. Missing
API credentials fail fast — before any request is made — with a message naming the environment
variable to set, rather than surfacing as a 401.

## Output

Each source writes a self-contained HTML report below the configured output directory, in a
sub-directory named after the source:

```
output.base-path/
├── Adzuna/
│   └── adzuna_Java_Developer_London.html
└── Reed/
    └── reed_Java_Developer_London.html
```

File names are derived from the source, keywords and location. Those values are sanitised
(everything but letters, digits, `.`, `_` and `-` becomes `_`, dot segments are stripped, length is
capped) and the resolved path is verified to stay inside the output directory before anything is
written.

Each report lists the job postings with:

- title (linked to the original advert)
- company name
- location
- salary (when provided by the source)
- summary (description snippet, truncated to 200 characters)

## Project Structure

```
src/main/java/com/mctait/
├── Main.java                        # Application entry point (sets the exit code)
├── cli/
│   ├── CliArguments.java            # Command line parsing and usage text
│   └── JobSearchRunner.java         # Runs one scrape, decides the exit code
├── config/
│   ├── HttpClientConfig.java        # Single shared OkHttpClient with timeouts
│   └── ScrapingProperties.java      # Pagination / retry settings
├── model/
│   └── JobPosting.java              # Job data model
├── scraper/
│   ├── JobScraper.java              # Scraper interface
│   ├── AbstractJobScraper.java      # HTTP, retry, pagination, HTML output
│   ├── ScrapeException.java         # Signals a failed scrape
│   ├── AdzunaScraper.java           # Adzuna implementation
│   └── ReedScraper.java             # Reed implementation
└── service/
    ├── AggregationResult.java       # Jobs found + sources that failed
    └── JobAggregatorService.java    # Aggregates results, isolates failures
```

## Adding New Scrapers

1. Extend `AbstractJobScraper` and annotate the class with `@Component`. Implement `buildUrl`,
   `mapJobPosting` and `getSourceName`; the base class provides pagination, retries, timeouts,
   path validation and HTML generation.

```java
@Component
public class ExampleScraper extends AbstractJobScraper {

    public ExampleScraper(
            @Value("${example.api.base-url}") String apiBaseUrl,
            @Value("${example.api.key}") String apiKey,
            @Value("${output.base-path}") String outputBasePath,
            OkHttpClient client,
            ScrapingProperties properties) {
        super(outputBasePath, client, properties);
        // ...
    }

    @Override
    protected String buildUrl(String keywords, String location, int page) {
        // Return the URL for this page; throw or let errors bubble up as ScrapeException
    }

    @Override
    protected JobPosting mapJobPosting(JsonObject jobObj) {
        // Map one API result onto a JobPosting
    }

    @Override
    public String getSourceName() {
        return "Example";
    }
}
```

2. Add properties to `application.properties` (and to `application.properties.example`):

```properties
example.api.key=${EXAMPLE_API_KEY:}
example.api.base-url=https://api.example.com
```

3. Spring auto-wires the new scraper into `JobAggregatorService`, and any failure it reports is
   reflected in the process exit code.

## Tests

```bash
./gradlew test
```

The suite runs entirely offline: `MainTest` boots the application with the `test` profile, which
excludes the CLI runner so no live API calls or file writes happen. Test-only settings live in
`src/test/resources/application-test.properties`.

## License

MIT — see [LICENSE](LICENSE).
