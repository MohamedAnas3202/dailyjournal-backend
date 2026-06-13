package com.dailyjournal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String username;

    @Value("${spring.datasource.password:#{null}}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = databaseUrl;
        String parsedUser = username;
        String parsedPassword = password;

        if (url != null) {
            url = url.trim();

            // Format 1: starts with // (e.g. //gateway01...)
            if (url.startsWith("//")) {
                url = "jdbc:mysql:" + url;
            }
            // Format 2: completely schema-less host:port/db (e.g. gateway01...)
            else if (!url.startsWith("jdbc:") && !url.contains("://")) {
                url = "jdbc:mysql://" + url;
            }

            String cleanUrl = url;
            boolean isJdbcPrefixed = url.startsWith("jdbc:");
            if (isJdbcPrefixed) {
                cleanUrl = url.substring(5); // Strip "jdbc:" prefix to parse standard URI structure
            }

            if (cleanUrl.startsWith("mysql://") || cleanUrl.startsWith("postgresql://")) {
                try {
                    URI uri = new URI(cleanUrl);
                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        parsedUser = parts[0];
                        parsedPassword = parts[1];
                    }

                    String host = uri.getHost();
                    int port = uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();
                    String scheme = uri.getScheme();

                    if ("mysql".equalsIgnoreCase(scheme)) {
                        if (query == null) {
                            query = "createDatabaseIfNotExist=true";
                        } else if (!query.contains("createDatabaseIfNotExist")) {
                            query = query + "&createDatabaseIfNotExist=true";
                        }
                    }

                    StringBuilder jdbcUrl = new StringBuilder();
                    jdbcUrl.append("jdbc:").append(scheme).append("://").append(host);
                    if (port != -1) {
                        jdbcUrl.append(":").append(port);
                    }
                    if (path != null) {
                        jdbcUrl.append(path);
                    }
                    if (query != null) {
                        jdbcUrl.append("?").append(query);
                    }
                    url = jdbcUrl.toString();
                    logger.info("Successfully parsed database URI and converted to clean JDBC URL format.");
                } catch (URISyntaxException e) {
                    logger.warn("Failed to parse database URI.", e);
                    if (!isJdbcPrefixed) {
                        url = "jdbc:" + url;
                    }
                }
            } else if (url.startsWith("mysql:") && !url.startsWith("jdbc:")) {
                url = "jdbc:" + url;
            }
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(parsedUser)
                .password(parsedPassword)
                .build();
    }
}
