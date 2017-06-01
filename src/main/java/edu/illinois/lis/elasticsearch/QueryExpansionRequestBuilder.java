package edu.illinois.lis.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.Client;

public class QueryExpansionRequestBuilder extends ActionRequestBuilder<QueryExpansionRequest, QueryExpansionResponse, QueryExpansionRequestBuilder, Client> {

    public QueryExpansionRequestBuilder(Client client) {
        super(client, new QueryExpansionRequest());
    }

	@Override
	protected void doExecute(ActionListener<QueryExpansionResponse> listener) {
        client.execute(QueryExpansionAction.INSTANCE, request, listener);
	}

}
