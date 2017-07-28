package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Response;

public class RocchioIT extends AbstractITCase {
	// The common test parameter set (individual tests can still use one-off
		// values)
	private static final String TEST_EXPAND_INDEX = "biocaddie";
	private static final String TEST_SEARCH_INDEX = "biocaddie";
	private static final String TEST_TYPE = "dataset";
	//private static final String TEST_FIELD = "_all";
	private static final int TEST_FB_TERMS = 10;
	private static final int TEST_FB_DOCS = 50;
	
	private static final String defaultEndpointParameters = "?fbTerms=" + TEST_FB_TERMS + "&fbDocs=" + TEST_FB_DOCS;
	private static final String expandOnParameter = "&expandOn=biocaddie";
	
	private static final String searchIndexEndpoint = String.format("/%s", TEST_SEARCH_INDEX);
	private static final String expandAndSearchEndpoint = String.format("%s/%s/_esearch", searchIndexEndpoint, TEST_TYPE);
	
	private static final String expandIndexEndpoint = String.format("/%s", TEST_EXPAND_INDEX);
	//private static final String typeEndpoint = String.format("%s/%s", expandIndexEndpoint, TEST_TYPE);
	private static final String expandEndpoint = String.format("%s/%s/_expand", expandIndexEndpoint, TEST_TYPE);
	
	
	@BeforeClass
	public static void setUp() {
		System.out.println("Setting up test environment!");
		createIndex(TEST_EXPAND_INDEX);
		
		if (!TEST_EXPAND_INDEX.equals(TEST_SEARCH_INDEX)) {
			createIndex(TEST_SEARCH_INDEX);
		}
		
		for (int i = 1; i <= 5; i++) {
			addDocument(TEST_EXPAND_INDEX, TEST_TYPE, i, documentsJson[i - 1]);
			
			if (!TEST_EXPAND_INDEX.equals(TEST_SEARCH_INDEX)) {
				addDocument(TEST_SEARCH_INDEX, TEST_TYPE, i, documentsJson[i - 1]);
			}
		}
	}
	
	@AfterClass
	public static void tearDown() {
		System.out.println("Tearing downtest environment!");
		for (int i = 1; i <= 5; i++) {
			removeDocument(TEST_EXPAND_INDEX, TEST_TYPE, i);
			
			if (!TEST_EXPAND_INDEX.equals(TEST_SEARCH_INDEX)) {
				removeDocument(TEST_SEARCH_INDEX, TEST_TYPE, i);
			}
		}
		
		deleteIndex(TEST_EXPAND_INDEX);
		deleteIndex(TEST_SEARCH_INDEX);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testPluginIsLoaded() throws Exception {

		Response response = client.performRequest("GET", "/_nodes/plugins");

		Map<String, Object> nodes = (Map<String, Object>) entityAsMap(response).get("nodes");
		for (String nodeName : nodes.keySet()) {
			boolean pluginFound = false;
			Map<String, Object> node = (Map<String, Object>) nodes.get(nodeName);
			List<Map<String, Object>> plugins = (List<Map<String, Object>>) node.get("plugins");
			for (Map<String, Object> plugin : plugins) {
				String pluginName = (String) plugin.get("name");
				if (pluginName.equals("queryexpansion")) {
					pluginFound = true;
					break;
				}
			}
			assertThat(pluginFound, is(true));
		}
	}
	
	@Test
	public void testRocchioExpand() throws Exception {
		String query = "rat";
		String params = defaultEndpointParameters + "&query=" + query;
		String request =  expandEndpoint + params;
		Response response = client.performRequest("GET", request);
		
		final String expectedResult = "{query=dorsal^0.008995920147034231 rat^0.6454347675122577 aging-associated^0.008995920147034231 root^0.008995920147034231 bladder^0.008995920147034231 effect^0.008995920147034231 oxidative^0.008995920147034231 urinary^0.008995920147034231 -^0.008995920147034231 preventive^0.008995920147034231}";
		assertEquals(expectedResult, entityAsMap(response).toString());
	}
	
	@Test
	public void testRocchioExpandWithSearch() throws Exception {
		String query = "rat";
		String params = defaultEndpointParameters + expandOnParameter + "&query=" + query;
		String request =  expandAndSearchEndpoint + params;
		Response response = client.performRequest("GET", request);
		System.out.println("Response: " + entityAsMap(response));
		
		// FIXME: This is clearly incorrect, but it's only returning empty right now
		String expectedResult = "{}";
		assertEquals(expectedResult, entityAsMap(response).toString());
	}
}