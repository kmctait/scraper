# Description of the Scraper project

## Summary

This project is supposed to be a web scraper specifically for Job adverts in the UK, in particular, for Java and Software
developer related jobs in the UK only.

This project is to implement a number of individual scrapers, each targeted at a specific company careers webpage or a 
job scraper site.

Each scraper will make send a HTTP request to each site and look for Job adverts. Given criteria such as 'Java developer', 
and a Location, such as 'London', the scraper will find job adverts and return them.

In a first iteration of the project, write the output to a flat file, HTML file, or just to console.

## Architecture

The project, termed a scraper, will actually be a collection of individual scrapers.
Each individual scraper will be targeted to a specific company careers website or job scraper/aggregator site.
A per-site scraper class will be required.

A modular architecture is therefore required.

### Scraper Interface

interface JobScraper {
    List<JobPosting> scrape();
}

### Per-site scraper implementations
Each scraper to handle one site:

class GreenhouseScraper implements JobScraper { }
class CompanyXScraper implements JobScraper { }

### Aggregator

List<JobPosting> allJobs = new ArrayList<>();

for (JobScraper scraper : scrapers) {
    allJobs.addAll(scraper.scrape());
}

### Filter layer

Filter on job description/title and location keywords:

jobs.stream()
.filter(job -> job.title.contains("Java"))
.filter(job -> job.location.contains("London"))

## Tech stack

- Spring boot application in Java 21
- Can use HttpClient or a suitable library such as OkHttp for fetching website information
- Will need a HTML parser such as JSoup or something more modern to parse the HTML
- Selenium
- Playwright

## Anti-scraping strategies

Handle the following:
- rate limit requests
- Add headers (User-Agent)
- Respect robots.txt

Also know that some sites split job listings across pages e.g.
/jobs?page=1
/jobs?page=2

## Data to extract, store and send back

- Job Title
- Company Name
- Location
- Salary
- Summary of job and duties
- Link back to job advert

A data structure for the above attributes will be required.

## Example websites to scrape
### Job scrapers or job aggregators

- https://www.greenhouse.com/careers
- https://uk.indeed.com/
- www.workday.com

## Searching

The following criteria are required for searching:
- Job title/description keywords e.g. 'Java developer'
- Location e.g. :ondon

## Workflow

1. Fetch careers page of a given company website
2. Parse job cards
3. Extract all the relevant job information into the class as described in section 'Data to extract, store and send back'
4. Filter on info such as job title/description and location
5. Write the collection of job ads retrieved to a file or console window eg:
   "Senior Java Developer, Acme company Ltd, London, 80k - 100k, https://company.com/careers/jobs/123",
"This job involves converting customer requirements into Java code..."