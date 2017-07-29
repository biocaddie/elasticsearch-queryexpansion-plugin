package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.logging.ESLoggerFactory;

/**
 * This is a simple integration test suite for the ElasticSearch Rocchio
 * Plugin.Use these test cases to verify correctness of the API endpoint, input
 * validation, compare performance, scale testing, etc <br/>
 * Before the test suite runs, the test runner will:
 * 
 * <pre>
 *    * Download ElasticSearch binaries
 *    * Install the ElasticSearch Rocchio Plugin
 *    * Start up an ElasticSearch cluster
 *    * Ensure that the TEST_INDEX has been created
 *    * Ensure that TEST_INDEX contains some test documents
 *    * Run the set of test cases
 *    * Tear down the cluster
 * </pre>
 * 
 * @see {@link AbstractITCase}
 * @see src/test/ant/integration-tests.xml
 * 
 * @author lambert8
 *
 */
public class RocchioIT extends AbstractITCase {
	private static final Logger staticLogger = ESLoggerFactory.getLogger(RocchioIT.class);

	// The common test parameter set (individual tests can still use one-off
	// values)
	private static final String TEST_INDEX = "biocaddie";
	private static final String TEST_TYPE = "dataset";
	private static final int TEST_FB_TERMS = 10;
	private static final int TEST_FB_DOCS = 5;
	
	private final String defaultEndpointParameters = "fbTerms=" + TEST_FB_TERMS + "&fbDocs=" + TEST_FB_DOCS;
	private final String expandEndpoint = String.format("/%s/%s/_expand?%s", TEST_INDEX, TEST_TYPE,
			defaultEndpointParameters);

