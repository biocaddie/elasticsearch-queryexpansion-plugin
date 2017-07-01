package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.client.Client;
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
import joptsimple.internal.Strings;

public class RocchioExpandRestAction extends BaseRestHandler {
	// FIXME: These are just random guesses
	private static int ALPHA_BETA_MIN = 0;
	private static int ALPHA_BETA_MAX = 1;
	private static int K1_MIN = 0;
	private static int K1_MAX = 2;
	private static int B_MIN = 0;
	private static int B_MAX = 1;

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
		this.logger.error("ERROR: " + error);
		return channel -> {
			XContentBuilder builder = JsonXContent.contentBuilder();
			builder.startObject();
			builder.field("error", error);
			builder.endObject();
			channel.sendResponse(new BytesRestResponse(status, builder));
		};
	}

	/**
	 * Verifies that String and numeric values are within their allowed ranges,
	 * 
	 * @param index
	 *            the String index to expand against
	 * @param query
	 *            the String query to expand
	 * @param type
	 *            the String type within the index
	 * @param field
	 *            the String field on the type
	 * @param fbDocs
	 *            the int number of feedback documents
	 * @param fbTerms
	 *            the int number of feedback terms
	 * @param alpha
	 *            the double Rocchio alpha parameter
	 * @param beta
	 *            the double Rocchio beta parameter
	 * @param k1
	 *            the double Rocchio k1 parameter
	 * @param b
	 *            the double Rocchio b parameter
	 * @return the String error message, or null if no errors are encountered
	 */
	private String getErrors(String index, String query, String type, String field, int fbDocs, int fbTerms,
			double alpha, double beta, double k1, double b) {
		if (Strings.isNullOrEmpty(index)) {
			return "You must specify an index to expand against";
		} else if (Strings.isNullOrEmpty(query)) {
			return "You must specify a query to expand";
		} else if (Strings.isNullOrEmpty(type)) {
			return "You must specify a type";
		} else if (Strings.isNullOrEmpty(field)) {
			return "You must specify a field";
		} else if (fbDocs < 1) {
			return "Number of feedback documents (fbDocs) must be a positive integer";
		} else if (fbTerms < 1) {
			return "Number of feedback terms (fbTerms) must be a positive integer";
		} else if (ALPHA_BETA_MIN > alpha || alpha > ALPHA_BETA_MAX) {
			return "Alpha value must be a real number between " + ALPHA_BETA_MIN + " and " + ALPHA_BETA_MAX;
		} else if (ALPHA_BETA_MIN > beta || beta > ALPHA_BETA_MAX) {
			return "Beta value must be a real number between " + ALPHA_BETA_MIN + " and " + ALPHA_BETA_MAX;
		} else if (K1_MIN > k1 || k1 > K1_MAX) {
			return "K1 value must be a real number between " + K1_MIN + " and " + K1_MAX;
		} else if (B_MIN > b || b > B_MAX) {
			return "B value must be a real number between " + B_MIN + " and " + B_MAX;
		}
		return null;
	}

	/**
	 * Returns an error message if term vectors are misconfigured. Otherwise,
	 * returns null.
	 * 
	 * TODO: Some of this can potentially be called at plugin startup, if we
	 * know what index/type we plan to expand against ahead of time...
	 * 
	 * @param client
	 *            the client to use for the connection
	 * @param index
	 *            the index to check for the desired type
	 * @param type
	 *            the type to check for the desired field
	 * @param field
	 *            the field for which to verify that term vectors are enabled
	 * @return the String error message, or null if no errors are encountered
	 * 
	 * @throws IOException
	 *             if the indexMetaData fails to deserialize into a map
	 */
	@SuppressWarnings("unchecked")
	private String ensureTermVectors(Client client, String index, String type, String field) throws IOException {
		// Verify that the index exists
		IndexMetaData indexMetaData = client.admin().cluster().state(Requests.clusterStateRequest()).actionGet()
				.getState().getMetaData().index(index);

		if (indexMetaData == null) {
			return "Index does not exist";
		}

		// Verify that the index contains the desired type
		ImmutableOpenMap<String, MappingMetaData> indexMap = indexMetaData.getMappings();
		if (!indexMap.containsKey(type)) {
			return "No mapping found on index " + index + " for: " + type;
		}

		// Grab the type and analyze it to locate the field
		MappingMetaData typeMetadata = indexMetaData.getMappings().get(type);
		Map<String, Object> typeMap = typeMetadata.getSourceAsMap();

		LinkedHashMap<String, Object> fieldProperties,
				allFieldProperties = (LinkedHashMap<String, Object>) typeMap.get("_all");
		if (!"_all".equals(field)) {
			// Otherwise, we need to drill down into "properties"
			LinkedHashMap<String, Object> typePropertiesMap = (LinkedHashMap<String, Object>) typeMap.get("properties");
			fieldProperties = (LinkedHashMap<String, Object>) typePropertiesMap.get(field);
		} else {
			// we can look for "store" on "_all" too
			fieldProperties = allFieldProperties;
		}

		// Verify that "store" is present on either _all or our target field
		if (allFieldProperties.containsKey("store")) {
			// Verify that term vector storage is enabled for all fields
			boolean storeEnabled = (boolean) allFieldProperties.get("store");
			if (!storeEnabled) {
				this.logger.error(
						"Term vectors storage for on " + index + "." + type + "." + field + " has been disabled");
				return "Term vectors storage for " + index + "." + type + "." + field + " has been disabled";
			}

			return null;
		} else if (fieldProperties.containsKey("store")) {
			// Verify that term vector storage is enabled at the field level
			boolean storeEnabled = (boolean) fieldProperties.get("store");
			if (!storeEnabled) {
				this.logger.error(
						"Term vectors storage for on index " + index + "." + type + "." + field + " has been disabled");
				return "Term vectors storage for " + index + "." + type + "." + field + " has been disabled";
			}

			return null;
		}

		// If neither of the above triggered, then we didn't have the right term
		// vectors initialized on our index
		this.logger.error(
				"Term vectors storage for on index " + index + "." + type + "." + field + " has not been configured");
		return "Term vectors storage for " + index + "." + type + "." + field + " has not been configured";
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

		this.logger.info(String.format("Starting Rocchio (%s,%s,%s,%s,%d,%d,%.2f,%.2f,%.2f,%.2f)", index, query, type,
				field, fbDocs, fbTerms, alpha, beta, k1, b));

		String shortCircuit = this.getErrors(index, query, type, field, fbDocs, fbTerms, alpha, beta, k1, b);
		if (!Strings.isNullOrEmpty(shortCircuit)) {
			return throwError(shortCircuit);
		}

		// TODO: Check that type has documents added to it?
		// TODO: Check that the documents in the type contain the desired field?

		// Examine the index to verify that store == true for the desired
		// index/type/field combination
		shortCircuit = this.ensureTermVectors(client, index, type, field);
		if (!Strings.isNullOrEmpty(shortCircuit)) {
			return throwError(shortCircuit);
		}

		// TODO: Check that term vectors/fields stats are available for the
		// desired index/type/field combination?

		try {
			this.logger.debug("Starting Rocchio with:");
			this.logger.debug("   index == " + index);
			this.logger.debug("   query == " + query);
			this.logger.debug("   type == " + type);
			this.logger.debug("   field == " + field);
			this.logger.debug("   fbTerms == " + fbTerms);
			this.logger.debug("   fbDocs == " + fbDocs);
			this.logger.debug("   alpha == " + alpha);
			this.logger.debug("   beta == " + beta);
			this.logger.debug("   k1 == " + k1);
			this.logger.debug("   b == " + b);

			Rocchio rocchio = new Rocchio(client, index, type, field, alpha, beta, k1, b);

			// Expand the query
			this.logger.debug("Generating feedback query for (" + query + "," + fbDocs + "," + fbTerms);
			FeatureVector feedbackQuery = rocchio.expandQuery(query, fbDocs, fbTerms);

			this.logger.debug("Expanding query: " + feedbackQuery.toString());
			StringBuffer expandedQuery = new StringBuffer();
			String separator = ""; // start out with no separator
			for (String term : feedbackQuery.getFeatures()) {
				expandedQuery.append(separator + term + "^" + feedbackQuery.getFeatureWeight(term));
				separator = " "; // add separator after first iteration
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
			String errorMessage = e.getMessage();
			if (Strings.isNullOrEmpty(errorMessage)) {
				errorMessage = "An unknown error was encountered.";
			}
			return throwError(errorMessage);
		}
	}
}