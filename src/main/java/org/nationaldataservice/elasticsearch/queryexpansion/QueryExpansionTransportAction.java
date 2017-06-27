package org.nationaldataservice.elasticsearch.queryexpansion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import edu.gslis.indexes.IndexWrapperLuceneImpl;
import edu.gslis.lucene.expansion.Rocchio;
import edu.gslis.lucene.main.config.RunQueryConfig;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;

public class QueryExpansionTransportAction extends HandledTransportAction<QueryExpansionRequest, QueryExpansionResponse> {
	private final Logger logger = ESLoggerFactory.getLogger(QueryExpansionTransportAction.class);

	private static String CONFIG_FILE_PATH = ".";
	private RunQueryConfig config = null;
	
    @Inject
    public QueryExpansionTransportAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                IndexNameExpressionResolver resolver, TransportService transportService) {
        super(settings, QueryExpansionAction.NAME, threadPool, transportService, actionFilters, resolver, QueryExpansionRequest::new);
        this.config = readConfig();
    }
    
    protected static RunQueryConfig readConfig() {
        File yamlFile = new File(QueryExpansionTransportAction.CONFIG_FILE_PATH);
        if(!yamlFile.exists()) {
            System.err.println("Configuration file not found.");
            System.exit(-1);
        }
        Yaml yaml = new Yaml(new Constructor(RunQueryConfig.class));
        try {
        	RunQueryConfig parsedConfig = (RunQueryConfig)yaml.load(new FileInputStream(yamlFile));
			return parsedConfig;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }
    
    @Override
    protected void doExecute(QueryExpansionRequest request, ActionListener<QueryExpansionResponse> listener) {
    	this.logger.info("Executing transport action!");
    	
        try {
            String name = request.getName();
            if (name == null) {
                name = "World";
            }
            
            String pathToIndex = ".";
            IndexWrapperLuceneImpl index = new IndexWrapperLuceneImpl(pathToIndex);
            
            GQuery query = new GQuery();
            query.setTitle("testquery");
            query.setText("Give me the moon");
            
            int fbDocs = 1;
            int fbTerms = 1;

            // Retrieve expanded query Rocchio(alpha, beta, k1, b)
            Rocchio rocchioFb = new Rocchio(1.0, 0.75, 1.2, 0.75);
            
            // WARNING: expandQuery() modifies the query parameter
            // TODO: it should probably return a new string, instead of modifying the parameter
            // XXX: this only works in Java, the same code would likely fail in C due ot calling conventions
            rocchioFb.expandQuery(index, query, fbDocs, fbTerms);
            
            // Perform search on expanded query
            SearchHits hits = index.runQuery(query, 1000, config.getSimilarity());
            
            // Send our response
            QueryExpansionResponse response = new QueryExpansionResponse();
            response.setMessage(hits.toString());
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
