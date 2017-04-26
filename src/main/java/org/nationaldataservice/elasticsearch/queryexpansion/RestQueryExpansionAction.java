package org.nationaldataservice.elasticsearch.queryexpansion;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

public class RestQueryExpansionAction extends BaseRestHandler {
    @Inject   
	public RestQueryExpansionAction (Settings settings, RestController controller) {
		super(settings);    
		controller.registerHandler(GET, "/_ hello", this);
	}

    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
          String clustername = client.settings().get("cluster.name");
          channel.sendResponse(new BytesRestResponse(RestStatus.OK, XContentFactory.jsonBuilder().startObject().field("hello", "This is cluster – " + clustername).endObject()));
    }
}
