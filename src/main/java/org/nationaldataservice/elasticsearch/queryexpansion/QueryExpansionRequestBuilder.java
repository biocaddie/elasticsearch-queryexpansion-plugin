package org.nationaldataservice.elasticsearch.queryexpansion;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class QueryExpansionRequestBuilder extends ActionRequestBuilder<QueryExpansionRequest, QueryExpansionResponse, QueryExpansionRequestBuilder> {

    public QueryExpansionRequestBuilder(ElasticsearchClient client, Action<QueryExpansionRequest, QueryExpansionResponse, QueryExpansionRequestBuilder> action) {
        super(client, action, new QueryExpansionRequest());
    }

}
