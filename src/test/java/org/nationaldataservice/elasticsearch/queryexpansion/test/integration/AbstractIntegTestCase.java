package org.nationaldataservice.elasticsearch.queryexpansion.test.integration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.BeforeClass;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.queryexpansion.QueryExpansionPlugin;

public abstract class AbstractIntegTestCase extends ESIntegTestCase {
    protected static final Logger staticLogger = ESLoggerFactory.getLogger("it");
    protected final static int HTTP_TEST_PORT = 9400;
    protected static RestClient client;
    
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(QueryExpansionPlugin.class);
    }

    @BeforeClass
    public static void startRestClient() {
        client = RestClient.builder(new HttpHost("localhost", HTTP_TEST_PORT)).build();
       
        Response response;
		try {
			response = client.performRequest("GET", "/");
            System.out.println(response.getEntity().getContent().toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
