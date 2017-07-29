package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsItemResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
//import org.elasticsearch.transport.client.PreBuiltTransportClient;

import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import joptsimple.internal.Strings;

/**
 * Rocchio implementation for Lucene based on:
 * https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java
 * 
 */
public class Rocchio {
	private Logger logger = ESLoggerFactory.getLogger(Rocchio.class);

	// FIXME: These are just random guesses see NDS-958
	private static int ALPHA_BETA_MIN = 0;
	private static int ALPHA_BETA_MAX = 1;
	private static int K1_MIN = 0;
	private static int K1_MAX = 2;
	private static int B_MIN = 0;
	private static int B_MAX = 1;

	// Error Strings returned from validate()
	public static String NULL_INDEX_ERROR = "You must specify an index to expand against";
	public static String NULL_QUERY_ERROR = "You must specify a query to expand";
	public static String NULL_TYPE_ERROR = "You must specify a type";
	public static String NULL_FIELD_ERROR = "You must specify a field";
	public static String INVALID_FB_TERMS_ERROR = "Number of feedback terms (fbTerms) must be a positive integer";
	public static String INVALID_FB_DOCS_ERROR = "Number of feedback documents (fbDocs) must be a positive integer";
	public static String INVALID_ALPHA_ERROR = "Alpha value must be a real number between " + ALPHA_BETA_MIN + " and "
			+ ALPHA_BETA_MAX;
	public static String INVALID_BETA_ERROR = "Beta value must be a real number between " + ALPHA_BETA_MIN + " and "
			+ ALPHA_BETA_MAX;
	public static String INVALID_K1_ERROR = "K1 value must be a real number between " + K1_MIN + " and " + K1_MAX;
	public static String INVALID_B_ERROR = "B value must be a real number between " + B_MIN + " and " + B_MAX;

	// Error Strings returned from ensureTermVectors()
	/**
	 * Returns a "nonexistent index" error message for the given index
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @return a "nonexistent index" error message
	 */
	public static String NONEXISTENT_INDEX_ERROR(String index) {
		return "Index does not exist: " + index;
	}

	/**
	 * Returns a "nonexistent type" error message for the given index/type
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @param type
	 * 			  the {@link String} type name
	 * @return a "nonexistent type error" error message
	 */
	public static String NONEXISTENT_TYPE_ERROR(String index, String type) {
		return "No mapping found on index " + index + " for: " + type;
	}

	/**
	 * Returns a "disabled term vectors" error message for the given index/type/field
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @param type
	 * 			  the {@link String} type name
	 * @param field
	 * 			  the {@link String} field name
	 * @return a "disabled term vectors" error message
	 */
	public static String DISABLED_TERM_VECTORS_ERROR(String index, String type, String field) {
		return "Term vectors storage for on " + index + "." + type + "." + field + " has been disabled";
	}

	/**
	 * Returns a "unconfigured term vectors" error message for the given index/type/field
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @param type
	 * 			  the {@link String} type name
	 * @param field
	 * 			  the {@link String} field name
	 * @return an "unconfigured term vectors" error message
	 */
	public static String UNCONFIGURED_TERM_VECTORS_ERROR(String index, String type, String field) {
		return "Term vectors storage for on index " + index + "." + type + "." + field + " has not been configured";
	}

	/**
	 * Returns a "missing term vector field" error message for the given index/type
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @param type
	 * 			  the {@link String} type name
	 * @return a "missing term vector field" error message
	 */
	public static String MISSING_TERM_VECTOR_FIELD(String index, String type) {
		return "Error: no fields received for term vector - " + index + "/" + type;
	}

	/**
	 * Returns a "missing field terms" error message for the given index/type/field
	 * 
	 * @param index
	 * 			  the {@link String} index name
	 * @param type
	 * 			  the {@link String} type name
	 * @param field
	 * 			  the {@link String} field name
	 * @return a "missing field terms" error message
	 */
	public static String MISSING_FIELD_TERMS(String index, String type, String field) {
		return "Error: no terms received for field - " + index + "/" + type + "/" + field;
	}

	private Client client; // ElasticSearch client
	private String index; // ElasticSearch index name
	private String type; // Document type
	private String field; // Field

	private double alpha; // Rocchio alpha
	private double beta; // Rocchio beta
	private double k1; // BM25 k1
	private double b; // BM25 b

	private Stopper stopper = null;

