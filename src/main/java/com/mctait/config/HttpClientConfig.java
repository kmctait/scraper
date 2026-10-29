package com.mctait.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Provides the single {@link OkHttpClient} shared by every scraper so that one dispatcher,
 * connection pool and thread pool is used for the whole application.
 */
@Configuration
public class HttpClientConfig {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);
    static final Duration CALL_TIMEOUT = Duration.ofSeconds(60);

    private static final String USER_AGENT = "UKJobScraper/1.0 (+https://github.com/kmctait/scraper)";

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .writeTimeout(WRITE_TIMEOUT)
                .callTimeout(CALL_TIMEOUT)
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()))
                .build();
    }
}
