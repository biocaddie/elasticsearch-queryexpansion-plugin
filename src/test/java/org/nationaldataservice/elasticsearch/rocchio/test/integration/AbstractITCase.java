package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
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

	protected static final Header contentTypeHeader = new BasicHeader("Content-Type", "application/json");

	protected static final String indexJson = "{\"mappings\":{\"dataset\":{\"_all\":{\"type\":\"text\",\"term_vector\":\"with_positions_offsets_payloads\",\"store\":true,\"analyzer\":\"fulltext_analyzer\"}}},\"settings\":{\"index\":{\"number_of_shards\":1,\"number_of_replicas\":0},\"analysis\":{\"analyzer\":{\"fulltext_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"lowercase\",\"type_as_payload\"]}}}}}";
	protected static final String[] documentsJson = { 
		"{\"DOCNO\":\"1\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Sinorhizobium meliloti\"}},\"dataItem\":{\"description\":\"We characterized transcriptomes of a strain overexpressing syrA.  Our work shows that the syrA transcriptome shares similar gene expression changes to the syrM and nodD3 transcriptomes and that nodD3 and syrA may be the only targets directly activated by SyrM.  We propose that most of the gene expression changes observed when nodD3 is overexpressed are due to NodD3 activation of syrM expression, which in turn stimulates SyrM activation of syrA expression.  The subsequent increase in SyrA abundance alters activity of the ChvI-ExoS-ExoR circuit, resulting in broad changes in gene expression. Gene expression profiling of Sinorhizobium meliloti overexpressing syrA was performed using custom Affymetrix GeneChips\",\"title\":\"The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA)\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520401\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA)\"}",
		"{\"DOCNO\":\"2\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Homo sapiens\"}},\"dataItem\":{\"description\":\"A study to define the binding loci of RelA-containing NF-kappaB dimers in a human myometrial smooth muscle cell line after exposure to TNF. Monolayers of PHM1-31 cells were exposed to TNF (10ng/ml) for 1 hour or left unstimulated. The Chromatin immunoprecipitation (ChIP) assay was performed to recover RelA-bound chromatin or non-specifically bound chromatin with IgG. That chromatin was prepared and used to probe Affymetrix GeneChIP 1.0R  Human Promoter arrays. Three biological replicates of each experiment were conducted. Datasets were subsequently analysed in Partek Genomics Suite V6.6 where baseline was normalised by subtraction of IgG values from conrresponding RelA-immunoprecipitated samples. Control samples immunoprecipitated with RelA were then compared with TNF-stimulated samples immunoprecipitated with RelA.\",\"title\":\"RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter)\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-05\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520482\",\"experimentType\":\"ChIP-chip by tiling array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter)\"}",
		"{\"DOCNO\":\"3\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Rattus norvegicus\"}},\"dataItem\":{\"description\":\"This SuperSeries is composed of the SubSeries listed below. Refer to individual Series\",\"title\":\"Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520420\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction\"}",
		"{\"DOCNO\":\"4\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Homo sapiens\"}},\"dataItem\":{\"description\":\"To reveal the effects of carnosine on Caco-2 cells, we have employed whole genome microarray to detect genes that showed significantly different expression when exposed to carnosine. Caco-2 cells were treated with 1 mM carnosine for 3 days. Caco-2 cells were treated with 1 mM carnosine for 3 days. Three independent experiments were performed.\",\"title\":\"Gene expression profile in Caco-2 cells treated with carnosine\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520441\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Gene expression profile in Caco-2 cells treated with carnosine\"}",
		"{\"DOCNO\":\"5\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Mus musculus\"}},\"dataItem\":{\"description\":\"Mutations in methyl-CpG-binding protein 2 (MeCP2), a major epigenetic regulator, are the predominant cause of Rett syndrome. We previously found that Mecp2-null microglia are deficient in phagocytic ability, and that engraftment of wild-type monocytes into the brain of Mecp2-deficient mice attenuates pathology. We have observed that Mecp2 deficiency is associated with increased levels of histone acetylation at the cis-regulatory regions of the Mecp2-regulated genes in macrophages. We hypothesized that Mecp2 recruits protein complexes containing histone deacetylases (HDACs) to repress the expression of its target genes. Our ChIP-Seq studies in bone-marrow derived macrophages revealed that Mecp2 co-localizes with Ncor2/Hdac3 protein complex at cis-regulatory regions of the target genes. These results suggest a role for Mecp2 in the recruitment and regulation of Ncor2/Hdac3 repressosome that plays a critical role in the regulation of inflammatory responses in macrophages. Examination of NCOR2 and HDAC3 genome-wide location in bone-marrow derived macrophages.\",\"title\":\"Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq]\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520444\",\"experimentType\":\"ChIP-seq\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq]\"}"			
	};

    protected static void createIndex(String indexName) {
		try {
			Map<String, String> params = new HashMap<String, String>();
			
			// Create our expand / search indices
			System.out.println("Setup: Creating " + indexName + " index...");
			StringEntity requestBody = new StringEntity(indexJson);
			Response resp = client.performRequest("PUT", "/" + indexName, params, requestBody, contentTypeHeader);
			System.out.println("Response: " + resp.getStatusLine());
		
		} catch (IOException e) {
			// Ignore this...? probably already exists
			System.err.println(e.getMessage());
			//e.printStackTrace();
			
			if (e instanceof UnsupportedEncodingException) {
				System.err.println("Error encoding JSON: " + e.getMessage());
				return;
			}
		}
	}
	
	protected static void addDocument(String indexName, String typeName, Integer id, String jsonDocument) {
		System.out.println("Setup: Creating document #" + id.toString() + "...");
		try {
			String documentEndpoint = String.format("/%s/%s/%d", indexName, typeName, id);
			StringEntity requestBody = new StringEntity(jsonDocument);
			Map<String, String> params = new HashMap<String, String>();
			
			Response resp = client.performRequest("PUT", documentEndpoint, params, requestBody, contentTypeHeader);
			System.out.println("Response: " + resp.getStatusLine());

		} catch (IOException e) {
			// Ignore this...? probably already exists
			System.err.println(e.getMessage());
			//e.printStackTrace();
			
			if (e instanceof UnsupportedEncodingException) {
				System.err.println("Error encoding JSON: " + e.getMessage());
				return;
			}
		}
	}
	
	protected static void removeDocument(String indexName, String typeName, int id) {
		try {
			String documentEndpoint = String.format("/%s/%s/%d", indexName, typeName, id);
			Response resp = client.performRequest("DELETE", documentEndpoint, contentTypeHeader);
			System.out.println("Response: " + resp.getStatusLine());
		} catch (IOException e) {
			// Ignore this...? probably already deleted
			System.err.println(e.getMessage());
			//e.printStackTrace();
		}
	}
	
	protected static void deleteIndex(String indexName) {
		try {
			String indexEndpoint = String.format("/%s", indexName);
			Response resp = client.performRequest("DELETE", indexEndpoint, contentTypeHeader);
			System.out.println("Response: " + resp.getStatusLine());
		} catch (IOException e) {
			// Ignore this...? probably already deleted
			System.err.println(e.getMessage());
			//e.printStackTrace();
		}
	}

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
