package org.nationaldataservice.elasticsearch.rocchio;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsItemResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
//import org.elasticsearch.transport.client.PreBuiltTransportClient;

import edu.gslis.textrepresentation.FeatureVector;


/**
 * Rocchio implementation for Lucene based on: 
 *   https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/expansion/Rocchio.java
 * 
 * 
 * 
 * Todo: 
 * 		Specify stoplist
 */
public class Rocchio 
{
	private Logger logger = ESLoggerFactory.getLogger(Rocchio.class);
	
	private Client client;  // ElasticSearch client
	private String index;   // ElasticSearch index name
	private String type;    // Document type
	private String field;   // Field
	
	private double alpha;   // Rocchio alpha
	private double beta;    // Rocchio beta
	private double k1;      // BM25 k1	
	private double b;       // BM25 b
		
	// Global statistics (there's certainly a better way to handle this)
	long docCount = 0;      // Number of documents in index
	double avgDocLen = 0;   // Average document length, needed by BM25
	Map<String, Long> dfStats = new HashMap<String, Long>(); // Cached doc frequency stats

	/**
	 * Instantiates a new instance of the Rocchio algorithm with the given client and parameters.
	 * 
	 * @param client
	 * @param index
	 * @param type
	 * @param field
	 * @param alpha
	 * @param beta
	 * @param k1
	 * @param b
	 */
	public Rocchio(Client client, String index, String type, String field, double alpha, double beta, double k1, double b) {
		this.client = client;
		this.index = index;
		this.type = type;
		this.field = field;
		this.alpha = alpha;
		this.beta = beta;
		this.k1 = k1;
		this.b = b;
	}
	
	private void fail(String errorMessage) {
		this.logger.error(errorMessage);
		throw new IllegalStateException();
	}
	
	private void failIf(Supplier<Boolean> condition, String errorMessage) {
		if (condition.get()) {
			this.logger.error("Condition failed");
			fail(errorMessage);
		}
	}
	
	/**
	 * Run the query using the client (this assumes that the client has already been initialized and is ready to execute)
	 * 
	 * @param query	Query string
	 * @param numDocs Number of results to return
	 * @return SearchHits object
	 */
	private SearchHits runQuery(String query, int numDocs) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index);	        
        QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(query);
        searchRequestBuilder.setQuery(queryStringQueryBuilder).setSize(numDocs);
        SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchHits hits = response.getHits();
        return hits;
	}
	
	/**
	 * Given a set of SearchHits, construct the feedback vector
	 * 
	 * @param hits  SearchHits
	 * @param fbDocs  Number of feedback documents
	 * @return  FeatureVector based on feedback documents
	 * @throws IOException
	 */
	private FeatureVector getFeedbackVector(SearchHits hits, int fbDocs) throws IOException 
	{
		FeatureVector summedDocVec = new FeatureVector(null);	

		// Use the multi termvector request to get vectors for all documents at once
	    MultiTermVectorsRequestBuilder mtbuilder = client.prepareMultiTermVectors();
		for (SearchHit hit: hits) {
			String id = hit.getId();
			TermVectorsRequest termVectorsRequest = new TermVectorsRequest();					
			termVectorsRequest.index(index).id(id).type(this.type).termStatistics(true).
				offsets(false).positions(false).payloads(false);
			
			mtbuilder.add(termVectorsRequest);
		}			
	    MultiTermVectorsResponse mtvresponse = mtbuilder.execute().actionGet();

		// Iterate over the returned document vectors. Construct the feedback vector.
	    // Store the global document count and calculate the global average document length
		// Store document frequencies for encountered terms in dfStats map.
	    for (MultiTermVectorsItemResponse item: mtvresponse.getResponses()) {
		    FeatureVector docVec = new FeatureVector(null);
		    
	    	TermVectorsResponse tv = item.getResponse();
	    	Fields fields =  tv.getFields();
	    	failIf(() -> tv == null, "Error: no fields received for term vector - " + this.index + "/" + this.type);
	    	
		    Terms terms = fields.terms(this.field);
	    	failIf(() -> terms == null, "Error: no terms received for field - " + this.index + "/" + this.type + "/" + this.field);
		    
		    // These are global settings and will be the same for all TermVectorResponses.
		    // There's a better way to handle this.
		    long sumTotalTermFreq = terms.getSumTotalTermFreq(); // Total number of terms in index
		    docCount = terms.getDocCount();  // Total number of documents in index
		    avgDocLen = sumTotalTermFreq/(double)docCount;
		    
		    // Get the term frequency and document frequency for each term
		    TermsEnum termsEnum = terms.iterator();
		    while (termsEnum.next() != null) {
		    	String term = termsEnum.term().utf8ToString();
		    	long freq = termsEnum.totalTermFreq();  // Frequency for term t in this document
		    	long df = termsEnum.docFreq(); // Frequency for term t in all documents (document frequency) -- a global statistic
		    	dfStats.put(term, df); // Map storing global document frequencies for seen terms, used by BM25
		    	docVec.addTerm(term, freq); // Current document vector
		    }
		    
		    // Add this document to the feedback document vector with BM25 weights
		    computeBM25Weights(docVec, summedDocVec);			    
	    }
        		    
		// Multiply the summed term vector by beta / |Dr|
		FeatureVector relDocTermVec = new FeatureVector(null);
		for (String term : summedDocVec.getFeatures()) {
			relDocTermVec.addTerm(term, summedDocVec.getFeatureWeight(term) * beta / fbDocs);
		}
		
		return relDocTermVec;
	}
	
	/**
	 * Construct the query vector with BM25 weights
	 * 
	 * @param query Query string
	 * @return FeatureVector
	 */
	public FeatureVector getQueryVector(String query) {
		// Create a query vector and scale by alpha
		FeatureVector rawQueryVec = new FeatureVector(null);
		rawQueryVec.addText(query);
		
		FeatureVector summedQueryVec = new FeatureVector(null);
	    computeBM25Weights(rawQueryVec, summedQueryVec);
		
		FeatureVector queryTermVec = new FeatureVector(null);
		for (String term : rawQueryVec.getFeatures()) {
			queryTermVec.addTerm(term, summedQueryVec.getFeatureWeight(term) * alpha);
		}
		
		return queryTermVec;
	}
	
	/**
	 * Expand the query.
	 * 
	 * @param query Query string
	 * @param fbDocs Number of feedback documents
	 * @param fbTerms Number of feedback terms
	 * @return Expanded feature vector
	 * @throws IOException
	 */
	public FeatureVector expandQuery(String query, int fbDocs, int fbTerms) throws IOException 
	{
		// Run the initial query
        SearchHits hits = runQuery(query, fbDocs);
       
        // Get the feedback document vector, weighted by beta
        FeatureVector feedbackVector = getFeedbackVector(hits, fbDocs);
        
        // Get the original query vector, weighted by alpha
        // Note, this is called after getFeedbackVector because it relies on dfStats
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
	 * @param outputVector
	 */
	private void computeBM25Weights(FeatureVector inputVector,  FeatureVector outputVector) 
	{
		for (String term : inputVector.getFeatures()) {
			long docOccur = dfStats.get(term);
			
			double idf = Math.log( (docCount + 1) / (docOccur + 0.5) ); // following Indri
			double tf = inputVector.getFeatureWeight(term);
			
			double weight = (idf * k1 * tf) / (tf + k1 * (1 - b + b * inputVector.getLength() / avgDocLen));
			outputVector.addTerm(term, weight);
		}
	}	
}