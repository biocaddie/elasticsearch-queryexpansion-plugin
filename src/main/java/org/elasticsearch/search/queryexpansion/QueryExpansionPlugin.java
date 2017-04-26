package org.elasticsearch.search.queryexpansion;

import java.util.Collections;
import java.util.List;

import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.nationaldataservice.elasticsearch.queryexpansion.RestQueryExpansionAction;

public class QueryExpansionPlugin extends Plugin implements ActionPlugin {
	public List<Class<? extends RestHandler>> getRestHandlers() {
	       return Collections.singletonList(RestQueryExpansionAction.class);
	}
}
