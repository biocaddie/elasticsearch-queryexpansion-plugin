package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import edu.gslis.textrepresentation.FeatureVector;

public class RocchioRestAction extends BaseRestHandler {

    @Inject
    public RocchioRestAction(Settings settings, RestController controller) {
        super(settings);
                
        // Register your handlers here
        controller.registerHandler(RestRequest.Method.GET, "/_expand/{index}", this);
        controller.registerHandler(RestRequest.Method.GET, "/_expand/{index}/{type}", this);
    }
    
    protected RestChannelConsumer throwError(String error) {
    	return throwError(error, RestStatus.BAD_REQUEST);
    }
    
    protected RestChannelConsumer throwError(String error, RestStatus status) {
	    return channel -> {
	    	XContentBuilder builder = JsonXContent.contentBuilder();
            builder.startObject();
            builder.field("error", error);
            builder.endObject();
	        channel.sendResponse(new BytesRestResponse(status, builder));
	    };
    }
    
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
    	this.logger.info("Executing REST action!");
    	
    	// Required parameters
    	String index = request.param("index");
    	String query = request.param("query");
    	
    	// Optional parameters, with sensible defaults
    	String type = request.param("type", "dataset");
    	String field = request.param("field", "_all");
    	double alpha = Double.parseDouble(request.param("alpha", "0.5"));
    	double beta = Double.parseDouble(request.param("beta", "0.5"));
    	double k1 = Double.parseDouble(request.param("k1", "1.2"));
    	double b = Double.parseDouble(request.param("b", "0.75"));
    	int fbDocs = Integer.parseInt(request.param("fbDocs", "10"));
    	int fbTerms = Integer.parseInt(request.param("fbTerms", "10"));

    	// FIXME: Our current implementation is very picky about inputs and state
    	// TODO: Check that index exists
    	// TODO: Check that index contains the desired type
    	// TODO: Check that type has documents added to it
    	// TODO: Check that the documents in the type contain the desired field
    	// 		NOTE: For _all we need special logic to check in a different place
    	// TODO: Check index to verify that store == true for the desired index/type/field combination
    	// TODO: Check that term vectors/fields stats are enabled and available for the desired index/type/field combination

    	boolean isValid = true;
    	if (isValid) {
    		return throwError("Testing");
    	}
    	
    	this.logger.info("Starting Rocchio with:");
    	this.logger.info("   index == " + index);
    	this.logger.info("   query == " + query);
    	this.logger.info("   type == " + type);
    	this.logger.info("   field == " + field);
    	this.logger.info("   fbTerms == " + fbTerms);
    	this.logger.info("   fbDocs == " + fbDocs);
    	this.logger.info("   alpha == " + alpha);
    	this.logger.info("   beta == " + beta);
    	this.logger.info("   k1 == " + k1);
    	this.logger.info("   b == " + b);
		Rocchio rocchio = new Rocchio(client, index, type, field, alpha, beta, k1, b);
		
		// Expand the query
    	this.logger.info("Generating feedback query!");
		FeatureVector feedbackQuery = rocchio.expandQuery(query, fbDocs, fbTerms);

    	this.logger.info("Expanding query!");
		StringBuffer expandedQuery = new StringBuffer();
		for (String term : feedbackQuery.getFeatures()) {
			expandedQuery.append(term + "^" + feedbackQuery.getFeatureWeight(term) + " ");
		}
				
		this.logger.info("Responding!");
	    return channel -> {
	    	XContentBuilder builder = JsonXContent.contentBuilder();
            builder.startObject();
            builder.field("query", expandedQuery.toString());
            builder.endObject();
	        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
	    };
    }
}