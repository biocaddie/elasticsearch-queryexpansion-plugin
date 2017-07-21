package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.cluster.ClusterModule;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public abstract class AbstractITCase  {
    protected static final Logger staticLogger = ESLoggerFactory.getLogger("it");
    protected final static int HTTP_TEST_PORT = 9400;
    protected static RestClient client;
    

    /**
     * Create a new {@link XContentParser}.
     */
    protected static XContentParser createParser(XContentBuilder builder) throws IOException {
        return builder.generator().contentType().xContent().createParser(xContentRegistry(), builder.bytes());
    }

    /**
     * Create a new {@link XContentParser}.
     */
    protected static XContentParser createParser(XContent xContent, String data) throws IOException {
        return xContent.createParser(xContentRegistry(), data);
    }

    /**
     * Create a new {@link XContentParser}.
     */
    protected static XContentParser createParser(XContent xContent, InputStream data) throws IOException {
        return xContent.createParser(xContentRegistry(), data);
    }

    /**
     * Create a new {@link XContentParser}.
     */
    protected static XContentParser createParser(XContent xContent, byte[] data) throws IOException {
        return xContent.createParser(xContentRegistry(), data);
    }

    /**
     * Create a new {@link XContentParser}.
     */
    protected static XContentParser createParser(XContent xContent, BytesReference data) throws IOException {
        return xContent.createParser(xContentRegistry(), data);
    }
    
    /**
     * The {@link NamedXContentRegistry} to use for this test. Subclasses should override and use liberally.
     */
    protected static NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(ClusterModule.getNamedXWriteables());
    }

	public static Map<String, Object> entityAsMap(Response response) throws UnsupportedOperationException, IOException {
        XContentType xContentType = XContentType.fromMediaTypeOrFormat(response.getEntity().getContentType().getValue());
        try (XContentParser parser = createParser(xContentType.xContent(), response.getEntity().getContent())) {
            return parser.map();
        }
	}
    
    @BeforeClass
    public static void startRestClient() {
        client = RestClient.builder(new HttpHost("localhost", HTTP_TEST_PORT)).build();
        try {
            Response response = client.performRequest("GET", "/");
            Map<String, Object> responseMap = entityAsMap(response);
            assertThat(responseMap, hasEntry("tagline", "You Know, for Search"));
            staticLogger.info("Integration tests ready to start... Cluster is running.");
        } catch (IOException e) {
            // If we have an exception here, let's ignore the test
            staticLogger.warn("Integration tests are skipped: [{}]", e.getMessage());
            assumeThat("Integration tests are skipped", e.getMessage(), not(containsString("Connection refused")));
            staticLogger.error("Full error is", e);
            fail("Something wrong is happening. REST Client seemed to raise an exception.");
        }
    }

    @AfterClass
    public static void stopRestClient() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }
        staticLogger.info("Stopping integration tests against an external cluster");
    }
}
