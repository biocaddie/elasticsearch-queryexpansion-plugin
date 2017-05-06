package org.elasticsearch.search.queryexpansion;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.nationaldataservice.elasticsearch.queryexpansion.HelloAction;
import org.nationaldataservice.elasticsearch.queryexpansion.HelloTransportAction;
import org.nationaldataservice.elasticsearch.queryexpansion.RestQueryExpansionAction;

public class QueryExpansionPlugin extends Plugin implements ActionPlugin {
	@Override
	public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
            IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
            IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
	       return Collections.singletonList(new RestQueryExpansionAction(settings, restController));
	}
	
	@Override
	public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
	    return Collections.singletonList(new ActionHandler<>(HelloAction.INSTANCE, HelloTransportAction.class));
	}
}
