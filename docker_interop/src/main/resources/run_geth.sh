#! /bin/sh
set -euo pipefail

PRIV_KEY=${1:?Must specify node key}
P2P_PORT=${2:?Must specify P2P port}
RPC_PORT=${3:?Must specify RPC port}
STATIC_NODE_FILE=${4:?Must specify static nodes filename}
LOG_LEVEL=${5:-3}
P2P_IP=`awk 'END{print $1}' /etc/hosts`

mkdir -p /eth/geth
cp /scripts/static-nodes/${STATIC_NODE_FILE} /eth/geth/static-nodes.json
geth --datadir "/eth" init "/scripts/quorum_genesis.json"
geth \
        --verbosity ${LOG_LEVEL} \
        --mine \
        --nousb \
        --identity "${HOSTNAME}" \
        --http \
        --http.addr "0.0.0.0" \
        --http.port "${RPC_PORT}" \
        --http.corsdomain "*" \
        --http.api "admin,db,eth,net,web3,istanbul,personal" \
        --datadir "/eth" \
        --port ${P2P_PORT} \
        --bootnodes "" \
        --networkid "2017" \
        --nat "extip:${P2P_IP}" \
        --nodekeyhex "${PRIV_KEY}" \
        --debug \
        --metrics \
        --syncmode "full" \
        --miner.gasprice 0 \
        --istanbul.blockperiod 5 \
        --istanbul.requesttimeout 10000