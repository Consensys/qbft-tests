/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.qbft.interop;

import static tech.pegasys.qbft.interop.DockerComposeYaml.IP_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SECP256K1;
import org.hyperledger.besu.ethereum.core.Address;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "qbft-docker-compose",
    mixinStandardHelpOptions = true,
    version = "qbft-docker-compose 1.0",
    description =
        "Generate docker-compose.yml and required files to run Besu and GoQuorum in QBFT interop mode.")
public class QbftDockerComposeApp implements Callable<Integer> {
  private static final SECP256K1 secp256K1 = new SECP256K1();

  @Option(
      names = {"-b", "--besuNodes"},
      description = "Number of Besu Nodes to include in docker-compose. Default: ${DEFAULT-VALUE}")
  private int besuNodes = 2;

  @Option(
      names = {"-q", "--quorumNodes"},
      description =
          "Number of Quorum Nodes to include in docker-compose. Default: ${DEFAULT-VALUE}")
  private int quorumNodes = 2;

  @Option(
      names = {"-d", "--destination"},
      description =
          "destination directory where docker-compose will be generated. Default: ${DEFAULT-VALUE}")
  private File destination = Path.of("out").toFile().getAbsoluteFile();

  @Option(
      names = {"-p", "--block-period"},
      description = "Block period (in seconds) to use. Default: ${DEFAULT-VALUE}")
  private Integer blockPeriodSeconds = 5;

  @Option(
      names = {"-r", "--request-timeout"},
      description = "Block request timeout (in seconds) to use. Default: ${DEFAULT-VALUE}")
  private Integer requestTimeoutSeconds = 10;