	// Global statistics (there's certainly a better way to handle this)
	long docCount = 0; // Number of documents in index
	double avgDocLen = 0; // Average document length, needed by BM25
	Map<String, Long> dfStats = new HashMap<String, Long>(); // Cached doc
																// frequency
																// stats

	/**
	 * Instantiates a new instance of the Rocchio algorithm with the given
	 * client and parameters.
	 * 
	 * @param client
	 *			  the {@link Client} to use for the connection
	 * @param index
	 *            the {@link String} index to expand against
	 * @param type
	 *            the {@link String} type within the index
	 * @param field
	 *            the {@link String} field on the type
	 * @param alpha
	 *            the {@link double} Rocchio alpha parameter
	 * @param beta
	 *            the {@link double} Rocchio beta parameter
	 * @param k1
	 *            the {@link double} Rocchio k1 parameter
	 * @param b
	 *            the {@link double} Rocchio b parameter
	 * @param stoplist
	 *            the {@link String} list of stop words
	 */
	public Rocchio(Client client, String index, String type, String field, double alpha, double beta, double k1,
			double b, String stoplist) {
		this.client = client;
		this.index = index;
		this.type = type;
		this.field = field;
		this.alpha = alpha;
		this.beta = beta;
		this.k1 = k1;
		this.b = b;

		this.setStoplist(stoplist);
	}
	
	/**
	 * Instantiates a new instance of the Rocchio algorithm with the given
	 * client and parameters.
	 * 
	 * @param client
	 *			  the {@link Client} to use for the connection
	 * @param index
	 *            the {@link String} index to expand against
	 * @param type
	 *            the {@link String} type within the index
	 * @param field
	 *            the {@link String} field on the type
	 * @param alpha
	 *            the {@link double} Rocchio alpha parameter
	 * @param beta
	 *            the {@link double} Rocchio beta parameter
	 * @param k1
	 *            the {@link double} Rocchio k1 parameter
	 * @param b
	 *            the {@link double} Rocchio b parameter
	 */
	public Rocchio(Client client, String index, String type, String field, double alpha, double beta, double k1,
			double b) {
		this(client, index, type, field, alpha, beta, k1, b, null);
	}

	// Assumes a space-delimited string
	private void setStoplist(String stoplist) {
		if (Strings.isNullOrEmpty(stoplist)) {
			this.stopper = null;
			return;
		}

		this.stopper = new Stopper();
		String[] stopwords = stoplist.split(" ");
		for (String term : stopwords) {
			stopper.addStopword(term);
		}
	}

	private void fail(String errorMessage) {
		this.logger.error(errorMessage);
		throw new IllegalStateException(errorMessage);
	}

	private void failIf(Supplier<Boolean> condition, String errorMessage) {
		if (condition.get()) {
			this.logger.error("Condition failed: " + condition.toString());
			fail(errorMessage);
		}
	}

	/**
	 * Verifies that String and numeric values are within their allowed ranges,
	 * then ensures that term vectors are properly enabled on the target index.
	 * 
	 * @param query
	 *            the String query to expand
	 * @param fbDocs
	 *            the int number of feedback documents
	 * @param fbTerms
	 *            the int number of feedback terms
	 * @return the String error message, or null if no errors are encountered
	 * @throws IOException
	 *             if the indexMetaData fails to deserialize into a map
	 */
	public String validate(String query, int fbDocs, int fbTerms) throws IOException {
		if (Strings.isNullOrEmpty(query)) {
			return NULL_QUERY_ERROR;
		} else if (fbDocs < 1) {
			return INVALID_FB_DOCS_ERROR;
		} else if (fbTerms < 1) {
			return INVALID_FB_TERMS_ERROR;
		} else if (Strings.isNullOrEmpty(index)) {
			return NULL_INDEX_ERROR;
		} else if (Strings.isNullOrEmpty(type)) {
			return NULL_TYPE_ERROR;
		} else if (Strings.isNullOrEmpty(field)) {
			return NULL_FIELD_ERROR;
		} else if (ALPHA_BETA_MIN > alpha || alpha > ALPHA_BETA_MAX) {
			return INVALID_ALPHA_ERROR;
		} else if (ALPHA_BETA_MIN > beta || beta > ALPHA_BETA_MAX) {
			return INVALID_BETA_ERROR;
		} else if (K1_MIN > k1 || k1 > K1_MAX) {
			return INVALID_K1_ERROR;
		} else if (B_MIN > b || b > B_MAX) {
			return INVALID_B_ERROR;
		}
		return this.ensureTermVectors();
	}

