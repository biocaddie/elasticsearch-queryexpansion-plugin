package edu.illinois.lis.elasticsearch;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.BaseTransportRequestHandler;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportService;

public class QueryExpansionTransportAction extends TransportAction<QueryExpansionRequest, QueryExpansionResponse> {
	private final ESLogger logger = ESLoggerFactory.getLogger(QueryExpansionTransportAction.class.toString());

	@Inject
    protected QueryExpansionTransportAction(Settings settings, ThreadPool threadPool,
            TransportService transportService,
            ActionFilters actionFilters) {
        super(settings, QueryExpansionAction.NAME, threadPool, actionFilters);
        transportService.registerHandler(QueryExpansionAction.NAME, new TransportHandler());
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
    
    private final class TransportHandler extends BaseTransportRequestHandler<QueryExpansionRequest> {
        @Override
        public QueryExpansionRequest newInstance() {
            return new QueryExpansionRequest();
        }

        @Override
        public void messageReceived(final QueryExpansionRequest request, 
                                    final TransportChannel channel) throws Exception {
            request.listenerThreaded(false);
            execute(request, new ActionListener<QueryExpansionResponse>() {
                @Override
                public void onResponse(QueryExpansionResponse response) {
                    try {
                        channel.sendResponse(response);
                    } catch (Exception e) {
                        onFailure(e);
                    }
                }

                @Override
                public void onFailure(Throwable e) {
                    try {
                        channel.sendResponse(e);
                    } catch (Exception e1) {
                        logger.warn("Failed to send error response for action [queryexpansion] and request [" + request + "]", e1);
                    }
                }
            });
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }
}
