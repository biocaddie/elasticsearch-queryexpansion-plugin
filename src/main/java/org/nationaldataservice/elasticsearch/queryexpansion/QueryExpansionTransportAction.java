package org.nationaldataservice.elasticsearch.queryexpansion;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class QueryExpansionTransportAction extends HandledTransportAction<QueryExpansionRequest, QueryExpansionResponse> {
	private final Logger logger = ESLoggerFactory.getLogger(QueryExpansionTransportAction.class);

    @Inject
    public QueryExpansionTransportAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                IndexNameExpressionResolver resolver, TransportService transportService) {
        super(settings, QueryExpansionAction.NAME, threadPool, transportService, actionFilters, resolver, QueryExpansionRequest::new);
    }
    
    @Override
    protected void doExecute(QueryExpansionRequest request, ActionListener<QueryExpansionResponse> listener) {
    	this.logger.info("Executing transport action!");
    	
        try {
            String name = request.getName();
            if (name == null) {
                name = "World";
            }
            QueryExpansionResponse response = new QueryExpansionResponse();
            response.setMessage("Hello " + name + "!");
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
