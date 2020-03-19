# Development guide

## Prerequisites

- [SDKMan](https://sdkman.io/)


## Steps

1. Install [Java 11+](./building.md#prerequisites)

```ssh
sdk install java xx.y.yy-zzz
```

> z: mayor version, y: minor version, z: vendor


2. Install Gradle

```ssh
sdk install gradle
```

## Starting Besu

Configure your prefered IDE with this program arguments:

```bash
--network=dev --miner-enabled --miner-coinbase=0xfe3b557e8fb62b89f4916b721be55ceb828dbd73 --rpc-http-cors-origins="all" --host-whitelist="*" --rpc-ws-enabled --rpc-http-enabled --data-path=/tmp/besu
```

Run and test if it is running:

```bash
curl --location --request POST 'localhost:8545' \
--header 'Content-Type: application/json' \
--data-raw '{
    "jsonrpc": "2.0",
    "method": "eth_chainId",
    "params": [],
    "id": 1
}'
```

The response it would be this:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": "0x7e2"
}
```

Where `0x7e2` is 2018, the development chain id.