package tech.pegasys.qbft.interop;

import org.hyperledger.besu.crypto.KeyPair;
import org.hyperledger.besu.crypto.SECP256K1;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DockerComposeYaml {
    static final int START_IP = 10;
    static final int START_RPC_PORT = 8541;
    static final int START_P2P_PORT = 35300;
    static final String IP_PREFIX = "172.16.239.";

    private int ip = START_IP;
    private int rpcPort = START_RPC_PORT;
    private int p2pPort = START_P2P_PORT;

    public Path generate(final Path directory, final List<KeyPair> besuNodeKeyPairs, final List<KeyPair> quorumNodeKeyPairs) throws IOException {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        final Yaml yaml = new Yaml(dumperOptions);
        final Path file = directory.resolve("docker-compose.yml");
        try (final FileWriter fileWriter = new FileWriter(file.toFile(), StandardCharsets.UTF_8)) {
            yaml.dump(addServices(besuNodeKeyPairs, quorumNodeKeyPairs), fileWriter);
        }
        return file;
    }

    private Map<String, Object> addServices(final List<KeyPair> besuNodeKeyPairs, final List<KeyPair> quorumNodeKeyPairs) throws IOException {
        final Map<String, Object> services = new LinkedHashMap<>();
        int staticNodeFileIndex = 0;
        final String staticNodeFileFormat = "static-nodes-%d.json";
        for (int i = 0; i < besuNodeKeyPairs.size(); i++) {
            final DockerComposeService service = DockerComposeService.Builder.aDockerComposeService()
                    .withIsBesuMode(true)
                    .withServiceName("besu-node-" + i)
                    .withHostname("besu-node-" + i)
                    .withIpAddress(IP_PREFIX + ip++)
                    .withRpcPort(rpcPort++)
                    .withP2pPort(p2pPort++)
                    .withPrivateKey(besuNodeKeyPairs.get(i).getPrivateKey().getEncodedBytes().toUnprefixedHexString())
                    .withStaticNodeFileName(String.format(staticNodeFileFormat, staticNodeFileIndex++))
                    .build();

            services.putAll(service.getMap());
        }

        for (int i = 0; i < quorumNodeKeyPairs.size(); i++) {
            final DockerComposeService service = DockerComposeService.Builder.aDockerComposeService()
                    .withIsBesuMode(false)
                    .withServiceName("quorum-node-" + i)
                    .withHostname("quorum-node-" + i)
                    .withIpAddress(IP_PREFIX + ip++)
                    .withRpcPort(rpcPort++)
                    .withP2pPort(p2pPort++)
                    .withPrivateKey(quorumNodeKeyPairs.get(i).getPrivateKey().getEncodedBytes().toUnprefixedHexString())
                    .withStaticNodeFileName(String.format(staticNodeFileFormat, staticNodeFileIndex++))
                    .build();

            services.putAll(service.getMap());
        }

        final Map<String, Object> template = readTemplate();
        template.put("services", services);

        return template;
    }

    private Map<String, Object> readTemplate() throws IOException {
        final Yaml yaml = new Yaml();
        try (final InputStream yamlInputStream = getClass().getClassLoader().getResourceAsStream("docker-compose-template.yml")) {
            return yaml.load(yamlInputStream);
        }
    }
}
