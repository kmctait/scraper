# Overcoming HTTP 403 Errors in Web Scraping

## Why You're Getting 403 Errors

Websites like Greenhouse block automated requests because:
1. **Bot detection** - They identify non-browser clients via headers, behavior patterns, and JavaScript execution
2. **WAF (Web Application Firewall)** - Services like Cloudflare detect scrapers
3. **IP reputation** - Automated patterns get flagged

## Solutions (In Order of Effectiveness)

### 1. Browser Automation (Recommended for Greenhouse)

Use Selenium or Playwright to control a real browser. This renders JavaScript and passes bot detection.

**Playwright** (recommended - faster, more modern):
```java
// Add to build.gradle
implementation "com.microsoft.playwright:playwright:1.40.0"

// Example usage
Playwright playwright = Playwright.create();
Browser browser = playwright.chromium().launch();
Page page = browser.newPage();
page.navigate("https://boards.greenhouse.io/...");
String content = page.content();
```

**Selenium** (more established):
```java
// Add to build.gradle
implementation "org.seleniumhq.selenium:selenium-chrome-driver:4.16.0"

WebDriver driver = new ChromeDriver();
driver.get("https://boards.greenhouse.io/...");
```

### 2. Enhanced HTTP Headers + Rotation

Your current approach is close, but add more headers and rotate User-Agents:

```java
private static final String[] USER_AGENTS = {
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36...",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36...",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36..."
};

private static final Random random = new Random();

private String getRandomUserAgent() {
    return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
}

// Add to connection:
.referrer("https://www.google.com/")
.cookie("session_id", "...")
.header("Origin", "https://example.com")
```

### 3. Use Proxies

Rotate IPs to avoid bans:
```java
System.setProperty("http.proxyHost", "proxyhost");
System.setProperty("http.proxyPort", "8080");
```

Free proxy lists are unreliable; consider services like:
- ScraperAPI
- Crawlera
- Oxylabs

### 4. Respect Rate Limits

Add random delays between requests:
```java
Thread.sleep(2000 + random.nextInt(3000)); // 2-5 seconds
```

### 5. Greenhouse-Specific: Use Their Public API

Greenhouse uses `jobboard.io` which has a standard endpoint:
```
https://boards.greenhouse.io/{company_handle}/jobs?content=true
```

Many companies expose job data via:
```
https://boards-api.greenhouse.io/v1/boards/{company_handle}/jobs
```

Check `https://boards.greenhouse.io/embed/job_card?url={job_url}` for job data.

## Recommended Architecture for Your Project

```
┌─────────────────────────────────────────┐
│           JobAggregatorService          │
└─────────────────────────────────────────┘
          │              │
          ▼              ▼
┌─────────────────┐  ┌─────────────────────┐
│  IndeedScraper   │  │ GreenhouseScraper   │
│  (JSoup +       │  │ (Playwright/        │
│   headers)       │  │  Selenium)          │
└─────────────────┘  └─────────────────────┘
```

## Quick Wins for Indeed (Current Implementation)

Your Indeed scraper is close. Add:
1. Rotate User-Agents
2. Add `referrer` header pointing to Google or a search page
3. Add `Sec-Ch-Ua` headers for Chrome 120+
4. Use cookies from a real browser session

## Files to Modify

1. `IndeedScraper.java` - Add User-Agent rotation, more headers
2. Create `GreenhouseScraper.java` - Use Playwright
3. Update `build.gradle` - Add Playwright/Selenium dependency
4. Update `JobAggregatorService.java` - Register GreenhouseScraper

## External Resources

- [Playwright Java Docs](https://playwright.dev/java/)
- [Selenium WebDriver](https://www.selenium.dev/documentation/webdriver/)
- [Greenhouse Job Board API](https://developers.greenhouse.io/job-board.html)