	/**
	 * Returns an error message if term vectors are misconfigured. Otherwise,
	 * returns null.
	 * 
	 * TODO: Some of this could potentially be called at plugin startup, if we
	 * know what index/type we plan to expand against ahead of time...
	 * 
	 * @return the String error message, or null if no errors are encountered
	 * 
	 * @throws IOException
	 *             if the indexMetaData fails to deserialize into a map
	 */
	@SuppressWarnings("unchecked")
	private String ensureTermVectors() throws IOException {
		// Verify that the index exists
		IndexMetaData indexMetaData = client.admin().cluster().state(Requests.clusterStateRequest()).actionGet()
				.getState().getMetaData().index(index);

		if (indexMetaData == null) {
			return NONEXISTENT_INDEX_ERROR(index);
		}

		// Verify that the index contains the desired type
		ImmutableOpenMap<String, MappingMetaData> indexMap = indexMetaData.getMappings();
		if (!indexMap.containsKey(type)) {
			return NONEXISTENT_TYPE_ERROR(index, type);
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
				String errorMessage = DISABLED_TERM_VECTORS_ERROR(index, type, field);
				this.logger.error(errorMessage);
				return errorMessage;
			}

			return null;
		} else if (fieldProperties.containsKey("store")) {
			// Verify that term vector storage is enabled at the field level
			boolean storeEnabled = (boolean) fieldProperties.get("store");
			if (!storeEnabled) {
				String errorMessage = DISABLED_TERM_VECTORS_ERROR(index, type, field);
				this.logger.error(errorMessage);
				return errorMessage;
			}

			return null;
		}

		// TODO: NDS-958 - Check that type has documents added to it?
		// TODO: NDS-958 - Check that the documents in the type contain the
		// desired field?
		// TODO: NDS-958 - Check that term vectors/fields stats are available
		// for the
		// desired index/type/field combination?

