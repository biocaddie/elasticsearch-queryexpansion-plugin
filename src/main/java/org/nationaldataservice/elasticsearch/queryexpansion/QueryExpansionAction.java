package org.nationaldataservice.elasticsearch.queryexpansion;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

public class QueryExpansionAction extends Action<QueryExpansionRequest, QueryExpansionResponse, QueryExpansionRequestBuilder> {

    public static final QueryExpansionAction INSTANCE = new QueryExpansionAction();
    public static final String NAME = "cluster:admin/hello";

    private QueryExpansionAction() {
        super(NAME);
    }

    @Override
    public QueryExpansionResponse newResponse() {
        return new QueryExpansionResponse();
    }

    @Override
    public QueryExpansionRequestBuilder newRequestBuilder(ElasticsearchClient elasticsearchClient) {
        return new QueryExpansionRequestBuilder(elasticsearchClient, INSTANCE);
    }
}
