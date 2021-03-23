# QBFT INTEROP TESTING

## Overview
This docker-composer allows a network of GoQuorum and Besu Ethereum nodes to be run up
in a network.

This network can then be queried via Json RPC (or by log parsing) to determine if the nodes
have peered and are producing blocks.

## Operation
To run this network:
0. Ensure a Docker server is running on the local PC
1. GoQuorum docker image can be built locally, e.g. git clone https://github.com/ConsenSysQuorum/quorum and checkout qibft branch. `docker build -t localquorum:1.0 .`
2. Modify `.env` file to update docker image variables (if required) which are used by docker-compose.yml file
3. In a terminal, change to this directory
4. Execute `docker-compose up --force-recreate` (this prevent old databases being reused)
5. Execute (in a separate shell window) `docker-compose down` to shutdown.

## Network Topology
* The node-address and IP address of each node is fixed in the docker-compose, thus the enode-address of each node is known, and stored in the static-nodes.yaml file. (Also see .env file)
* Quorum nodes connect to each other via static-nodes
* Besu connects to all Quorum nodes via discovery (bootnodes specified on commandline)

## Potential Issues
The node-keys used by the nodes _must_ be hardcoded (rather than randomly assigned) to ensure the nodes
which are QBFT validators align with the content of the Genesis File's ExtraData field (which encodes the
addresses of the nodes which are validators).

Thus, there is some manual effort required if you wish to reconfigure the network (eg bring in an extra besu validator).

The steps to do this:
1. Create new SECP256k1 keypair for the node
2. Create a new 'ExtraData' string, and insert into the Genesis file
3. Create a new node in the docker-compose.yml, and assign the new key to the node

## Tests to be conducted
There are a variety of tests which are useful to conduct using this system:

1. Peering - do our nodes connect (agree on SubProtocols, and Genesis File Hash)
2. Passive Mining - do our nodes accept blocks mined by the network?
3. Active Mining - can Besu and GoQuorum interact using QBFT messaging to produce blocks (ideally will need 50/50 Besu/Quorum participants)
4. Validator Changes - can we vote in/out validators? Does our block hashing and process work as expected?
5. Round-change - can we terminate 1/3 of the network, and continue to produce blocks
6. Migration - can a network migrate from IBFT --> IBFT(2/3) --> QBFT? Do all participants transition)


