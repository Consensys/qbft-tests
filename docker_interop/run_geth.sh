#! /bin/sh
set -euo pipefail

NODE_KEY=${1:?Must specify node key}

mkdir -p /eth/geth
cp /importData/static-nodes.json /eth/geth/
geth --datadir "/eth" init "/importData/quorum_genesis.json"
geth \
        --mine \
        --nousb \
        --identity "${HOSTNAME}" \
        --rpc \
        --bootnodes "" \
        --rpcaddr "0.0.0.0" \
        --rpcport "8545" \
        --rpccorsdomain "*" \
        --datadir "/eth" \
        --port "30303" \
        --rpcapi "admin,db,eth,net,web3,istanbul,personal" \
        --networkid "2017" \
        --nat "any" \
        --nodekeyhex "${NODE_KEY}" \
        --debug \
        --metrics \
        --syncmode "full" \
        --gasprice 0