package org.nationaldataservice.elasticsearch.rocchio.test.unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsItemResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.nationaldataservice.elasticsearch.rocchio.Rocchio;

import edu.gslis.textrepresentation.FeatureVector;

@RunWith(MockitoJUnitRunner.class)
public class RocchioTest {
	// TODO: Insert real/mocked settings here?
	//private Settings settings = Settings.builder().build();
	
	// TODO: Mock these
	private Client client;
	
	// Our common test parameter set (individual tests can still use one-off values)
	private static String TEST_EXPAND_INDEX = "biocaddie";
	private static String TEST_SEARCH_INDEX = "biocaddie";
	private static String TEST_QUERY = "multiple sclerosis";
	private static String TEST_TYPE = "dataset";
	private static String TEST_FIELD = "_all";
	private static int TEST_FB_TERMS = 10;
	private static int TEST_FB_DOCS = 50;
	private double TEST_ALPHA = 0.5;
	private double TEST_BETA = 0.5;
	private double TEST_K1 = 1.2;
	private double TEST_B = 0.75;
	
	private Rocchio rocchio;
	
	/* This one is sort of a ridiculous rabbit hole to mock out... */
	private void mockIndexMetaDataRequest() throws IOException {
		LinkedHashMap<String, Object> fieldPropertiesMap = new LinkedHashMap<String, Object>();
		fieldPropertiesMap.put("store", true);
		
		LinkedHashMap<String, Object> typePropertiesMap = new LinkedHashMap<String, Object>();
		typePropertiesMap.put(TEST_FIELD, fieldPropertiesMap);
		
		LinkedHashMap<String, Object> typeMap = new LinkedHashMap<String, Object>();
		typeMap.put("properties", typePropertiesMap);
		
		MappingMetaData mockTypeMetadata = mock(MappingMetaData.class);
		when(mockTypeMetadata.getSourceAsMap()).thenReturn(typeMap);

		IndexMetaData mockIndexMetaData = mock(IndexMetaData.class);
		//ImmutableOpenMap<String, MappingMetaData> mockIndexMappingMetadata = new ImmutableOpenMap<String, MappingMetaData>();
		
		// FIXME: We can't mock final classes...
		//ImmutableOpenMap<String, MappingMetaData> mockIndexMappingMetadata = (ImmutableOpenMap<String, MappingMetaData>) mock(ImmutableOpenMap.class);
		//when(mockIndexMetaData.getMappings()).thenReturn(mockIndexMappingMetadata);
		//when(mockIndexMappingMetadata.get(anyString())).thenReturn(mockTypeMetadata);
		
		// Initialize our mocked ElasticSearch environment
		/*when(this.client.admin().cluster().state(Requests.clusterStateRequest()).actionGet()
				.getState().getMetaData().index(anyString())).thenReturn(mockIndexMetaData);*/
		
		AdminClient adminClient = mock(AdminClient.class);
		when(client.admin()).thenReturn(adminClient);
		
		ClusterAdminClient clusterAdminClient = mock(ClusterAdminClient.class);
		when(adminClient.cluster()).thenReturn(clusterAdminClient);
		
		ActionFuture<ClusterStateResponse> clusterStateFuture = (ActionFuture<ClusterStateResponse>) mock(ActionFuture.class);
		when(clusterAdminClient.state(Requests.clusterStateRequest())).thenReturn(clusterStateFuture);
		
		ClusterStateResponse clusterStateResponse = mock(ClusterStateResponse.class);
		when(clusterStateFuture.actionGet()).thenReturn(clusterStateResponse);
		
		ClusterState clusterState = mock(ClusterState.class);
		when(clusterStateResponse.getState()).thenReturn(clusterState);
		
		MetaData clusterMetadata = mock(MetaData.class);
		when(clusterState.getMetaData()).thenReturn(clusterMetadata);
		
		when(clusterMetadata.index(anyString())).thenReturn(mockIndexMetaData);
		
		
	}
	
	private void mockTermVectorRequest() {
		MultiTermVectorsRequestBuilder multiTermVectorBuilder = mock(MultiTermVectorsRequestBuilder.class);
		when(multiTermVectorBuilder.add(any())).thenReturn(multiTermVectorBuilder);
		
		MultiTermVectorsResponse mtvResponse = mock(MultiTermVectorsResponse.class);
		
		@SuppressWarnings("unchecked")
		ListenableActionFuture<MultiTermVectorsResponse> mockedFuture = mock(ListenableActionFuture.class);

		when(multiTermVectorBuilder.execute()).thenReturn(mockedFuture);
		when(mockedFuture.actionGet()).thenReturn(mtvResponse);
		
		when(this.client.prepareMultiTermVectors()).thenReturn(multiTermVectorBuilder);
		
		//MultiTermVectorsItemResponse mtvItemResponse = mock(MultiTermVectorsItemResponse.class);
		//when(mtvResponse.getResponses()).thenReturn(mtvItemResponse);
	}
	
