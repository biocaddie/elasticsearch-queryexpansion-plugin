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

public class HelloTransportAction extends HandledTransportAction<HelloRequest, HelloResponse> {
	private final Logger logger = ESLoggerFactory.getLogger(HelloTransportAction.class);

    @Inject
    public HelloTransportAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                IndexNameExpressionResolver resolver, TransportService transportService) {
        super(settings, HelloAction.NAME, threadPool, transportService, actionFilters, resolver, HelloRequest::new);
    }
    
    @Override
    protected void doExecute(HelloRequest request, ActionListener<HelloResponse> listener) {
    	this.logger.info("Executing transport action!");
    	
        try {
            String name = request.getName();
            if (name == null) {
                name = "World";
            }
            HelloResponse response = new HelloResponse();
            response.setMessage("Hello " + name + "!");
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
