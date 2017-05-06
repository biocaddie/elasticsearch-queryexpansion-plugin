package org.nationaldataservice.elasticsearch.queryexpansion;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

public class HelloAction extends Action<HelloRequest, HelloResponse, HelloRequestBuilder> {

    public static final HelloAction INSTANCE = new HelloAction();
    public static final String NAME = "cluster:admin/hello";

    private HelloAction() {
        super(NAME);
    }

    @Override
    public HelloResponse newResponse() {
        return new HelloResponse();
    }

    @Override
    public HelloRequestBuilder newRequestBuilder(ElasticsearchClient elasticsearchClient) {
        return new HelloRequestBuilder(elasticsearchClient, INSTANCE);
    }
}
