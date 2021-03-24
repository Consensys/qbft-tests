#! /bin/bash

NODE_KEY=${1:?Must specify node key}
P2P_IP=`awk 'END{print $1}' /etc/hosts`

echo ${NODE_KEY} > /tmp/prv.key
cp /eth/static-nodes.json /opt/besu/database/

besu --genesis-file=/eth/besu_genesis.json \
     --logging=debug \
     --discovery-enabled=false \
     --host-allowlist=all \
     --rpc-http-cors-origins=all \
     --rpc-http-enabled=true \
     --p2p-host=${P2P_IP} \
     --node-private-key-file=/tmp/prv.key \
     --rpc-http-apis=ETH,NET,DEBUG,ADMIN,WEB3,EEA,PRIV