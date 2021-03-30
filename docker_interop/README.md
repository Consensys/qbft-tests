# QBFT INTEROP TESTING

## Overview
This projects generate docker-compose files that allows a network of GoQuorum and Besu Ethereum nodes to be run up
in a network.

This network can then be queried via Json RPC (or by log parsing) to determine if the nodes have peered and are 
producing blocks.

## Built docker images locally (if required)
1. GoQuorum docker image can be built locally, e.g. git clone https://github.com/ConsenSysQuorum/quorum and checkout qibft branch. `docker build -t localquorum:1.0 .`
2. Besu docker image can be built locally. e.g. git clone https://github.com/hyperledger/besu.git. `./gradlew distDocker`.
   Use `docker images` to determine the image name and tag. It should be similar to: 
   `hyperledger/besu:21.1.3-SNAPSHOT-openjdk-11`. The latest image from dockerhub can also be fetched `hyperledger/besu:develop`
3. Update values of `QUORUM_IMAGE` and `BESU_IMAGE` in `.env` file if required 
   (either in resources which will be applicable on all future runs `./app/src/main/resources/.env` or in generated `./out/.env`)

## Generate docker-compose
1. Execute `./gradlew run`
2. The above command will (by default) generate a docker-compose with 2 Besu nodes and 2 Quorum nodes in `./out` folder.
3. To get command help, run `./gradlew run --args="-h"`.


## Operation
To run this network:
0. Ensure a Docker server is running on the local PC
1. `cd` to `out` directory (or directory where `docker-compose.yml` is generated).   
2. Execute `docker-compose up --force-recreate` (this prevent old databases being reused)
3. Execute (in a separate shell window) `docker-compose down` to bring the network down. `<CTRL+C>` also works.


## Network Topology
* Each time the project is executed, a new set of SECP256k1 keys are generated for each node.
  docker-compose and static-nodes files are updated accordingly with fixed ports and IP addresses.
* static-nodes file is also generated for each node (excluding self enode address). Discovery is also enabled.

## Sample commands from host:
* `curl -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"admin_peers","params":[],"id":1}' http://localhost:8542 | jq '.'`
* `docker-compose logs quorum-node-0`
* `docker-compose logs besu-nodes-0`

## Tests to be conducted
There are a variety of tests which are useful to conduct using this system:

1. Peering - do our nodes connect (agree on SubProtocols, and Genesis File Hash)
2. Passive Mining - do our nodes accept blocks mined by the network?
3. Active Mining - can Besu and GoQuorum interact using QBFT messaging to produce blocks (ideally will need 50/50 Besu/Quorum participants)
4. Validator Changes - can we vote in/out validators? Does our block hashing and process work as expected?
5. Round-change - can we terminate 1/3 of the network, and continue to produce blocks
6. Migration - can a network migrate from IBFT --> IBFT(2/3) --> QBFT? Do all participants transition)


