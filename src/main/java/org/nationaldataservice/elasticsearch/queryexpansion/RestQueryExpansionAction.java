package org.nationaldataservice.elasticsearch.queryexpansion;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.io.IOException;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

public class RestQueryExpansionAction extends BaseRestHandler {
    @Inject   
	public RestQueryExpansionAction (Settings settings, RestController controller) {
		super(settings);    
		controller.registerHandler(GET, "/_hello", this);
		controller.registerHandler(GET, "/_hello/{name}", this);
	}
    @Override
	protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String clustername = client.settings().get("cluster.name");
        return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.OK, XContentFactory.jsonBuilder().startObject().field("hello", "This is cluster – " + clustername).endObject()));
	}
}
