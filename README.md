# elasticsearch-queryexpansion-plugin
A simple ElasticSearch plugin wrapping around the search endpoint to provide Rocchio query expansion

# Prerequisites
* Docker
   
**OR**

* Git + Maven

# Usage
For now, cloning the source is required to run the plugin (see TODOs):
```bash
git clone bodom0015/elasticsearch-queryexpansion-plugin queryexpansion && cd queryexpansion 
```
To use:

0. [Setup](README.md#setup)
1. [Build](README.md#build)
2. [Load](README.md#load)
3. [Test](README.md#test)

## Setup
Make sure that the biocaddie benchmark test dataset exists somewhere on disk:
```bash
cd $HOME
wget https://biocaddie.org/sites/default/files/update_json_folder.zip && unzip update_json_folder.zip
```

Run an ElasticSearch 5.3.2 container using the helper script:
```bash
./scripts/start.sh
```

Then, set up an index with the required parameters (store==true):
```bash
./scripts/create-index.sh
```

NOTE: You may need to modify *dataset_path* in `./scripts/add-docs.sh` if your benchmark data is not located within `$HOME`.

Finally, use the helper script to add the documents to the index:
```bash
./scripts/add-docs.sh
```

NOTE: Indexing the full benchmark set can take a long time. If you only need a small subset of the documents, you can always `Ctrl+C` once you get the desired number of records indexed.

## Build
A helper script has been included to ease building:
```bash
./scripts/build.sh
```

This will attempt to build the source using Maven (or Docker, if Maven is not available).

Either way, the build should produce a `target/releases/` directory with the necessary `.zip` file.

The `.zip` that ElasticSearch needs should be found at `./target/releases/queryexpansion-5.3.2-SNAPSHOT.zip`.

## Load
Once the artifacts are built, we just need to install them and restart ElasticSearch:
```bash
./scripts/install.sh
./scripts/restart.sh
```

## Test
You should now be able to test the new endpoint using the helper script or via raw `curl`:
```bash
$ ./test.sh
{"query":"sclerosis^2.798773920190095 study^0.4716440174771813 disease^0.584064093901503 or^0.3394485958568884 patients^0.79730633189081 multiple^1.941784058395449 was^0.4222225922753828 is^0.38702376034952857 to^0.4432445617796595 on^0.3817563584164061"}

$ ./test.sh search
<placeholder - this output format is currently incorrect> 
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

# TODO
* Write some unit / integration tests
* Publish release artifacts so users don't need to build the source by hand
