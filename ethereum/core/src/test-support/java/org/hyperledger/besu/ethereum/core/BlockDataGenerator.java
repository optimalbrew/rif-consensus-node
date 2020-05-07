/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.core;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toSet;

import com.google.common.base.Supplier;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.besu.crypto.SECP256K1;
import org.hyperledger.besu.crypto.SecureRandomProvider;
import org.hyperledger.besu.ethereum.mainnet.BodyValidation;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.merkleutils.ClassicMerkleAwareProvider;
import org.hyperledger.besu.ethereum.merkleutils.MerkleAwareProvider;
import org.hyperledger.besu.ethereum.worldstate.WorldStateArchive;

public class BlockDataGenerator {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final Random random;
  private final KeyPairGenerator keyPairGenerator;
  private Supplier<BlockOptions> blockOptionsSupplier = BlockOptions::create;

  public BlockDataGenerator(final int seed) {
    this.random = new Random(seed);
    keyPairGenerator = createKeyPairGenerator(seed);
  }

  public BlockDataGenerator() {
    this(1);
  }

  public void setBlockOptionsSupplier(final Supplier<BlockOptions> blockOptionsSupplier) {
    this.blockOptionsSupplier = blockOptionsSupplier;
  }

  private KeyPairGenerator createKeyPairGenerator(final long seed) {
    final KeyPairGenerator keyPairGenerator;
    try {
      keyPairGenerator = KeyPairGenerator.getInstance(SECP256K1.ALGORITHM, SECP256K1.PROVIDER);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(SECP256K1.CURVE_NAME);
    try {
      final SecureRandom secureRandom = SecureRandomProvider.createSecureRandom();
      secureRandom.setSeed(seed);
      keyPairGenerator.initialize(ecGenParameterSpec, secureRandom);
    } catch (final InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
    return keyPairGenerator;
  }

  /**
   * Generates a sequence of blocks with some accounts and account storage pre-populated with random
   * data.
   */
  private List<Block> blockSequence(
      final int count,
      final long nextBlock,
      final Hash parent,
      final WorldStateArchive worldStateArchive,
      final List<Address> accountsToSetup,
      final List<UInt256> storageKeys) {
    final List<Block> seq = new ArrayList<>(count);

    final MutableWorldState worldState = worldStateArchive.getMutable();

    long nextBlockNumber = nextBlock;
    Hash parentHash = parent;

    for (int i = 0; i < count; i++) {
      final WorldUpdater stateUpdater = worldState.updater();
      if (i == 0) {
        // Set up some accounts
        accountsToSetup.forEach(stateUpdater::createAccount);
        stateUpdater.commit();
      } else {
        // Mutate accounts
        accountsToSetup.forEach(
            hash -> {
              final MutableAccount a = stateUpdater.getAccount(hash).getMutable();
              a.incrementNonce();
              a.setBalance(Wei.of(positiveLong()));
              storageKeys.forEach(key -> a.setStorageValue(key, UInt256.ONE));
            });
        stateUpdater.commit();
      }
      final BlockOptions options =
          blockOptionsSupplier
              .get()
              .setBlockNumber(nextBlockNumber)
              .setParentHash(parentHash)
              .setStateRoot(worldState.rootHash());
      final Block next = block(options);
      seq.add(next);
      parentHash = next.getHash();
      nextBlockNumber = nextBlockNumber + 1L;
      worldState.persist();
    }

    return seq;
  }

  public List<Account> createRandomVanillaAccounts(
      final MutableWorldState worldState, final int count) {
    return createRandomAccounts(worldState, count, 0, 0);
  }

  public List<Account> createRandomAccounts(final MutableWorldState worldState, final int count) {
    return createRandomAccounts(worldState, count, .5f, .75f);
  }

  public List<Account> createRandomContractAccountsWithNonEmptyStorage(
      final MutableWorldState worldState, final int count) {
    return createRandomAccounts(worldState, count, 1f, 1f);
  }

  private List<Account> createRandomAccounts(
      final MutableWorldState worldState,
      final int count,
      final float percentContractAccounts,
      final float percentContractAccountsWithNonEmptyStorage) {
    WorldUpdater updater = worldState.updater();
    List<Account> accounts = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      MutableAccount account = updater.getOrCreate(address()).getMutable();
      if (random.nextFloat() < percentContractAccounts) {
        // Some percentage of accounts are contract accounts
        account.setCode(bytesValue(5, 50));
        account.setVersion(Account.DEFAULT_VERSION);
        if (random.nextFloat() < percentContractAccountsWithNonEmptyStorage) {
          // Add some storage for contract accounts
          int storageValues = random.nextInt(20) + 10;
          for (int j = 0; j < storageValues; j++) {
            account.setStorageValue(uint256(), uint256());
          }
        }
      }
      account.setNonce(random.nextInt(10));
      account.setBalance(Wei.of(positiveLong()));

      accounts.add(account);
    }
    updater.commit();
    worldState.persist();
    return accounts;
  }

  public List<Block> blockSequence(final int count) {
    return blockSequence(count, new ClassicMerkleAwareProvider());
  }

  public List<Block> blockSequence(final int count, final MerkleAwareProvider merkleAwareProvider) {
    final WorldStateArchive worldState =
        InMemoryStorageProvider.createInMemoryWorldStateArchive(merkleAwareProvider);
    return blockSequence(count, worldState, Collections.emptyList(), Collections.emptyList());
  }

  public List<Block> blockSequence(final Block previousBlock, final int count) {
    return blockSequence(previousBlock, count, new ClassicMerkleAwareProvider());
  }

  public List<Block> blockSequence(
      final Block previousBlock, final int count, final MerkleAwareProvider merkleAwareProvider) {
    final WorldStateArchive worldState =
        InMemoryStorageProvider.createInMemoryWorldStateArchive(merkleAwareProvider);
    Hash parentHash = previousBlock.getHeader().getHash();
    long blockNumber = previousBlock.getHeader().getNumber() + 1;
    return blockSequence(
        count,
        blockNumber,
        parentHash,
        worldState,
        Collections.emptyList(),
        Collections.emptyList());
  }

  public List<Block> blockSequence(
      final int count,
      final WorldStateArchive worldStateArchive,
      final List<Address> accountsToSetup,
      final List<UInt256> storageKeys) {
    final long blockNumber = BlockHeader.GENESIS_BLOCK_NUMBER;
    final Hash parentHash = Hash.ZERO;
    return blockSequence(
        count, blockNumber, parentHash, worldStateArchive, accountsToSetup, storageKeys);
  }

  public Block genesisBlock() {
    return genesisBlock(blockOptionsSupplier.get());
  }

  public Block genesisBlock(final BlockOptions options) {
    options
        .setBlockNumber(BlockHeader.GENESIS_BLOCK_NUMBER)
        .setStateRoot(Hash.EMPTY_TRIE_HASH)
        .setParentHash(Hash.ZERO);
    return block(options);
  }

  public Block block(final BlockOptions options) {
    final long blockNumber = options.getBlockNumber(positiveLong());
    final BlockBody body =
        blockNumber == BlockHeader.GENESIS_BLOCK_NUMBER ? BlockBody.empty() : body(options);
    final BlockHeader header = header(blockNumber, body, options);
    return new Block(header, body);
  }

  public Block block() {
    return block(new BlockOptions());
  }

  public BlockOptions nextBlockOptions(final Block afterBlock) {
    return blockOptionsSupplier
        .get()
        .setBlockNumber(afterBlock.getHeader().getNumber() + 1)
        .setParentHash(afterBlock.getHash());
  }

  public Block nextBlock(final Block afterBlock) {
    final BlockOptions options = nextBlockOptions(afterBlock);
    return block(options);
  }

  public BlockHeader header(final long blockNumber, final BlockBody blockBody) {
    return header(blockNumber, blockBody, new BlockOptions());
  }

  public BlockHeader header(final long blockNumber) {
    return header(blockNumber, body(), blockOptionsSupplier.get());
  }

  public BlockHeader header() {
    return header(positiveLong(), body(), blockOptionsSupplier.get());
  }

  public BlockHeader header(final long number, final BlockBody body, final BlockOptions options) {
    final int gasLimit = random.nextInt() & Integer.MAX_VALUE;
    final int gasUsed = Math.max(0, gasLimit - 1);
    final long blockNonce = random.nextLong();
    return BlockHeaderBuilder.create()
        .parentHash(options.getParentHash(hash()))
        .ommersHash(BodyValidation.ommersHash(body.getOmmers()))
        .coinbase(address())
        .stateRoot(options.getStateRoot(hash()))
        .transactionsRoot(BodyValidation.transactionsRoot(body.getTransactions()))
        .receiptsRoot(options.getReceiptsRoot(hash()))
        .logsBloom(options.getLogsBloom(logsBloom()))
        .difficulty(options.getDifficulty(Difficulty.of(uint256(4))))
        .number(number)
        .gasLimit(gasLimit)
        .gasUsed(options.getGasUsed(gasUsed))
        .timestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS).getEpochSecond())
        .extraData(options.getExtraData(bytes32()))
        .mixHash(hash())
        .nonce(blockNonce)
        .blockHeaderFunctions(options.getBlockHeaderFunctions(new MainnetBlockHeaderFunctions()))
        .buildBlockHeader();
  }

  public BlockBody body() {
    return body(blockOptionsSupplier.get());
  }

  public BlockBody body(final BlockOptions options) {
    final List<BlockHeader> ommers = new ArrayList<>();
    if (options.hasOmmers()) {
      final int ommerCount = random.nextInt(3);
      for (int i = 0; i < ommerCount; i++) {
        ommers.add(ommer());
      }
    }
    final List<Transaction> defaultTxs = new ArrayList<>();
    if (options.hasTransactions()) {
      defaultTxs.add(transaction());
      defaultTxs.add(transaction());
    }

    return new BlockBody(options.getTransactions(defaultTxs), ommers);
  }

  private BlockHeader ommer() {
    return header(positiveLong(), body(BlockOptions.create().hasOmmers(false)));
  }

  public Transaction transaction() {
    return transaction(bytes32());
  }

  public Transaction transaction(final Bytes payload) {
    return transaction(payload, address());
  }

  public Transaction transaction(final Bytes payload, final Address to) {
    return Transaction.builder()
        .nonce(positiveLong())
        .gasPrice(Wei.wrap(bytes32()))
        .gasLimit(positiveLong())
        .to(to)
        .value(Wei.wrap(bytes32()))
        .payload(payload)
        .chainId(BigInteger.ONE)
        .signAndBuild(generateKeyPair());
  }

  public Set<Transaction> transactions(final int n) {
    Wei gasPrice = Wei.wrap(bytes32());
    long gasLimit = positiveLong();
    Address to = address();
    Wei value = Wei.wrap(bytes32());
    int chainId = 1;
    Bytes32 payload = bytes32();
    final SECP256K1.Signature signature = SECP256K1.sign(payload, generateKeyPair());

    final Set<Transaction> txs =
        IntStream.range(0, n)
            .parallel()
            .mapToObj(
                v ->
                    new Transaction(
                        v,
                        gasPrice,
                        gasLimit,
                        Optional.of(to),
                        value,
                        signature,
                        payload,
                        to,
                        Optional.of(BigInteger.valueOf(chainId))))
            .collect(toSet());
    return txs;
  }

  public TransactionReceipt receipt(final long cumulativeGasUsed) {
    return new TransactionReceipt(
        hash(), cumulativeGasUsed, Arrays.asList(log(), log()), Optional.empty());
  }

  public TransactionReceipt receipt(final Bytes revertReason) {
    return new TransactionReceipt(
        hash(), positiveLong(), Arrays.asList(log(), log()), Optional.of(revertReason));
  }

  public TransactionReceipt receipt() {
    return receipt(positiveLong());
  }

  public TransactionReceipt receipt(final List<Log> logs) {
    return new TransactionReceipt(hash(), positiveLong(), logs, Optional.empty());
  }

  public UInt256 storageKey() {
    return uint256();
  }

  public List<TransactionReceipt> receipts(final Block block) {
    final long totalGas = block.getHeader().getGasUsed();
    final int receiptCount = block.getBody().getTransactions().size();

    final List<TransactionReceipt> receipts = new ArrayList<>(receiptCount);
    for (int i = 0; i < receiptCount; i++) {
      receipts.add(receipt((totalGas * (i + 1)) / (receiptCount)));
    }

    return receipts;
  }

  public List<Log> logs(final int logsCount, final int topicsPerLog) {
    return Stream.generate(() -> log(topicsPerLog)).limit(logsCount).collect(Collectors.toList());
  }

  public Log log() {
    return log(0);
  }

  public Log log(final int topicCount) {
    final List<LogTopic> topics =
        Stream.generate(this::logTopic).limit(topicCount).collect(Collectors.toList());
    return new Log(address(), bytesValue(5, 15), topics);
  }

  private LogTopic logTopic() {
    return LogTopic.wrap(bytesValue(Bytes32.SIZE));
  }

  private Bytes32 bytes32() {
    return Bytes32.wrap(bytes(Bytes32.SIZE));
  }

  public Bytes bytesValue(final int size) {
    return Bytes.wrap(bytes(size));
  }

  public Bytes bytesValue() {
    return bytesValue(1, 20);
  }

  public Bytes bytesValue(final int minSize, final int maxSize) {
    checkArgument(minSize >= 0);
    checkArgument(maxSize >= 0);
    checkArgument(maxSize > minSize);
    final int size = random.nextInt(maxSize - minSize) + minSize;
    return Bytes.wrap(bytes(size));
  }

  /**
   * Creates a UInt256 with a value that fits within maxByteSize
   *
   * @param maxByteSize The byte size to cap this value to
   * @return
   */
  private UInt256 uint256(final int maxByteSize) {
    assert maxByteSize <= 32;
    return UInt256.fromBytes(Bytes32.wrap(bytes(32, 32 - maxByteSize)));
  }

  private UInt256 uint256() {
    return UInt256.fromBytes(bytes32());
  }

  private long positiveLong() {
    final long l = random.nextLong();
    return l < 0 ? Math.abs(l + 1) : l;
  }

  public Hash hash() {
    return Hash.wrap(bytes32());
  }

  public Address address() {
    return Address.wrap(bytesValue(Address.SIZE));
  }

  public LogsBloomFilter logsBloom() {
    return new LogsBloomFilter(Bytes.of(bytes(LogsBloomFilter.BYTE_SIZE)));
  }

  private byte[] bytes(final int size) {
    return bytes(size, 0);
  }

  /**
   * Creates a byte sequence with leading zeros.
   *
   * @param size The size of the byte array to return
   * @param zerofill The number of lower-order bytes to fill with zero (creating a smaller big
   *     endian integer value)
   * @return
   */
  private byte[] bytes(final int size, final int zerofill) {
    final byte[] bytes = new byte[size];
    random.nextBytes(bytes);
    Arrays.fill(bytes, 0, zerofill, (byte) 0x0);
    return bytes;
  }

  private SECP256K1.KeyPair generateKeyPair() {
    final java.security.KeyPair rawKeyPair = keyPairGenerator.generateKeyPair();
    final BCECPrivateKey privateKey = (BCECPrivateKey) rawKeyPair.getPrivate();
    final BCECPublicKey publicKey = (BCECPublicKey) rawKeyPair.getPublic();

    final BigInteger privateKeyValue = privateKey.getD();

    // Ethereum does not use encoded public keys like bitcoin - see
    // https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm for details
    // Additionally, as the first bit is a constant prefix (0x04) we ignore this value
    final byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
    final BigInteger publicKeyValue =
        new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));

    return new SECP256K1.KeyPair(
        SECP256K1.PrivateKey.create(privateKeyValue), SECP256K1.PublicKey.create(publicKeyValue));
  }

  public static class BlockOptions {
    private OptionalLong blockNumber = OptionalLong.empty();
    private Optional<Hash> parentHash = Optional.empty();
    private Optional<Hash> stateRoot = Optional.empty();
    private Optional<Difficulty> difficulty = Optional.empty();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<BlockHeader> ommers = new ArrayList<>();
    private Optional<Bytes> extraData = Optional.empty();
    private Optional<BlockHeaderFunctions> blockHeaderFunctions = Optional.empty();
    private Optional<Hash> receiptsRoot = Optional.empty();
    private Optional<Long> gasUsed = Optional.empty();
    private Optional<LogsBloomFilter> logsBloom = Optional.empty();
    private boolean hasOmmers = true;
    private boolean hasTransactions = true;

    public static BlockOptions create() {
      return new BlockOptions();
    }

    public List<Transaction> getTransactions(final List<Transaction> defaultValue) {
      return transactions.isEmpty() ? defaultValue : transactions;
    }

    public List<BlockHeader> getOmmers(final List<BlockHeader> defaultValue) {
      return ommers.isEmpty() ? defaultValue : ommers;
    }

    public long getBlockNumber(final long defaultValue) {
      return blockNumber.orElse(defaultValue);
    }

    public Hash getParentHash(final Hash defaultValue) {
      return parentHash.orElse(defaultValue);
    }

    public Hash getStateRoot(final Hash defaultValue) {
      return stateRoot.orElse(defaultValue);
    }

    public Difficulty getDifficulty(final Difficulty defaultValue) {
      return difficulty.orElse(defaultValue);
    }

    public Bytes getExtraData(final Bytes32 defaultValue) {
      return extraData.orElse(defaultValue);
    }

    public BlockHeaderFunctions getBlockHeaderFunctions(final BlockHeaderFunctions defaultValue) {
      return blockHeaderFunctions.orElse(defaultValue);
    }

    public Hash getReceiptsRoot(final Hash defaultValue) {
      return receiptsRoot.orElse(defaultValue);
    }

    public long getGasUsed(final long defaultValue) {
      return gasUsed.orElse(defaultValue);
    }

    public LogsBloomFilter getLogsBloom(final LogsBloomFilter defaultValue) {
      return logsBloom.orElse(defaultValue);
    }

    public boolean hasTransactions() {
      return hasTransactions;
    }

    public boolean hasOmmers() {
      return hasOmmers;
    }

    public BlockOptions addTransaction(final Transaction... tx) {
      transactions.addAll(Arrays.asList(tx));
      return this;
    }

    public BlockOptions addOmmers(final BlockHeader... headers) {
      ommers.addAll(Arrays.asList(headers));
      return this;
    }

    public BlockOptions addTransaction(final Collection<Transaction> txs) {
      return addTransaction(txs.toArray(new Transaction[] {}));
    }

    public BlockOptions setBlockNumber(final long blockNumber) {
      this.blockNumber = OptionalLong.of(blockNumber);
      return this;
    }

    public BlockOptions setParentHash(final Hash parentHash) {
      this.parentHash = Optional.of(parentHash);
      return this;
    }

    public BlockOptions setStateRoot(final Hash stateRoot) {
      this.stateRoot = Optional.of(stateRoot);
      return this;
    }

    public BlockOptions setDifficulty(final Difficulty difficulty) {
      this.difficulty = Optional.of(difficulty);
      return this;
    }

    public BlockOptions setExtraData(final Bytes extraData) {
      this.extraData = Optional.of(extraData);
      return this;
    }

    public BlockOptions setBlockHeaderFunctions(final BlockHeaderFunctions blockHeaderFunctions) {
      this.blockHeaderFunctions = Optional.of(blockHeaderFunctions);
      return this;
    }

    public BlockOptions setReceiptsRoot(final Hash receiptsRoot) {
      this.receiptsRoot = Optional.of(receiptsRoot);
      return this;
    }

    public BlockOptions setGasUsed(final long gasUsed) {
      this.gasUsed = Optional.of(gasUsed);
      return this;
    }

    public BlockOptions setLogsBloom(final LogsBloomFilter logsBloom) {
      this.logsBloom = Optional.of(logsBloom);
      return this;
    }

    public BlockOptions hasTransactions(final boolean hasTransactions) {
      this.hasTransactions = hasTransactions;
      return this;
    }

    public BlockOptions hasOmmers(final boolean hasOmmers) {
      this.hasOmmers = hasOmmers;
      return this;
    }
  }
}