	// TODO: Improve expectations
	private final String EXPECTED_EXPANDED_QUERY_OBJECT = "{query=dorsal^0.09029725274935405 rat^0.7267361001145776 aging-associated^0.09029725274935405 root^0.09029725274935405 bladder^0.09029725274935405 effect^0.09029725274935405 ganglia^0.09029725274935405 oxidative^0.09029725274935405 urinary^0.09029725274935405 preventive^0.09029725274935405}";
	private final String EXPECTED_EXPANDED_QUERY_STRING = "dorsal^0.09029725274935405 rat^0.7267361001145776 aging-associated^0.09029725274935405 root^0.09029725274935405 bladder^0.09029725274935405 effect^0.09029725274935405 ganglia^0.09029725274935405 oxidative^0.09029725274935405 urinary^0.09029725274935405 preventive^0.09029725274935405";
	private final String EXPECTED_SEARCH_HITS = "{_shards={total=1, failed=0, successful=1}, hits={hits=[{_index=biocaddie, _type=dataset, _source={DOCNO=1, REPOSITORY=arrayexpress_020916, TITLE=The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA), METADATA={dataItem={dataTypes=[organism, dataItem, citation], releaseDate=2015-03-31, lastUpdateDate=2015-04-04, description=We characterized transcriptomes of a strain overexpressing syrA.  Our work shows that the syrA transcriptome shares similar gene expression changes to the syrM and nodD3 transcriptomes and that nodD3 and syrA may be the only targets directly activated by SyrM.  We propose that most of the gene expression changes observed when nodD3 is overexpressed are due to NodD3 activation of syrM expression, which in turn stimulates SyrM activation of syrA expression.  The subsequent increase in SyrA abundance alters activity of the ChvI-ExoS-ExoR circuit, resulting in broad changes in gene expression. Gene expression profiling of Sinorhizobium meliloti overexpressing syrA was performed using custom Affymetrix GeneChips, ID=520401, title=The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA), experimentType=transcription profiling by array}, organism={experiment={species=Sinorhizobium meliloti}}, citation={count=0}, dataResource={altNames=[], acronyms=[], keywords=[]}}}, _id=1, _score=1.0}, {_index=biocaddie, _type=dataset, _source={DOCNO=2, REPOSITORY=arrayexpress_020916, TITLE=RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter), METADATA={dataItem={dataTypes=[organism, dataItem, citation], releaseDate=2015-03-31, lastUpdateDate=2015-04-05, description=A study to define the binding loci of RelA-containing NF-kappaB dimers in a human myometrial smooth muscle cell line after exposure to TNF. Monolayers of PHM1-31 cells were exposed to TNF (10ng/ml) for 1 hour or left unstimulated. The Chromatin immunoprecipitation (ChIP) assay was performed to recover RelA-bound chromatin or non-specifically bound chromatin with IgG. That chromatin was prepared and used to probe Affymetrix GeneChIP 1.0R  Human Promoter arrays. Three biological replicates of each experiment were conducted. Datasets were subsequently analysed in Partek Genomics Suite V6.6 where baseline was normalised by subtraction of IgG values from conrresponding RelA-immunoprecipitated samples. Control samples immunoprecipitated with RelA were then compared with TNF-stimulated samples immunoprecipitated with RelA., ID=520482, title=RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter), experimentType=ChIP-chip by tiling array}, organism={experiment={species=Homo sapiens}}, citation={count=0}, dataResource={altNames=[], acronyms=[], keywords=[]}}}, _id=2, _score=1.0}, {_index=biocaddie, _type=dataset, _source={DOCNO=3, REPOSITORY=arrayexpress_020916, TITLE=Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction, METADATA={dataItem={dataTypes=[organism, dataItem, citation], releaseDate=2015-03-31, lastUpdateDate=2015-04-04, description=This SuperSeries is composed of the SubSeries listed below. Refer to individual Series, ID=520420, title=Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction, experimentType=transcription profiling by array}, organism={experiment={species=Rattus norvegicus}}, citation={count=0}, dataResource={altNames=[], acronyms=[], keywords=[]}}}, _id=3, _score=1.0}, {_index=biocaddie, _type=dataset, _source={DOCNO=4, REPOSITORY=arrayexpress_020916, TITLE=Gene expression profile in Caco-2 cells treated with carnosine, METADATA={dataItem={dataTypes=[organism, dataItem, citation], releaseDate=2015-03-31, lastUpdateDate=2015-04-04, description=To reveal the effects of carnosine on Caco-2 cells, we have employed whole genome microarray to detect genes that showed significantly different expression when exposed to carnosine. Caco-2 cells were treated with 1 mM carnosine for 3 days. Caco-2 cells were treated with 1 mM carnosine for 3 days. Three independent experiments were performed., ID=520441, title=Gene expression profile in Caco-2 cells treated with carnosine, experimentType=transcription profiling by array}, organism={experiment={species=Homo sapiens}}, citation={count=0}, dataResource={altNames=[], acronyms=[], keywords=[]}}}, _id=4, _score=1.0}, {_index=biocaddie, _type=dataset, _source={DOCNO=5, REPOSITORY=arrayexpress_020916, TITLE=Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq], METADATA={dataItem={dataTypes=[organism, dataItem, citation], releaseDate=2015-03-31, lastUpdateDate=2015-04-04, description=Mutations in methyl-CpG-binding protein 2 (MeCP2), a major epigenetic regulator, are the predominant cause of Rett syndrome. We previously found that Mecp2-null microglia are deficient in phagocytic ability, and that engraftment of wild-type monocytes into the brain of Mecp2-deficient mice attenuates pathology. We have observed that Mecp2 deficiency is associated with increased levels of histone acetylation at the cis-regulatory regions of the Mecp2-regulated genes in macrophages. We hypothesized that Mecp2 recruits protein complexes containing histone deacetylases (HDACs) to repress the expression of its target genes. Our ChIP-Seq studies in bone-marrow derived macrophages revealed that Mecp2 co-localizes with Ncor2/Hdac3 protein complex at cis-regulatory regions of the target genes. These results suggest a role for Mecp2 in the recruitment and regulation of Ncor2/Hdac3 repressosome that plays a critical role in the regulation of inflammatory responses in macrophages. Examination of NCOR2 and HDAC3 genome-wide location in bone-marrow derived macrophages., ID=520444, title=Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq], experimentType=ChIP-seq}, organism={experiment={species=Mus musculus}}, citation={count=0}, dataResource={altNames=[], acronyms=[], keywords=[]}}}, _id=5, _score=1.0}], total=5, max_score=1.0}, took=1, timed_out=false}";
	