		// If neither of the above triggered, then we didn't have the right term
		// vectors initialized on our index
		String errorMessage = UNCONFIGURED_TERM_VECTORS_ERROR(index, type, field);
		this.logger.error(errorMessage);
		return errorMessage;
	}

	/**
	 * Run the query using the client (this assumes that the client has already
	 * been initialized and is ready to execute)
	 * 
	 * @param index
	 *            the String index to expand against
	 * @param query
	 *            Query string
	 * @param numDocs
	 *            Number of results to return
	 * @return SearchHits object
	 */
	private SearchResponse runQuery(String index, String query, int numDocs) {
		QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(query);
		return client.prepareSearch(index).setQuery(queryStringQueryBuilder).setSize(numDocs).execute().actionGet();
	}

	/**
	 * Given a set of SearchHits, construct the feedback vector
	 * 
	 * @param hits
	 *            SearchHits
	 * @param fbDocs
	 *            Number of feedback documents
	 * @return FeatureVector based on feedback documents
	 * @throws IOException
	 * 			  if the TermVector has no fields, or
	 * 			  if its Fields contain no terms
	 */
	private FeatureVector getFeedbackVector(SearchHits hits, int fbDocs) throws IOException {
		FeatureVector summedDocVec = new FeatureVector(this.stopper);

		// Use the multi termvector request to get vectors for all documents at
		// once
		MultiTermVectorsRequestBuilder mtbuilder = client.prepareMultiTermVectors();
		for (SearchHit hit : hits.hits()) {
			String id = hit.getId();
			TermVectorsRequest termVectorsRequest = new TermVectorsRequest();
			termVectorsRequest.index(index).id(id).type(this.type).termStatistics(true).offsets(false).positions(false)
					.payloads(false);

			mtbuilder.add(termVectorsRequest);
		}
		MultiTermVectorsResponse mtvresponse = mtbuilder.execute().actionGet();

		// Iterate over the returned document vectors. Construct the feedback
		// vector.
		// Store the global document count and calculate the global average
		// document length
		// Store document frequencies for encountered terms in dfStats map.
		for (MultiTermVectorsItemResponse item : mtvresponse.getResponses()) {
			FeatureVector docVec = new FeatureVector(this.stopper);

			TermVectorsResponse tv = item.getResponse();
			Fields fields = tv.getFields();
			failIf(() -> tv == null, MISSING_TERM_VECTOR_FIELD(index, type));

			Terms terms = fields.terms(this.field);
			failIf(() -> terms == null, MISSING_FIELD_TERMS(index, type, field));

			// These are global settings and will be the same for all
			// TermVectorResponses.
			// There's a better way to handle this.
			long sumTotalTermFreq = terms.getSumTotalTermFreq(); // Total number
																	// of terms
																	// in index
			docCount = terms.getDocCount(); // Total number of documents in
											// index
			avgDocLen = sumTotalTermFreq / (double) docCount;

			// Get the term frequency and document frequency for each term
			TermsEnum termsEnum = terms.iterator();
			while (termsEnum.next() != null) {
				String term = termsEnum.term().utf8ToString();
				long freq = termsEnum.totalTermFreq(); // Frequency for term t
														// in this document
				long df = termsEnum.docFreq(); // Frequency for term t in all
												// documents (document
												// frequency) -- a global
												// statistic
				dfStats.put(term, df); // Map storing global document
										// frequencies for seen terms, used by
										// BM25
				docVec.addTerm(term, freq); // Current document vector
			}

			// Add this document to the feedback document vector with BM25
			// weights
			computeBM25Weights(docVec, summedDocVec);
		}

		// Multiply the summed term vector by beta / |Dr|
		FeatureVector relDocTermVec = new FeatureVector(this.stopper);
		for (String term : summedDocVec.getFeatures()) {
			relDocTermVec.addTerm(term, summedDocVec.getFeatureWeight(term) * beta / fbDocs);
		}

		return relDocTermVec;
	}

	/**
	 * Construct the query vector with BM25 weights
	 * 
	 * @param query
	 *            Query string
	 * @return FeatureVector
	 */
	public FeatureVector getQueryVector(String query) {
		// Create a query vector and scale by alpha
		FeatureVector rawQueryVec = new FeatureVector(this.stopper);
		rawQueryVec.addText(query);

		FeatureVector summedQueryVec = new FeatureVector(this.stopper);
		computeBM25Weights(rawQueryVec, summedQueryVec);

		FeatureVector queryTermVec = new FeatureVector(this.stopper);
		for (String term : rawQueryVec.getFeatures()) {
			queryTermVec.addTerm(term, summedQueryVec.getFeatureWeight(term) * alpha);
		}

		return queryTermVec;
	}

	/**
	 * Expand the query.
	 * 
	 * @param query
	 *            Query string
	 * @param fbDocs
	 *            Number of feedback documents
	 * @param fbTerms
	 *            Number of feedback terms
	 * @return Expanded feature vector
	 * @throws IOException
	 * 			  if we fail to get the feedback vector
	 */
	public FeatureVector expandQuery(String query, int fbDocs, int fbTerms) throws IOException {
		// Run the initial query
		SearchHits hits = runQuery(this.index, query, fbDocs).getHits();

		// Get the feedback document vector, weighted by beta
		FeatureVector feedbackVector = getFeedbackVector(hits, fbDocs);

		// Get the original query vector, weighted by alpha
		// Note, this is called after getFeedbackVector because it relies on
		// dfStats
		FeatureVector queryVector = getQueryVector(query);

		// Combine query and feedbackvectors
		for (String term : queryVector.getFeatures()) {
			feedbackVector.addTerm(term, queryVector.getFeatureWeight(term));
		}

		// Get top terms -- aka head
		feedbackVector.clip(fbTerms);

		return feedbackVector;
	}

	/**
	 * Compute BM25 weights for the input vector and add to the output vector
	 * 
	 * @param inputVector
	 * 				the {@link FeatureVector} input
	 * @param outputVector
	 * 				the {@link FeatureVector} output
	 */
	private void computeBM25Weights(FeatureVector inputVector, FeatureVector outputVector) {
		for (String term : inputVector.getFeatures()) {
			long docOccur = dfStats.get(term);

			double idf = Math.log((docCount + 1) / (docOccur + 0.5)); // following Indri
			double tf = inputVector.getFeatureWeight(term);

			double weight = (idf * k1 * tf) / (tf + k1 * (1 - b + b * inputVector.getLength() / avgDocLen));
			outputVector.addTerm(term, weight);
		}
	}

	/**
	 * Debug: Command line options for the main() method (see below)
	 * 
	 * @return the CLI options
	 */
	public static Options createOptions() {
		Options options = new Options();
		options.addOption("cluster", true, "ElasticSearch cluster name (default: biocaddie)");
		options.addOption("host", true, "ElasticSearch host (default: localhost)");
		options.addOption("port", true, "ElasticSearch transport port (default: 9300)");
		options.addOption("index", true, "ElasticSearch index name (default: biocaddie)");
		options.addOption("type", true, "ElasticSearch document type  (default: dataset)");
		options.addOption("field", true, "ElasticSearch  field  (default: _all)");
		options.addOption("alpha", true, "Rocchio alpha (default: 0.5)");
		options.addOption("beta", true, "Rocchio beta (default: 0.5)");
		options.addOption("k1", true, "BM25 k1 (default: 1.2)");
		options.addOption("b", true, "BM25 b (default: 0.75)");
		options.addOption("query", true, "Query string");
		options.addOption("auth", true, "Basic authentication string (default: elastic:biocaddie)");
		return options;
	}

	/**
	 * Debug: this main method will run Rocchio as a standalone command-line
	 * application.
	 * 
	 * NOTE: You will need to add the following dependency to your
	 * {@code pom.xml}:
	 * 
	 * <pre>
	 *  &lt;dependency&gt;
	 *    &lt;groupId&gt;org.elasticsearch.client&lt;/groupId&gt;
	 *    &lt;artifactId&gt;transport&lt;/artifactId&gt;
	 *    &lt;version&gt;${elasticsearch.version}&lt;/version&gt;
	 *  &lt;/dependency&gt;
	 * </pre>
	 * 
	 * @param args
	 *            the command-line arguments
	 * @throws IOException
	 *             if expandQuery throws an IOException, or
	 *             if the host lookup fails (localhost shouldn't)
	 * @throws ParseException
	 *             if the command-line arguments cannot be parsed
	 */
	public static void main(String[] args) throws IOException, ParseException {

		/*
		 * Options options = createOptions(); CommandLineParser parser = new
		 * GnuParser(); CommandLine cl = parser.parse(options, args); if
		 * (cl.hasOption("help")) { HelpFormatter formatter = new
		 * HelpFormatter();
		 * formatter.printHelp(Rocchio.class.getCanonicalName(), options);
		 * return; }
		 * 
		 * // Get the many command line parameters String cluster =
		 * cl.getOptionValue("cluster", "elasticsearch"); String host =
		 * cl.getOptionValue("host", "localhost"); int port =
		 * Integer.parseInt(cl.getOptionValue("port", "9300")); double alpha =
		 * Double.parseDouble(cl.getOptionValue("alpha", "0.5")); double beta =
		 * Double.parseDouble(cl.getOptionValue("beta", "0.5")); double k1 =
		 * Double.parseDouble(cl.getOptionValue("k1", "1.2")); double b =
		 * Double.parseDouble(cl.getOptionValue("b", "0.75")); int fbTerms =
		 * Integer.parseInt(cl.getOptionValue("fbTerms", "10")); int fbDocs =
		 * Integer.parseInt(cl.getOptionValue("fbDocs", "10")); String index =
		 * cl.getOptionValue("index", "biocaddie"); String type =
		 * cl.getOptionValue("type", "dataset"); String field =
		 * cl.getOptionValue("field", "_all");
		 * 
		 * String auth = cl.getOptionValue("auth", "elastic:biocaddie"); String
		 * query = cl.getOptionValue("query", "multiple sclerosis");
		 * 
		 * // Connect to ElasticSearch Settings settings =
		 * Settings.builder().put("cluster.name", cluster).build();
		 * TransportClient transportClient = new
		 * PreBuiltTransportClient(settings);
		 * transportClient.addTransportAddress(new
		 * InetSocketTransportAddress(InetAddress.getByName(host), port));
		 * Client client =
		 * transportClient.filterWithHeader(Collections.singletonMap(
		 * "Authorization", auth));
		 * 
		 * // Construct Rocchio Rocchio rocchio = new Rocchio(client, index,
		 * type, field, alpha, beta, k1, b);
		 * 
		 * // Expand the query FeatureVector feedbackQuery =
		 * rocchio.expandQuery(query, fbDocs, fbTerms);
		 * 
		 * // Dump the expanded query StringBuffer esQuery = new StringBuffer();
		 * for (String term : feedbackQuery.getFeatures()) { esQuery.append(term
		 * + "^" + feedbackQuery.getFeatureWeight(term) + " "); }
		 * System.out.println(esQuery);
		 * 
		 * transportClient.close();
		 */
	}
}