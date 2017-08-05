package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestRequest.Method;
import org.elasticsearch.rest.RestStatus;

import edu.gslis.textrepresentation.FeatureVector;
import joptsimple.internal.Strings;

public class RocchioExpandRestAction extends BaseRestHandler {
    private final Logger logger = ESLoggerFactory.getLogger(RocchioExpandRestAction.class);

    @Inject
    public RocchioExpandRestAction(Settings settings, RestController controller) {
        super(settings);

        // Register your handlers here
        controller.registerHandler(Method.GET, "/{index}/{type}/_expand", this);
        controller.registerHandler(Method.GET, "/{index}/_expand", this);
    }

    /**
     * Helper method for throwing an error
     * 
     * @param error
     *            the String error message
     * @return a RestChannelConsumer to build up the error
     */
    protected RestChannelConsumer throwError(String error) {
        return throwError(error, RestStatus.BAD_REQUEST);
    }

    /**
     * Helper method for throwing an error
     * 
     * @param error
     *            the String error message
     * @param status
     *            the HTTP status to return
     * @return a RestChannelConsumer to build up the error
     */
    protected RestChannelConsumer throwError(String error, RestStatus status) {
        this.logger.error("ERROR: " + error);
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
        this.logger.debug("Executing Rocchio expand action!");

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

        // Optional stoplist - assumes a space-delimited string of stop words
        // TODO: Populate list of default stop words
        String stoplist = request.param("stoplist", "");

        // Log the request with our full parameter set
        this.logger.info(String.format(
                "Starting RocchioExpand (index=%s, query=%s, type=%s, "
                        + "field=%s, fbDocs=%d, fbTerms=%d, α=%.2f, β=%.2f, k1=%.2f, b=%.2f, stoplist=%s)",
                index, query, type, field, fbDocs, fbTerms, alpha, beta, k1, b, stoplist));

        // TODO: Check that type has documents added to it?
        // TODO: Check that the documents in the type contain the desired field?
        // TODO: Check that term vectors/fields stats are available for the
        // desired index/type/field combination?

        try {
            Rocchio rocchio = new Rocchio(client, index, type, field, alpha, beta, k1, b, stoplist);
    
            // Validate input parameters
            String shortCircuit = rocchio.validate(query, fbDocs, fbTerms);
            if (!Strings.isNullOrEmpty(shortCircuit)) {
                return throwError(shortCircuit);
            }
    
            // Expand the query
            this.logger.debug("Generating feedback query for (" + query + "," + fbDocs + "," + fbTerms);
            FeatureVector feedbackQuery = rocchio.expandQuery(query, fbDocs, fbTerms);
    
            // Format our expanded query with Lucene's boosting syntax
            this.logger.debug("Expanding query: " + feedbackQuery.toString());
            StringBuffer expandedQuery = new StringBuffer();
            String separator = ""; // start out with no separator
    
            for (String term : feedbackQuery.getFeatures()) {
                expandedQuery.append(separator + term + "^" + feedbackQuery.getFeatureWeight(term));
                separator = " "; // add separator after first iteration
            }
    
            String fullQuery = expandedQuery.toString().trim();
    
            // Return the expanded query (don't actually perform the search)
            this.logger.debug("Responding: " + expandedQuery.toString());
            return channel -> {
                XContentBuilder builder = JsonXContent.contentBuilder();
                builder.startObject();
    
                builder.field("query", fullQuery);
                builder.endObject();
                channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
            };
		} catch (Exception e) {
			// FIXME: Catching generic Exception is bad practice
			// TODO: make this more specific for production
			String errorMessage = e.getMessage();
			if (Strings.isNullOrEmpty(errorMessage)) {
				errorMessage = "An unknown error was encountered.";
			}
			return throwError(errorMessage);
		}
	}
}