## Mods for running Pablo's stress tests
Simple mods to get the experiments running

Started mods from branch *unitrie-master-merge* as that's probably where Pablo touched it last

Changes to `build.gradle`
- showstandardstreams for logging
- get rid of error prone,  previously turned off only for Java 13+
- add the params `stress.accounts` and `stress.alpha`  so systemproperties passed via `-D` to gradle are passed along to the respective test JVMs

Add the above to commit message for easier refernce

To replicate Pablo's trie experiments [blog post](https://blog.rsk.co/noticia/stress-testing-ethereums-world-state/)

Use `stress.accounts` to set the number of accounts and `stress.alpha` to set the fraction of contracts (some code is inserted into righ child).


``` 
./gradlew ethereum:core:test \
 -Dstress.accounts="50000" -Dstress.alpha="0.25" \ //no. accounts and fraction contracts
 --tests org.hyperledger.besu.ethereum.worldstate.AccountCreationTest
```

Optionally to run the test again with new params (this is frowned upon) use `cleanTest`. See SO [discussion](https://stackoverflow.com/questions/29427020/how-to-run-gradle-test-when-all-tests-are-up-to-date)

```
./gradlew cleanTest  ethereum:core:test \
 -Dstress.accounts="50000" -Dstress.alpha="0.25" \ //no. accounts and fraction contracts
 --tests org.hyperledger.besu.ethereum.worldstate.AccountCreationTest
```

this will redo 2 tasks, leaving 82 unaffected.

The result will look like

```
Total elapsed: 4.835
    Stats: Total nodes = 10608, Branches = 5608, Min len = 0, Max len = 230, Path Avg = 24.963
    Looked up for 5000 accounts, found 5000, elapsed = 0.243
```

This can be modified to gather data. Better to extend the code for random data generation in a single shot.
