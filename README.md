# Rocchio expansion for ElasticSearch

<img src="https://github.com/craig-willis/ndslabs/blob/master/docs/images/logos/NDS-badge.png" width="100" alt="NDS"> <img src="https://biocaddie.org/sites/default/files/biocaddie-logo.png" alt="bioCADDIE">

This is a prototype plugin for ElasticSearch 5.x to add Rocchio-based query expansion support with BM25 similarity. This plugin adds an ``_expand`` REST endpoint to ElasticSearch that returns a "query string query" with Lucene-style terms weights. This plugin was developed as part of the  NDS [bioCADDIE pilot](https://biocaddie.org/expansion-models-biomedical-data-search).

## Why Rocchio?
Our original goal was to implement relevance model (RM) based expansion using Lucene's language modeling similarity implementations. Our investigations revealed that [Lucene's language modeling implementation is incomplete](https://issues.apache.org/jira/browse/LUCENE-5847) and may not be suitable for use with RM. Given Lucene's origins as a vector-space implementation and current default BM25 scorer, we opted to instead implement Rocchio-style expansion.

## REST Interface

Endpoint:  
``/index/_expand``

Parameters:
* ``type``: Document type, defaults to ``dataset``
* ``field``: Field to search, defaults to ``_all``
* ``alpha``: Original query weight, defaults to 0.5
* ``beta``: Feedback query weight, defaults to 0.5
* ``k1``: BM25 k1 parameter, defaults to 1.2
* ``b``: BM25 b parameter, defaults to 0.75
* ``fbDocs``: Number of feedback documents, defaults to 10
* ``fbTerms``: Number of feedback terms, defaults to 10
* ``stoplist``: Additional stoplist terms (modifies primary stoplist)
* ``query``:  Query to expand

The expand endpoint returns a JSON object with the expanded query in "query string query" format with each expansion term and the associated expansion weight:
```
{
    "query":  "term1^weight1 term2^weight2 ..."
}
```


## Prerequisites

* ElasticSearch 5.3.2 (native or via Docker)
* Git + Maven (native or via Docker)
* ElasticSearch index

## Installing from OSSRH
You can install the plugin using the following command:
```bash
bin/elasticsearch-plugin install https://oss.sonatype.org/content/repositories/snapshots/edu/illinois/lis/queryexpansion/5.3.2-SNAPSHOT/queryexpansion-5.3.2-20170726.231658-1.zip
```

NOTE: You can check https://oss.sonatype.org/content/repositories/snapshots/edu/illinois/lis/queryexpansion/5.3.2-SNAPSHOT for a link to the newest `.zip` file.

## Building From Source
Clone this repository:
```bash
git clone nds-org/elasticsearch-queryexpansion-plugin queryexpansion && cd queryexpansion 
mvn package
bin/elasticsearch-plugin install file:///path/to/elasticsearch-queryexpansion-plugin/target/releases/queryexpansion-5.3.2-SNAPSHOT.zip
```


##  Example usage

The repository includes several scripts demonstrating how to install and use the plugin via Docker:

1. [Setup](README.md#setup)
2. [Build](README.md#build)
3. [Load](README.md#load)
4. [Test](README.md#test)

### Setup
The following steps demonstrate how to build an ElasticSearch index from the bioCADDIE test collection.

Make sure that the biocaddie benchmark test dataset exists somewhere on disk:
```bash
cd $HOME
wget https://biocaddie.org/sites/default/files/update_json_folder.zip && unzip update_json_folder.zip
```

Start ElasticSearch or run ElasticSearch 5.3.2 via Docker using the helper script:
```bash
./scripts/start.sh
```

Create an index with the required parameters (store==true):
```bash
./scripts/create-index.sh
```

NOTE: You may need to modify *dataset_path* in `./scripts/add-docs.sh` if your benchmark data is not located within `$HOME`.

Finally, use the helper script to add the documents to the index:
```bash
./scripts/add-docs.sh
```

NOTE: Indexing the full benchmark set can take a long time. If you only need a small subset of the documents, you can always `Ctrl+C` once you get the desired number of records indexed.

### Build
The following helper script will build the plugin using Maven (or using Docker if Maven is not installed):
```bash
./scripts/build.sh
```
Either way, the build should produce a `target/releases/` directory with the necessary `.zip` file.

The `.zip` that ElasticSearch needs should be found at `./target/releases/rocchio-0.0.1-SNAPSHOT.jar`.

### Load
Once the artifacts are built, we just need to install them and restart ElasticSearch. The following helper scripts assume that you are running ElasticSearch via Docker:
```bash
./scripts/install.sh
./scripts/restart.sh
```

### Test
You should now be able to test the new endpoint using the helper script or via raw `curl`:
```bash
$ ./test.sh
{"query":"sclerosis^2.798773920190095 study^0.4716440174771813 disease^0.584064093901503 or^0.3394485958568884 patients^0.79730633189081 multiple^1.941784058395449 was^0.4222225922753828 is^0.38702376034952857 to^0.4432445617796595 on^0.3817563584164061"}
```

You can check the container logs to see what happened under the covers:
```bash
$ ./logs.sh
...
[2017-07-01T04:54:54,007][INFO ][o.e.p.PluginsService     ] [lmIsnX7] loaded module [reindex]
[2017-07-01T04:54:54,008][INFO ][o.e.p.PluginsService     ] [lmIsnX7] loaded module [transport-netty3]
[2017-07-01T04:54:54,008][INFO ][o.e.p.PluginsService     ] [lmIsnX7] loaded module [transport-netty4]
[2017-07-01T04:54:54,009][INFO ][o.e.p.PluginsService     ] [lmIsnX7] loaded plugin [queryexpansion]
[2017-07-01T04:54:54,009][INFO ][o.e.p.PluginsService     ] [lmIsnX7] loaded plugin [x-pack]
[2017-07-01T04:55:00,722][INFO ][o.e.n.Node               ] initialized
[2017-07-01T04:55:00,744][INFO ][o.e.n.Node               ] [lmIsnX7] starting ...
[2017-07-01T04:55:01,467][WARN ][i.n.u.i.MacAddressUtil   ] Failed to find a usable hardware address from the network interfaces; using random bytes: f8:2c:c0:8c:3e:88:3b:3b
[2017-07-01T04:55:01,695][INFO ][o.e.t.TransportService   ] [lmIsnX7] publish_address {127.0.0.1:9300}, bound_addresses {127.0.0.1:9300}
[2017-07-01T04:55:02,082][INFO ][o.e.m.j.JvmGcMonitorService] [lmIsnX7] [gc][1] overhead, spent [260ms] collecting in the last [1s]
[2017-07-01T04:55:05,179][INFO ][o.e.c.s.ClusterService   ] [lmIsnX7] new_master {lmIsnX7}{lmIsnX7NRH2_Vmq6avBitQ}{iyWg9zTcQqCeF97xX-hdJQ}{127.0.0.1}{127.0.0.1:9300}, reason: zen-disco-elected-as-master ([0] nodes joined)
[2017-07-01T04:55:05,305][INFO ][o.e.x.s.t.n.SecurityNetty4HttpServerTransport] [lmIsnX7] publish_address {172.17.0.2:9200}, bound_addresses {[::]:9200}
[2017-07-01T04:55:05,318][INFO ][o.e.n.Node               ] [lmIsnX7] started
[2017-07-01T04:55:06,492][INFO ][o.e.l.LicenseService     ] [lmIsnX7] license [0a8ce788-74ad-49d9-aa3c-3c46ab9100d8] mode [trial] - valid
[2017-07-01T04:55:06,513][INFO ][o.e.g.GatewayService     ] [lmIsnX7] recovered [4] indices into cluster_state
[2017-07-01T04:55:08,078][INFO ][o.e.c.r.a.AllocationService] [lmIsnX7] Cluster health status changed from [RED] to [YELLOW] (reason: [shards started [[.monitoring-es-2-2017.07.01][0], [biocaddie][0]] ...]).
[2017-07-01T04:55:13,088][INFO ][o.n.e.r.RocchioExpandRestAction] [lmIsnX7] Starting Rocchio (biocaddie,multiple sclerosis,dataset,_all,10,10,0.50,0.50,1.20,0.75)
...
```

## Helper Scripts
A few other helper scripts are included to ease testing:
```bash
./scripts/start.sh          # Runs or starts your elasticsearch container
./scripts/stop.sh           # Stops your elasticsearch container
./scripts/restart.sh
./scripts/create-index.sh   # Creates a test index with the proper settings to enable storing term vectors
./scripts/add-docs.sh [-v]  # Adds documents from the biocaddie benchmark set to your index (assumes correct paths)
./scripts/delete-index.sh   # Deletes your container's test index and the records within
./scripts/build.sh          # Builds up elasticsearch plugin artifacts
./scripts/install.sh        # Installs the elasticsearch plugin into your running container
./scripts/remove.sh         # Removes your container's installed queryexpanion plugin
./rebuild.sh                # Removes the current plugin, builds the artifacts, installs the new plugin, and restarts elasticsearch to facilitate rapid development and testing
./logs.sh                   # View your elasticsearch container logs (tail=100)
./test.sh [search]          # Performs a test query against our REST API endpoint (only expands by default, but searches if first parameter is "search")
```

# Deploying artifacts
New artifacts can be deployed to OSSRH using the following command:
```bash
GPG_TTY=$(tty) mvn clean deploy
```
