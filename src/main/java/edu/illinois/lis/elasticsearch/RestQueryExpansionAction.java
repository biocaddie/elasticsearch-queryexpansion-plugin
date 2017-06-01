package edu.illinois.lis.elasticsearch;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.io.IOException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

public class RestQueryExpansionAction extends BaseRestHandler {
	private final ESLogger logger = ESLoggerFactory.getLogger(RestQueryExpansionAction.class.toString());
	
    @Inject   
	public RestQueryExpansionAction (Settings settings, Client client, RestController controller) {
		super(settings, controller,client);
		this.logger.info("Plugin loaded!");
		
		controller.registerHandler(GET, "/_hello", this);
		controller.registerHandler(GET, "/_hello/{name}", this);
	}
    
    @Override
    protected void handleRequest(final RestRequest restRequest, final RestChannel channel, final Client client) throws IOException {
        QueryExpansionRequest request = new QueryExpansionRequest();
		this.logger.info("Preparing request!");
		
		String name = restRequest.param("name");
	    
        if (name != null) {
            request.setName(name);
        } else if (restRequest.hasContent()){
            request.setRestContent(restRequest.content());
        }

        QueryExpansionRequest actionRequest = new QueryExpansionRequest();
        
        client.execute(QueryExpansionAction.INSTANCE, actionRequest, new ActionListener<QueryExpansionResponse>() {
        	private ESLogger logger = ESLoggerFactory.getLogger("QueryExpansionActionListener");
        	
			@Override
			public void onFailure(Throwable e) {
				this.logger.error("Sending error:", e);
		        try {
					channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, channel.newBuilder()));
				} catch (IOException innerException) {
					this.logger.error("I/O error:", innerException);
				}
			}

			@Override
			public void onResponse(QueryExpansionResponse response) {
				this.logger.info("Sending response: " + response.getMessage());
		        XContentBuilder builder;
				try {
					builder = channel.newBuilder();
			        builder.startObject();
			        response.toXContent(builder, restRequest);
			        builder.endObject();
			        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
				} catch (IOException e) {
					onFailure(e);
				}
			}
        	
        });
    }

	class Message implements ToXContent {
	
	    private final String name;
	
	    public Message(String name) {
	        if (name == null) {
	            this.name = "World";
	        } else {
	            this.name = name;
	        }
	    }
	
	    @Override
	    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
	        return builder.field("message", "Hello " + name + "!");
	    }
	}
}
