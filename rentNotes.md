## Mods for running Pablo's stress tests
Simple mods to get the experiments running

Started mods from branch *unitrie-master-merge* as that's probably where Pablo touched it last

Changes to `build.gradle`
- showstandardstreams for logging
- get rid of error prone,  previously turned off only for Java 13+
- add the params `stress.accounts` and `stress.alpha`  so systemproperties passed via `-D` to gradle are passed along to the respective test JVMs

Add the above to commit message for easier refernce

To replicate Pablo's trie experiments [blog post](https://blog.rsk.co/noticia/stress-testing-ethereums-world-state/)


``` 
./gradlew ethereum:core:test \
 -Dstress.accounts="50000" -Dstress.alpha="0.25" \ //no. accounts and fraction contracts
 --tests org.hyperledger.besu.ethereum.worldstate.AccountCreationTest
```