  public static void main(String[] args) {
    final int exitCode = new CommandLine(new QbftDockerComposeApp()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    System.out.printf(
        "Generating docker-compose for %d Besu and %d Quorum nodes in %s%n",
        besuNodes, quorumNodes, destination);

    // generate output directories
    final Path directory = Files.createDirectories(destination.toPath());

    final List<KeyPair> besuKeyPairs = generateNodeKeys(besuNodes);
    final List<KeyPair> quorumKeyPairs = generateNodeKeys(quorumNodes);

    // generate docker-compose file
    final DockerComposeYaml dockerComposeYaml = new DockerComposeYaml();
    final Path generated = dockerComposeYaml.generate(directory, besuKeyPairs, quorumKeyPairs);
    System.out.println("Generated: " + generated);

    // write dev.env, run scripts
    copyResource(".env", directory.resolve(".env"));
    System.out.println("Generated: " + directory.resolve(".env"));

    copyResource("run_besu.sh", directory.resolve("run_besu.sh"));
    System.out.println("Generated: " + directory.resolve("run_besu.sh"));

    modifyAndCopyResource(
        "run_geth.sh",
        directory.resolve("run_geth.sh"),
        List.of("--istanbul.blockperiod 5", "--istanbul.requesttimeout 10000"),
        List.of(
            "--istanbul.blockperiod " + blockPeriodSeconds,
            "--istanbul.requesttimeout " + requestTimeoutSeconds * 1_000L));
    System.out.println("Generated: " + directory.resolve("run_geth.sh"));

    // generate ExtraData
    final String genesisExtraDataString =
        QbftExtraDataCodec.createGenesisExtraDataString(getAddresses(besuKeyPairs, quorumKeyPairs));
    System.out.println("Extra data: " + genesisExtraDataString);

    // write besu_genesis
    modifyAndCopyResource(
        "besu_genesis_template.json",
        directory.resolve("besu_genesis.json"),
        List.of("%EXTRA_DATA%", "%BLOCK_PERIOD%", "%REQUEST_TIMEOUT%"),
        List.of(
            genesisExtraDataString,
            String.valueOf(blockPeriodSeconds),
            String.valueOf(requestTimeoutSeconds)));
    System.out.println("Generated: " + directory.resolve("besu_genesis.json"));

    // write quorum_genesis
    modifyAndCopyResource(
        "quorum_genesis_template.json",
        directory.resolve("quorum_genesis.json"),
        List.of("%EXTRA_DATA%"),
        List.of(genesisExtraDataString));
    System.out.println("Generated: " + directory.resolve("quorum_genesis_template.json"));

    // write static-nodes
    final Path staticNodesDir = Files.createDirectories(directory.resolve("static-nodes"));
    final List<String> enodesForStaticNodes = getEnodesForStaticNodes(besuKeyPairs, quorumKeyPairs);
    generateStaticNodes(staticNodesDir, enodesForStaticNodes);

    return 0;
  }

  private void generateStaticNodes(
      final Path staticNodesDir, final List<String> enodesForStaticNodes) throws IOException {
    // write static-nodes for each node by not including enode for itself
    for (int i = 0; i < enodesForStaticNodes.size(); i++) {
      List<String> enodes = new ArrayList<>(enodesForStaticNodes);
      enodes.remove(i);
      // write as JSON array
      final String quoted =
          enodes.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
      Files.writeString(staticNodesDir.resolve("static-nodes-" + i + ".json"), "[" + quoted + "]");
    }
  }

  private static void copyResource(final String resource, final Path destination)
      throws IOException {
    try (final InputStream resourceAsStream =
        QbftDockerComposeApp.class.getClassLoader().getResourceAsStream(resource)) {
      if (resourceAsStream != null) {
        Files.copy(resourceAsStream, destination, StandardCopyOption.REPLACE_EXISTING);
      } else {
        throw new IOException("Resource not found, null returned");
      }
    }
  }

  private static void modifyAndCopyResource(
      final String resource,
      final Path destination,
      final List<String> tokens,
      final List<String> replacements)
      throws IOException {
    try (final InputStream resourceAsStream =
        QbftDockerComposeApp.class.getClassLoader().getResourceAsStream(resource)) {
      if (resourceAsStream == null) {
        throw new IllegalStateException("Unable to load contents from resource: " + resource);
      }
      final String contents = new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
      final String modified =
          StringUtils.replaceEach(
              contents, tokens.toArray(String[]::new), replacements.toArray(String[]::new));
      Files.writeString(destination, modified, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
  }

  private static List<KeyPair> generateNodeKeys(final int numberOfNodes) {
    return IntStream.range(0, numberOfNodes)
        .mapToObj(i -> secp256K1.generateKeyPair())
        .collect(Collectors.toList());
  }

  private static List<Address> getAddresses(
      final List<KeyPair> besuKeys, final List<KeyPair> quorumKeys) {
    final List<Address> addresses = new ArrayList<>();
    besuKeys.forEach(keyPair -> addresses.add(Address.extract(keyPair.getPublicKey())));
    quorumKeys.forEach(keyPair -> addresses.add(Address.extract(keyPair.getPublicKey())));
    return addresses;
  }

  private static List<String> getEnodesForStaticNodes(
      final List<KeyPair> besuKeys, final List<KeyPair> quorumKeys) {
    int startIp = DockerComposeYaml.START_IP;
    int startP2PPort = DockerComposeYaml.START_P2P_PORT;
    final List<String> enodeList = new ArrayList<>();
    final String template = "enode://%s@%s:%d";
    for (KeyPair besuKey : besuKeys) {
      final String pubKey = besuKey.getPublicKey().getEncodedBytes().toUnprefixedHexString();
      enodeList.add(String.format(template, pubKey, IP_PREFIX + startIp++, startP2PPort++));
    }

    for (KeyPair quorumKey : quorumKeys) {
      final String pubKey = quorumKey.getPublicKey().getEncodedBytes().toUnprefixedHexString();
      enodeList.add(String.format(template, pubKey, IP_PREFIX + startIp++, startP2PPort++));
    }
    return enodeList;
  }
}
