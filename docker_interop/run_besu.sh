#! /bin/bash

NODE_KEY=${1:?Must specify node key}
RPC_PORT=${2:?Must specify RPC port}
P2P_PORT=${3:?Must specify P2P port}
P2P_IP=`awk 'END{print $1}' /etc/hosts`

echo ${NODE_KEY} > /tmp/prv.key
cp /eth/static-nodes.json /opt/besu/

besu --genesis-file=/eth/besu_genesis.json \
     --logging=debug \
     --discovery-enabled=true \
     --host-allowlist=all \
     --rpc-http-cors-origins=all \
     --rpc-http-enabled=true \
     --rpc-http-port=${RPC_PORT} \
     --p2p-host=${P2P_IP} \
     --p2p-port=${P2P_PORT} \
     --node-private-key-file=/tmp/prv.key \
     --rpc-http-apis=ETH,NET,DEBUG,ADMIN,WEB3,EEA,PRIV