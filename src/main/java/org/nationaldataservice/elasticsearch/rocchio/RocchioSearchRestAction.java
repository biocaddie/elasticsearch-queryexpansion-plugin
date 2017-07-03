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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import edu.gslis.textrepresentation.FeatureVector;
import joptsimple.internal.Strings;

public class RocchioSearchRestAction extends BaseRestHandler {
	
	@Inject
	public RocchioSearchRestAction(Settings settings, RestController controller) {
		super(settings);
		
		// Register your handlers here
		controller.registerHandler(RestRequest.Method.GET, "/{index}/{type}/_esearch", this);
		controller.registerHandler(RestRequest.Method.GET, "/{index}/_esearch", this);
	}

	protected RestChannelConsumer throwError(String error) {
		return throwError(new RocchioException(error));
	}

	protected RestChannelConsumer throwError(RocchioException ex) {
		return throwError(ex, RestStatus.BAD_REQUEST);
	}

	protected RestChannelConsumer throwError(RocchioException ex, RestStatus status) {
		this.logger.error("ERROR: " + ex.getMessage(), ex);
		
		// Log nested errors
		Throwable current = ex.getCause();
		while (current != null) {
			if (ex.getCause() != null) {
				this.logger.error("Caused By: " + current.getMessage(), current);
			}
		}
		
		return channel -> {
			XContentBuilder builder = JsonXContent.contentBuilder();
			builder.startObject();
			builder.field("error", ex.getMessage());
			builder.endObject();
			channel.sendResponse(new BytesRestResponse(status, builder));
		};
	}
	
	@Override
	protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
		this.logger.debug("Executing Rocchio expand + search action!");

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
		
		// Optional stoplist (defaults to null)
		String stoplist = request.param("stoplist", null);
		
		// Optional searchIndex (if different from the index we are expanding against)
		String expandOn = request.param("expandOn", index);

		// Log the request with our full parameter set
		this.logger.info(String.format("Starting RocchioSearch (index=%s, expandOn=%s, query=%s, "
				+ "type=%s, field=%s, fbDocs=%d, fbTerms=%d, α=%.2f, β=%.2f, k1=%.2f, b=%.2f, stoplist=%s)", 
				index, expandOn, query, type, field, fbDocs, fbTerms, alpha, beta, k1, b, stoplist));

		try {
			// Run Rocchio expansion against the index given by "expandOn"
			Rocchio rocchio = new Rocchio(client, expandOn, type, field, alpha, beta, k1, b, stoplist);

			String shortCircuit = rocchio.validate(query, fbDocs, fbTerms);
			if (!Strings.isNullOrEmpty(shortCircuit)) {
				return throwError(shortCircuit);
			}

			// Expand the query
			this.logger.debug("Generating feedback query for (" + query + "," + fbDocs + "," + fbTerms + ")");
			FeatureVector feedbackQuery = rocchio.expandQuery(query, fbDocs, fbTerms);

			// Format our expanded query with Lucene's boosting syntax
			this.logger.debug("Expanding query: " + feedbackQuery.toString());
			StringBuffer expandedQuery = new StringBuffer();
			String separator = ""; // start out with no separator
			for (String term : feedbackQuery.getFeatures()) {
				expandedQuery.append(separator + term + "^" + feedbackQuery.getFeatureWeight(term));
				separator = "+"; // add separator after first iteration
			}
			
			// Now, perform the actual search with the expanded query
			this.logger.info("Running expanded query against: " + index);
			SearchHits hits = rocchio.runQuery(index, query, 10);

			// Build of a response of our search hits
			this.logger.debug("Responding: " + expandedQuery.toString());
			return channel -> {
				final XContentBuilder builder = JsonXContent.contentBuilder();
				//builder.startObject();
				// TODO: Match return value/structure for _search
				//builder.field("hits");
				builder.startArray();
				for (SearchHit hit : hits) {
					builder.value(hit.getSourceAsString());
				}
				builder.endArray();
				//builder.endObject();
				channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
			};
		} catch (Exception e) {
			// FIXME: Catching generic Exception is bad practice
			// TODO: make this more specific for production
			String errorMessage = e.getMessage();
			if (Strings.isNullOrEmpty(errorMessage)) {
				errorMessage = "An unknown error was encountered.";
			}
			return throwError(new RocchioException(errorMessage, e.getCause()));
		}
	}
}