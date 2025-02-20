package com.payment.wallet.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;

public class ESRestClientConfig {
    @Bean
    public RestClient restClient(){
        return RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient){
        return new ElasticsearchClient(
            new RestClientTransport(restClient, new JacksonJsonpMapper())
        );
    }
}
