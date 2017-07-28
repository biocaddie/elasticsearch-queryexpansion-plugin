package org.nationaldataservice.elasticsearch.rocchio.test.unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.SearchHit;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsItemResponse;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
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
	/** The Rocchio instance to test */
	private Rocchio rocchio;

	// The common test parameter set (individual tests can still use one-off
	// values)
	private static final String TEST_EXPAND_INDEX = "biocaddie";
	private static final String TEST_SEARCH_INDEX = "biocaddie";
	private static final String TEST_QUERY = "multiple sclerosis";
	private static final String TEST_TYPE = "dataset";
	private static final String TEST_FIELD = "_all";
	private static final int TEST_FB_TERMS = 10;
	private static final int TEST_FB_DOCS = 50;
	private static final double TEST_ALPHA = 0.5;
	private static final double TEST_BETA = 0.5;
	private static final double TEST_K1 = 1.2;
	private static final double TEST_B = 0.75;

	// Mock out all of the ElasticSearch internals
	private static final Client client = mock(Client.class);

	@SuppressWarnings("unchecked")
	private static final ActionFuture<ClusterStateResponse> clusterStateFuture = (ActionFuture<ClusterStateResponse>) mock(
			ActionFuture.class);
	private static final AdminClient adminClient = mock(AdminClient.class);
	private static final ClusterAdminClient clusterAdminClient = mock(ClusterAdminClient.class);
	private static final ClusterState clusterState = mock(ClusterState.class);
	private static final ClusterStateResponse clusterStateResponse = mock(ClusterStateResponse.class);
	private static final MetaData clusterMetadata = mock(MetaData.class);
	private static final IndexMetaData mockIndexMetaData = mock(IndexMetaData.class);

	@SuppressWarnings("unchecked")
	private static final ListenableActionFuture<MultiTermVectorsResponse> mockMtvFuture = mock(
			ListenableActionFuture.class);
	private static final MultiTermVectorsResponse mockMtvResponse = mock(MultiTermVectorsResponse.class);
	private static final TermVectorsResponse mockTvResponse = mock(TermVectorsResponse.class);
	private static final MultiTermVectorsItemResponse mockMtvItemResponse = mock(MultiTermVectorsItemResponse.class);
	private static final MultiTermVectorsRequestBuilder mockMtvBuilder = mock(MultiTermVectorsRequestBuilder.class);
	private static final Fields mockFields = mock(Fields.class);
	private static final Terms mockTerms = mock(Terms.class);
	private static final MultiTermVectorsItemResponse[] mockMtvItemResponses = { mockMtvItemResponse };

	// FIXME: finish mocking out iterator and expand
	private static final TermsEnum mockIterator = mock(TermsEnum.class);

	@SuppressWarnings("unchecked")
	private static final ListenableActionFuture<SearchResponse> mockSearchFuture = mock(ListenableActionFuture.class);
	private static final SearchRequestBuilder srBuilder = mock(SearchRequestBuilder.class);
	private static final SearchResponse mockSearchResponse = mock(SearchResponse.class);

	// FIXME: Somehow get real values to return here for expand / search
	private static final SearchHits hits = mock(SearchHits.class);
	private static final SearchHit hit1 = mock(SearchHit.class);
	private static final SearchHit hit2 = mock(SearchHit.class);
	private static final SearchHit hit3 = mock(SearchHit.class);
	private static final SearchHit[] hitsArray = { hit1, hit2, hit3 };

	// The index mapping metadata and sub-mappings
	private static final MappingMetaData mockTypeMetadata = mock(MappingMetaData.class);
	private static final ImmutableOpenMap<String, MappingMetaData> indexMappingMetadata;
	private static final LinkedHashMap<String, Object> fieldPropertiesMap = new LinkedHashMap<String, Object>();
	private static final LinkedHashMap<String, Object> typePropertiesMap = new LinkedHashMap<String, Object>();
	private static final LinkedHashMap<String, Object> typeMap = new LinkedHashMap<String, Object>();
	private static final Map<String, MappingMetaData> typeMetadataMapping = new HashMap<>();

	// FIXME: finish mocking out iterator and expand
	private static final BytesRef ref = new BytesRef("asdf");

	static {
		// Build up our properties mapping: { "store": true } object
		fieldPropertiesMap.put("store", true);

		// Build up our test field mapping with the properties map
		typePropertiesMap.put(TEST_FIELD, fieldPropertiesMap);

		// Build up our test type mapping from the test field mapping
		typeMap.put("properties", typePropertiesMap);
		typeMap.put("_all", fieldPropertiesMap);

		// Build up our test type mapping of the type metadata
		typeMetadataMapping.put(TEST_TYPE, mockTypeMetadata);

		// Build up our index mapping from the type mapping
		indexMappingMetadata = new ImmutableOpenMap.Builder<String, MappingMetaData>().putAll(typeMetadataMapping)
				.build();

		try {
			// Mock out all expected ElasticSearch responses
			when(client.admin()).thenReturn(adminClient);
			when(adminClient.cluster()).thenReturn(clusterAdminClient);
			when(clusterAdminClient.state(any())).thenReturn(clusterStateFuture);
			when(clusterStateFuture.actionGet()).thenReturn(clusterStateResponse);
			when(clusterStateResponse.getState()).thenReturn(clusterState);
			when(clusterState.getMetaData()).thenReturn(clusterMetadata);
			when(clusterMetadata.index(anyString())).thenReturn(mockIndexMetaData);
			when(mockIndexMetaData.getMappings()).thenReturn(indexMappingMetadata);
			when(mockTypeMetadata.getSourceAsMap()).thenReturn(typeMap);

			// Mock out all expected ElasticSearch responses
			when(mockMtvBuilder.execute()).thenReturn(mockMtvFuture);
			when(mockMtvFuture.actionGet()).thenReturn(mockMtvResponse);
			when(mockMtvBuilder.add(any())).thenReturn(mockMtvBuilder);
			when(client.prepareMultiTermVectors()).thenReturn(mockMtvBuilder);
			when(mockMtvItemResponse.getResponse()).thenReturn(mockTvResponse);
			when(mockMtvResponse.getResponses()).thenReturn(mockMtvItemResponses);

			// FIXME: Mock out Lucene responses
			when(mockTvResponse.getFields()).thenReturn(mockFields);
			when(mockFields.terms(TEST_FIELD)).thenReturn(mockTerms);
			when(mockTerms.getDocCount()).thenReturn(10);
			when(mockTerms.getSumTotalTermFreq()).thenReturn(10L);
			when(mockTerms.iterator()).thenReturn(mockIterator);

			// FIXME: finish mocking out iterator and expand
			when(mockIterator.next()).thenReturn(null);
			when(mockIterator.totalTermFreq()).thenReturn(10L);
			when(mockIterator.docFreq()).thenReturn(10);
			when(mockIterator.term()).thenReturn(ref);

			// Mock building our SearchRequest
			when(client.prepareSearch(anyString())).thenReturn(srBuilder);
			when(srBuilder.setQuery(any(QueryStringQueryBuilder.class))).thenReturn(srBuilder);
			when(srBuilder.setSize(anyInt())).thenReturn(srBuilder);
			when(srBuilder.execute()).thenReturn(mockSearchFuture);

			// Mock performing our SearchRequest
			when(mockSearchFuture.actionGet()).thenReturn(mockSearchResponse);
			when(mockSearchResponse.getHits()).thenReturn(hits);

			// Mock parsing the search results
			when(hits.getHits()).thenReturn(hitsArray);
			when(hits.hits()).thenReturn(hitsArray);

			// FIXME: is this ok?
			when(hits.totalHits()).thenReturn(Long.valueOf(3));
			when(hits.getTotalHits()).thenReturn(Long.valueOf(3));
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	};

	@Before
	public void setUp() throws IOException {
		// Initialize our Rocchio implementation (not mocked)
		this.rocchio = new Rocchio(client, TEST_EXPAND_INDEX, TEST_TYPE, TEST_FIELD, TEST_ALPHA, TEST_BETA, TEST_K1, TEST_B);
	}

	@After
	public void tearDown() {
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
		/*StringBuffer expandedQuery = new StringBuffer();
		String separator = ""; // start out with no separator
		for (String term : feedbackQuery.getFeatures()) {
			expandedQuery.append(separator + term + "^" + feedbackQuery.getFeatureWeight(term));
			separator = " "; // add separator after first iteration
		}*/

		// FIXME: Regex?
		assertEquals("multiple^", feedbackQuery.toString());
	}
}
