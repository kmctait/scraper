package com.mctait.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {
    private String title;
    private String companyName;
    private String location;
    private String salary;
    private String summary;
    private String url;
    private String sourceSite;
}