package org.nationaldataservice.elasticsearch.rocchio.test.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Response;

public class RocchioIT extends AbstractITCase {
	private static final String TEST_EXPAND_INDEX = "biocaddie";
	private static final String TEST_SEARCH_INDEX = "biocaddie";
	private static final String TEST_TYPE = "dataset";
	private static final String TEST_FIELD = "_all";
	private static final int TEST_FB_TERMS = 10;
	private static final int TEST_FB_DOCS = 50;
	private static final double TEST_ALPHA = 0.5;
	private static final double TEST_BETA = 0.5;
	private static final double TEST_K1 = 1.2;
	private static final double TEST_B = 0.75;
	
	private static final Header contentTypeHeader = new BasicHeader("Content-Type", "application/json");
	
	private static final String defaultParameters = "?fbTerms=" + TEST_FB_TERMS + "&fbDocs=" + TEST_FB_DOCS;
	
	private static final String indexJson = "{\"mappings\":{\"dataset\":{\"_all\":{\"type\":\"text\",\"term_vector\":\"with_positions_offsets_payloads\",\"store\":true,\"analyzer\":\"fulltext_analyzer\"}}},\"settings\":{\"index\":{\"number_of_shards\":1,\"number_of_replicas\":0},\"analysis\":{\"analyzer\":{\"fulltext_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"whitespace\",\"filter\":[\"lowercase\",\"type_as_payload\"]}}}}}";
	private static final String doc1Json = "{\"DOCNO\":\"1\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Sinorhizobium meliloti\"}},\"dataItem\":{\"description\":\"We characterized transcriptomes of a strain overexpressing syrA.  Our work shows that the syrA transcriptome shares similar gene expression changes to the syrM and nodD3 transcriptomes and that nodD3 and syrA may be the only targets directly activated by SyrM.  We propose that most of the gene expression changes observed when nodD3 is overexpressed are due to NodD3 activation of syrM expression, which in turn stimulates SyrM activation of syrA expression.  The subsequent increase in SyrA abundance alters activity of the ChvI-ExoS-ExoR circuit, resulting in broad changes in gene expression. Gene expression profiling of Sinorhizobium meliloti overexpressing syrA was performed using custom Affymetrix GeneChips\",\"title\":\"The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA)\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520401\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"The Sinorhizobium meliloti SyrM regulon: effects on global gene expression are mediated by syrA and nodD3 (SyrA)\"}";
	private static final String doc2Json = "{\"DOCNO\":\"2\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Homo sapiens\"}},\"dataItem\":{\"description\":\"A study to define the binding loci of RelA-containing NF-kappaB dimers in a human myometrial smooth muscle cell line after exposure to TNF. Monolayers of PHM1-31 cells were exposed to TNF (10ng/ml) for 1 hour or left unstimulated. The Chromatin immunoprecipitation (ChIP) assay was performed to recover RelA-bound chromatin or non-specifically bound chromatin with IgG. That chromatin was prepared and used to probe Affymetrix GeneChIP 1.0R  Human Promoter arrays. Three biological replicates of each experiment were conducted. Datasets were subsequently analysed in Partek Genomics Suite V6.6 where baseline was normalised by subtraction of IgG values from conrresponding RelA-immunoprecipitated samples. Control samples immunoprecipitated with RelA were then compared with TNF-stimulated samples immunoprecipitated with RelA.\",\"title\":\"RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter)\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-05\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520482\",\"experimentType\":\"ChIP-chip by tiling array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"RelA Nuclear factor-kappaB (NF-kB) Subunit binding Loci in Promoter Regions of PHM1-31 Myometrial Smooth Muscle Cells (Promoter)\"}";
	private static final String doc3Json = "{\"DOCNO\":\"3\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Rattus norvegicus\"}},\"dataItem\":{\"description\":\"This SuperSeries is composed of the SubSeries listed below. Refer to individual Series\",\"title\":\"Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520420\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Aging-associated inflammatory and oxidative changes in the rat urinary bladder and dorsal root ganglia - preventive effect of caloric restriction\"}";
	private static final String doc4Json = "{\"DOCNO\":\"4\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Homo sapiens\"}},\"dataItem\":{\"description\":\"To reveal the effects of carnosine on Caco-2 cells, we have employed whole genome microarray to detect genes that showed significantly different expression when exposed to carnosine. Caco-2 cells were treated with 1 mM carnosine for 3 days. Caco-2 cells were treated with 1 mM carnosine for 3 days. Three independent experiments were performed.\",\"title\":\"Gene expression profile in Caco-2 cells treated with carnosine\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520441\",\"experimentType\":\"transcription profiling by array\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Gene expression profile in Caco-2 cells treated with carnosine\"}";
	private static final String doc5Json = "{\"DOCNO\":\"5\",\"METADATA\":{\"dataResource\":{\"keywords\":[],\"altNames\":[],\"acronyms\":[]},\"citation\":{\"count\":\"0\"},\"organism\":{\"experiment\":{\"species\":\"Mus musculus\"}},\"dataItem\":{\"description\":\"Mutations in methyl-CpG-binding protein 2 (MeCP2), a major epigenetic regulator, are the predominant cause of Rett syndrome. We previously found that Mecp2-null microglia are deficient in phagocytic ability, and that engraftment of wild-type monocytes into the brain of Mecp2-deficient mice attenuates pathology. We have observed that Mecp2 deficiency is associated with increased levels of histone acetylation at the cis-regulatory regions of the Mecp2-regulated genes in macrophages. We hypothesized that Mecp2 recruits protein complexes containing histone deacetylases (HDACs) to repress the expression of its target genes. Our ChIP-Seq studies in bone-marrow derived macrophages revealed that Mecp2 co-localizes with Ncor2/Hdac3 protein complex at cis-regulatory regions of the target genes. These results suggest a role for Mecp2 in the recruitment and regulation of Ncor2/Hdac3 repressosome that plays a critical role in the regulation of inflammatory responses in macrophages. Examination of NCOR2 and HDAC3 genome-wide location in bone-marrow derived macrophages.\",\"title\":\"Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq]\",\"releaseDate\":\"2015-03-31\",\"lastUpdateDate\":\"2015-04-04\",\"dataTypes\":[\"organism\",\"dataItem\",\"citation\"],\"ID\":\"520444\",\"experimentType\":\"ChIP-seq\"}},\"REPOSITORY\":\"arrayexpress_020916\",\"TITLE\":\"Mecp2: an unexpected regulator of macrophage gene expression and function [ChIP-Seq]\"}";
	
