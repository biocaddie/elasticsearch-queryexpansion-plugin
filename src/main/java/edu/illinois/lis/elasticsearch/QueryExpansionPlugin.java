package edu.illinois.lis.elasticsearch;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

public class QueryExpansionPlugin extends AbstractPlugin {
	@Override
	public String description() {
		return "Query expansion";
	}

	@Override
	public String name() {
		return "query-expansion";
	}


    /* Invoked on component assembly. */
    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestQueryExpansionAction.class);
        //restModule.addRestAction(ListAlgorithmsAction.RestListAlgorithmsAction.class);
    }
}
