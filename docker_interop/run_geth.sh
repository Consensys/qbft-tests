#! /bin/sh
set -euo pipefail

NODE_KEY=${1:?Must specify node key}
RPC_PORT=${2:?Must specify RPC port}
NETWORK_PORT=${3:?Must specify network port}
P2P_IP=`awk 'END{print $1}' /etc/hosts`

mkdir -p /eth/geth
cp /importData/static-nodes.json /eth/geth/
geth --datadir "/eth" init "/importData/quorum_genesis.json"
geth \
        --mine \
        --nousb \
        --identity "${HOSTNAME}" \
        --http \
        --http.addr "0.0.0.0" \
        --http.port "${RPC_PORT}" \
        --http.corsdomain "*" \
        --http.api "admin,db,eth,net,web3,istanbul,personal" \
        --datadir "/eth" \
        --port ${NETWORK_PORT} \
        --bootnodes "" \
        --networkid "2017" \
        --nat "extip:${P2P_IP}" \
        --nodekeyhex "${NODE_KEY}" \
        --debug \
        --metrics \
        --syncmode "full" \
        --miner.gasprice 0