	private void mockSearchRequest() {
		SearchRequestBuilder srBuilder = mock(SearchRequestBuilder.class);
		when(srBuilder.setQuery(any(QueryStringQueryBuilder.class))).thenReturn(srBuilder);
		when(srBuilder.setSize(anyInt())).thenReturn(srBuilder);
		
		@SuppressWarnings("unchecked")
		ListenableActionFuture<SearchResponse> mockedFuture = mock(ListenableActionFuture.class);
		when(srBuilder.execute()).thenReturn(mockedFuture);
		
		// TODO: Somehow get the values to return here for expand / search
		SearchHit[] hitsArray = new SearchHit[3];
		hitsArray[0] = mock(SearchHit.class);
		hitsArray[1] = mock(SearchHit.class);
		hitsArray[2] = mock(SearchHit.class);
		
		SearchHits hits = mock(SearchHits.class);
		when(hits.getHits()).thenReturn(hitsArray);
		when(hits.hits()).thenReturn(hitsArray);
		when(hits.totalHits()).thenReturn(Long.valueOf(3));
		when(hits.getTotalHits()).thenReturn(Long.valueOf(3));
		
		SearchResponse response = mock(SearchResponse.class);
		when(mockedFuture.actionGet()).thenReturn(response);
		when(response.getHits()).thenReturn(hits);
		
		when(this.client.prepareSearch(anyString())).thenReturn(srBuilder);
	}
	
	@Before
	public void setUp() throws IOException {
		this.client = mock(TransportClient.class);
		
		// FIXME: Can't mock this due to final class
		//mockIndexMetaDataRequest();
		mockTermVectorRequest();
		mockSearchRequest();
		
		// Initialize our Rocchio implementation (not mocked)
		this.rocchio = createRocchio(TEST_EXPAND_INDEX, TEST_TYPE, TEST_FIELD, TEST_ALPHA, TEST_BETA, TEST_K1, TEST_B);
	}
	
	private Rocchio createRocchio(String index, String type, String field, double alpha, double beta, double k1, double b) {
		return new Rocchio(client, index, type, field, alpha, beta, k1, b);
		
	}
	
	@After
	public void tearDown() {
		this.client = null;
		this.rocchio = null;
	}
	
	@Test
	// Test that validate returns null if all parameters are valid
	public void testValidate() throws IOException {
		// Validate example input parameters (should be valid)
		String shouldBeNull = rocchio.validate(TEST_QUERY, TEST_FB_DOCS, TEST_FB_TERMS);
		assertNull(shouldBeNull);
	}
	
	@Test
	// Test that validate fails when query is null
	public void testValidateInvalidQuery() throws IOException {
		// Validate example input parameters (should be valid)
		String errorMessage = rocchio.validate("", TEST_FB_DOCS, TEST_FB_TERMS);
		assertNotNull(errorMessage);
		assertEquals(Rocchio.NULL_QUERY_ERROR, errorMessage);
	}
	
	@Test
	// Test that validate fails when fbDocs < 1
	public void testValidateInvalidFeedbackDocuments() throws IOException {
		// Validate example input parameters (should be valid)
		String errorMessage = rocchio.validate(TEST_QUERY, 0, TEST_FB_TERMS);
		assertNotNull(errorMessage);
		assertEquals(Rocchio.INVALID_FB_DOCS_ERROR, errorMessage);
	}
	
	@Test
	// Test that validate fails when fbTerms < 1
	public void testValidateInvalidFeedbackTerms() throws IOException {
		// Validate example input parameters (should be valid)
		String errorMessage = rocchio.validate(TEST_QUERY, TEST_FB_DOCS, 0);
		assertNotNull(errorMessage);
		assertEquals(Rocchio.INVALID_FB_TERMS_ERROR, errorMessage);
	}
	
	@Test
	// Test that we have correctly mocked runQuery
	public void testRunQuery() {
		SearchHits hits = rocchio.runQuery(TEST_SEARCH_INDEX, TEST_QUERY, TEST_FB_DOCS).getHits();
		assertEquals(3, hits.getTotalHits());
	}
	
	@Test
	// Test that we can expand a query against the test index
	public void testExpandQuery() throws IOException {
		// Expand the query
		FeatureVector feedbackQuery = rocchio.expandQuery(TEST_QUERY, TEST_FB_DOCS, TEST_FB_TERMS);

		// Format our expanded query with Lucene's boosting syntax
		StringBuffer expandedQuery = new StringBuffer();
		String separator = ""; // start out with no separator
		for (String term : feedbackQuery.getFeatures()) {
			expandedQuery.append(separator + term + "^" + feedbackQuery.getFeatureWeight(term));
			separator = " "; // add separator after first iteration
		}
		
		// FIXME: Regex?
		assertEquals("multiple^", expandedQuery.toString());
	}
}
