#! /bin/bash

PRIV_KEY=${1:?Must specify node key}
P2P_PORT=${2:?Must specify P2P port}
RPC_PORT=${3:?Must specify rpc port}
STATIC_NODE_FILE=${4:?Must specify static nodes filename}
LOG_LEVEL=${5:-tracing}
P2P_IP=`awk 'END{print $1}' /etc/hosts`

echo ${PRIV_KEY} > /tmp/prv.key
cp /scripts/static-nodes/${STATIC_NODE_FILE} /opt/besu/static-nodes.json

besu --genesis-file=/scripts/besu_genesis.json \
     --logging=${LOG_LEVEL} \
     --discovery-enabled=true \
     --host-allowlist=all \
     --rpc-http-cors-origins=all \
     --rpc-http-enabled=true \
     --rpc-http-port=${RPC_PORT} \
     --p2p-host=${P2P_IP} \
     --p2p-port=${P2P_PORT} \
     --node-private-key-file=/tmp/prv.key \
     --rpc-http-apis=ETH,NET,DEBUG,ADMIN,WEB3,EEA,PRIV,QBFT