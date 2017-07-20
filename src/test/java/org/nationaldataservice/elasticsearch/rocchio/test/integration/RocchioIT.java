package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.test.ESIntegTestCase;

public class RocchioIT extends AbstractITCase {
	/*
	 * @Test public void testCase() throws Exception { Response response =
	 * client.performRequest("GET", "/_cat/plugins");
	 * System.out.println(response);
	 * 
	 * HttpEntity entity = response.getEntity(); System.out.println(entity); }
	 */

	@Test
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
}