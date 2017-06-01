package edu.illinois.lis.elasticsearch;

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.client.Client;

public class QueryExpansionAction extends ClientAction<QueryExpansionRequest, QueryExpansionResponse, QueryExpansionRequestBuilder> {

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
	public QueryExpansionRequestBuilder newRequestBuilder(Client elasticsearchClient) {
        return new QueryExpansionRequestBuilder(elasticsearchClient);
	}
}
