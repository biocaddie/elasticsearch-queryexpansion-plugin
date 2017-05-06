package org.nationaldataservice.elasticsearch.queryexpansion;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

public class RestQueryExpansionAction extends BaseRestHandler {
	private final Logger logger = ESLoggerFactory.getLogger(RestQueryExpansionAction.class);
	
    @Inject   
	public RestQueryExpansionAction (Settings settings, RestController controller) {
		super(settings);
		this.logger.fatal("Plugin loaded!");
		controller.registerHandler(GET, "/_hello", this);
		controller.registerHandler(GET, "/_hello/{name}", this);
	}
    
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        HelloRequest request = new HelloRequest();
		this.logger.fatal("Preparing request!");

        String name = restRequest.param("name");
        if (name != null) {
            request.setName(name);
        } else if (restRequest.hasContent()){
            request.setRestContent(restRequest.content());
        }

        return channel -> client.execute(HelloAction.INSTANCE, request, new ActionListener<HelloResponse>() {
        	private final Logger logger = ESLoggerFactory.getLogger(HelloResponse.class);
        	
			@Override
			public void onResponse(HelloResponse response) {
				System.out.println(response.toString());
				this.logger.fatal("Preparing request!");
			}

			@Override
			public void onFailure(Exception e) {
				System.err.println(e);
				this.logger.fatal("Response: error!");
			}
        });
    }
}
