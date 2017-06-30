package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.elasticsearch.client.Response;

public class RocchioIT extends AbstractIntegTestCase {
	@Test
    public void testCase() throws Exception {
        Response response = client.performRequest("GET", "/_cat/plugins");
        System.out.println(response);
        
        HttpEntity entity = response.getEntity();
        System.out.println(entity);
    }
}