	private static final String indexEndpoint = String.format("/%s", TEST_EXPAND_INDEX);
	private static final String typeEndpoint = String.format("%s/%s", indexEndpoint, TEST_TYPE);
	private static final String expandEndpoint = String.format("%s/%s/_expand", indexEndpoint, TEST_TYPE);
	private static final String expandAndSearchEndpoint = String.format("%s/%s/_esearch", indexEndpoint, TEST_TYPE);
	
	
	private static void createIndex() {
		try {
			Map<String, String> params = new HashMap<String, String>();
			
			// Create our expand / search indices
			//System.out.println("Setup: Creating " + TEST_SEARCH_INDEX + " index...");
			//client.performRequest("PUT", "/" + TEST_SEARCH_INDEX, params, indexBody, contentTypeHeader);
			System.out.println("Setup: Creating " + TEST_EXPAND_INDEX + " index...");
			Response resp =client.performRequest("PUT", "/" + TEST_EXPAND_INDEX, params, new StringEntity(indexJson), contentTypeHeader);
			System.out.println("Response: " + entityAsMap(resp));
		
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			
			if (e instanceof UnsupportedEncodingException) {
				System.err.println("Error encoding JSON: " + e.getMessage());
				return;
			}
		}
	}
	
	private static void addDocument(Integer id, String jsonDocument) {
		System.out.println("Setup: Creating document #" + id.toString() + "...");
		try {
			String documentEndpoint = String.format("%s/%d", typeEndpoint, id);
			StringEntity requestBody = new StringEntity(jsonDocument);
			Map<String, String> params = new HashMap<String, String>();
			
			Response resp = client.performRequest("PUT", documentEndpoint, params, requestBody, contentTypeHeader);
			System.out.println("Response: " + entityAsMap(resp));

		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			
			if (e instanceof UnsupportedEncodingException) {
				System.err.println("Error encoding JSON: " + e.getMessage());
				return;
			}
		}
	}
	
	@BeforeClass
	public static void beforeAll() {
		System.out.println("Setting up test environment!");
		createIndex();
		
		addDocument(1, doc1Json);
		addDocument(2, doc2Json);
		addDocument(3, doc3Json);
		addDocument(4, doc4Json);
		addDocument(5, doc5Json);
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
		String params = defaultParameters + "&query=" + query;
		String request =  expandEndpoint + params;
		Response response = client.performRequest("GET", request);
		
		final String expectedResult = "{query=dorsal^0.008995920147034231 rat^0.6454347675122577 aging-associated^0.008995920147034231 root^0.008995920147034231 bladder^0.008995920147034231 effect^0.008995920147034231 oxidative^0.008995920147034231 urinary^0.008995920147034231 -^0.008995920147034231 preventive^0.008995920147034231}";
		assertEquals(expectedResult, entityAsMap(response).toString());
	}
	
	@Test
	public void testRocchioExpandWithSearch() throws Exception {
		String query = "rat";
		String params = defaultParameters + "&query=" + query;
		String request =  expandAndSearchEndpoint + params;
		Response response = client.performRequest("GET", request);
		System.out.println("Response: " + entityAsMap(response));
		
		//assertEquals();
	}
}