	@BeforeClass
	public static void setUp() {
		// Ensure that the index exists
		staticLogger.info("Setting up test environment!");
		createIndex(TEST_INDEX);

		// Ensure that documents to the index
		for (int i = 1; i <= 5; i++) {
			addDocument(TEST_INDEX, TEST_TYPE, i, DOCUMENTS_JSON[i - 1]);
		}

		// Tests will fail if we don't wait for ES to index the new documents
		staticLogger.info("Waiting for ES to finish indexing documents...");
		wait(3000);
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
				if (pluginName.equals("rocchio")) {
					pluginFound = true;
					break;
				}
			}
			assertThat(pluginFound, is(true));
		}
	}

	@Test
	public void testExpandEndpoint() throws Exception {
		String query = "rat";
		String params = "&query=" + query;
		String request = expandEndpoint + params;
		
		Response response = client.performRequest("GET", request);
		assertEquals(EXPECTED_EXPANDED_QUERY_OBJECT, entityAsMap(response).toString());
	}

	// FIXME: Test case currently fails (see below)
	//@Test
	/** Compare performance and  */
	public void testSearchPerformance() throws Exception {
		String indexRequest = "/" + TEST_INDEX;
		Response indicesResponse = client.performRequest("GET", indexRequest, contentTypeHeader);
		staticLogger.info(entityAsMap(indicesResponse).toString());
		String query = "rat";
		String searchEndpoint = "/" + TEST_INDEX + "/_search";
		
		// Time a normal (unexpanded) search for our query
		long searchStart = System.nanoTime();
		Response unexpandedSearchResponse = client.performRequest("GET", searchEndpoint + "?q=" + query, contentTypeHeader);
		long searchDuration = System.nanoTime() - searchStart;

		// Time a query expansion
		String expandParams =  "&query=" + query;
		String expandRequest = expandEndpoint + expandParams;
		long expandStart = System.nanoTime();
		Response expandResponse = client.performRequest("GET", expandRequest);
		long expandDuration = System.nanoTime() - expandStart;
		
		// Verify that expansion returns correctly
		String expandedQuery = entityAsMap(expandResponse).get("query").toString();
		assertEquals(EXPECTED_EXPANDED_QUERY_STRING, expandedQuery);

		// FIXME: Test currently fails on this syntax, stating that " " is an
		// invalid character. I have attempt to use "+", as well as "%20" with
		// no luck yet. I even tried to send the query as the request body, 
		// but struggled to find the correct syntax
		//StringEntity expandedSearchRequestBody = new StringEntity("{\"query\":\"" + expandedQuery.trim() + "\"}");
		String expandedSearchQueryString = "?q=" + expandedQuery.trim().replaceAll(" ", "+");
		
		// Time an expanded search on the same query
		long expandedSearchStart = System.nanoTime();
		Response expandedSearchResponse = client.performRequest("GET", searchEndpoint + expandedSearchQueryString, contentTypeHeader);
		long expandedSearchDuration = System.nanoTime() - expandedSearchStart;
		long fullExpansionDuration = expandDuration + expandedSearchDuration;

		// Log expansion results
		staticLogger.info(String.format("Original query: %s", query));
		staticLogger.info(String.format("Expanded query: %s", expandedQuery));
		
		// Log timings
		staticLogger.info(String.format("Query expansion took: %d ns", expandDuration));
		staticLogger.info(String.format("Expanded search took: %d ns", expandedSearchDuration));
		staticLogger.info(String.format("Full expansion + search took: %d ns", fullExpansionDuration));
		staticLogger.info(String.format("Unexpanded search took: %d ns", searchDuration));
		
		// Verify that expanded search returns as expected
		assertEquals(EXPECTED_SEARCH_HITS, entityAsMap(expandedSearchResponse).toString());
		
		// TODO: Analyze expanded results for accuracy?
		//staticLogger.info(String.format("Unexpanded search results: %s", entityAsMap(unexpandedSearchResponse)));
		//staticLogger.info(String.format("Expanded search results: %s", entityAsMap(expandedSearchResponse)));
	}
}