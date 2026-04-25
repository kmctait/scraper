package com.mctait.model;

public class JobPosting {
    private String title;
    private String companyName;
    private String location;
    private String salary;
    private String summary;
    private String url;
    private String sourceSite;

    public JobPosting() {}

    public JobPosting(String title, String companyName, String location, String salary, String summary, String url, String sourceSite) {
        this.title = title;
        this.companyName = companyName;
        this.location = location;
        this.salary = salary;
        this.summary = summary;
        this.url = url;
        this.sourceSite = sourceSite;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSourceSite() { return sourceSite; }
    public void setSourceSite(String sourceSite) { this.sourceSite = sourceSite; }

    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s, %s, %s",
                title != null ? title : "N/A",
                companyName != null ? companyName : "N/A",
                location != null ? location : "N/A",
                salary != null ? salary : "N/A",
                summary != null ? summary : "N/A",
                url != null ? url : "N/A");
    }
}
