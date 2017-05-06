package org.nationaldataservice.elasticsearch.queryexpansion;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class HelloRequestBuilder extends ActionRequestBuilder<HelloRequest, HelloResponse, HelloRequestBuilder> {

    public HelloRequestBuilder(ElasticsearchClient client, Action<HelloRequest, HelloResponse, HelloRequestBuilder> action) {
        super(client, action, new HelloRequest());
    }

}
