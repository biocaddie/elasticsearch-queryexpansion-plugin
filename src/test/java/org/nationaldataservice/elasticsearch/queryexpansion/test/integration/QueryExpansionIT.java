package org.nationaldataservice.elasticsearch.queryexpansion.test.integration;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;

public class QueryExpansionIT extends AbstractITCase {
	@Test
    public void testCase() throws Exception {
        Response response = client.performRequest("GET", "/_nodes/plugins");
        System.out.println(response);
        
        HttpEntity entity = response.getEntity();
        System.out.println(entity);
        
        /*Map<String, Object> nodes = (Map<String, Object>) entityAsMap(response).get("nodes");
        for (String nodeName : nodes.keySet()) {
            boolean pluginFound = false;
            Map<String, Object> node = (Map<String, Object>) nodes.get(nodeName);
            List<Map<String, Object>> plugins = (List<Map<String, Object>>) node.get("plugins");
            for (Map<String, Object> plugin : plugins) {
                String pluginName = (String) plugin.get("name");
                if (pluginName.equals("ingest-bano")) {
                    pluginFound = true;
                    break;
                }
            }
            assertThat(pluginFound, is(true));
        }*/
    }
}