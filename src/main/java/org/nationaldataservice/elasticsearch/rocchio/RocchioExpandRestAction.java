package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
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

public class RocchioExpandRestAction extends BaseRestHandler {

    @Inject
    public RocchioExpandRestAction(Settings settings, RestController controller) {
        super(settings);
                
        // Register your handlers here
        controller.registerHandler(RestRequest.Method.GET, "/{index}/{type}/_expand", this);
        controller.registerHandler(RestRequest.Method.GET, "/{index}/_expand", this);
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
    	
    	// Required path parameter
    	String index = request.param("index");
    	
    	// Required query string parameter
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

    	IndexMetaData indexMetaData = client.admin().cluster()
                .state(Requests.clusterStateRequest())
                .actionGet()
                .getState()
                .getMetaData()
                .index(index);

    	ImmutableOpenMap<String,MappingMetaData> indexMap = indexMetaData.getMappings();
    	if (!indexMap.containsKey(type)) {
    		return throwError("No mapping found for: " + type);
    	}
    	
    	MappingMetaData typeMetadata = indexMetaData.getMappings().get(type);
    	Map<String, Object> typeMap = typeMetadata.getSourceAsMap();

    	LinkedHashMap<String, Object> fieldProperties;
    	// "_all" is located on the same level as properties, and needs special handling
    	if ("_all".equals(field)) {
    		fieldProperties = (LinkedHashMap<String, Object>) typeMap.get("_all");
    	} else {
    		LinkedHashMap<String, Object>  typePropertiesMap = (LinkedHashMap<String, Object>) typeMap.get("properties");
    		fieldProperties = (LinkedHashMap<String, Object>) typePropertiesMap.get(field);
    	}
    	
		if (!fieldProperties.containsKey("store")) {
			return throwError("Term vectors storage for the given field has not been configured");
		}
		
		boolean storeEnabled = (boolean) fieldProperties.get("store");
		if (!storeEnabled) {
			return throwError("Term vectors storage for the given field has been disabled");
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
    	
    	try {
			Rocchio rocchio = new Rocchio(client, index, type, field, alpha, beta, k1, b);
			
			// Expand the query
	    	this.logger.debug("Generating feedback query for (" + query +","+fbDocs+","+fbTerms);
			FeatureVector feedbackQuery = rocchio.expandQuery(query, fbDocs, fbTerms);
	
	    	this.logger.debug("Expanding query: " + feedbackQuery.toString());
			StringBuffer expandedQuery = new StringBuffer();
			for (String term : feedbackQuery.getFeatures()) {
				expandedQuery.append(term + "^" + feedbackQuery.getFeatureWeight(term) + " ");
			}
			
			this.logger.debug("Responding: " + expandedQuery.toString());
		    return channel -> {
		    	XContentBuilder builder = JsonXContent.contentBuilder();
		        builder.startObject();
		        builder.field("query", expandedQuery.toString());
		        builder.endObject();
		        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
		    };
    	} catch (Exception e) {
    		// FIXME: Catching generic Exception is bad practice
    		// TODO: make this more specific for production
    		return throwError("An unknown error was encountered.");
    	}
    }
}