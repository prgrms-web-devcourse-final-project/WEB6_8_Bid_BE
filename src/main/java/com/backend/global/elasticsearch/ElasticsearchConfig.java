package com.backend.global.elasticsearch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://elasticsearch_1:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:elastic}")
    private String username;

    @Value("${spring.elasticsearch.password:1234}")
    private String password;

    @Override
    @NonNull
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.TerminalClientConfigurationBuilder builder =
                ClientConfiguration.builder()
                        .connectedTo(elasticsearchUri.replace("http://", "").replace("https://", ""))
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withSocketTimeout(Duration.ofSeconds(30));

        // username과 password가 있으면 Basic Auth 설정
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}