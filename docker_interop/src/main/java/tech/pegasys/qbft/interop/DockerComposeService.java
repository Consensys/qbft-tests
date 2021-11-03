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

import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DockerComposeService {
  private static final String BESU_COMMAND =
      "chmod 755 /scripts/run_besu.sh && /scripts/run_besu.sh";
  private static final String GO_QUORUM_COMMAND =
      "chmod 755 /scripts/run_geth.sh && /scripts/run_geth.sh";
  private String serviceName;
  private boolean isBesuMode;
  private String hostname;
  private int rpcPort;
  private int p2pPort;
  private String ipAddress;
  private String privateKey;
  private String staticNodeFileName;
  private Level logLevel;

  public Map<String, Object> getMap() {
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("hostname", hostname);
    map.put("image", isBesuMode ? "${BESU_IMAGE}" : "${QUORUM_IMAGE}");
    map.put(
        "ports",
        List.of(
            p2pPort + ":" + p2pPort + "/tcp",
            p2pPort + ":" + p2pPort + "/udp",
            rpcPort + ":" + rpcPort));
    map.put("entrypoint", List.of("/bin/sh"));
    map.put("command", List.of("-c", getCommand()));
    map.put("networks", Map.of("app_net", Map.of("ipv4_address", ipAddress)));
    map.put("restart", "no");
    map.put("volumes", List.of("./:/scripts"));

    return Map.of(serviceName, map);
  }

  private String getCommand() {
    final String commandFormat = "%s %s %d %d %s %s";
    return String.format(
        commandFormat,
        isBesuMode ? BESU_COMMAND : GO_QUORUM_COMMAND,
        privateKey,
        p2pPort,
        rpcPort,
        staticNodeFileName,
        isBesuMode ? logLevel.name() : parseLogLevelToGoQuorumValue(logLevel));
  }

  private int parseLogLevelToGoQuorumValue(Level logLevel) {
    // 0=silent, 1=error, 2=warn, 3=info, 4=debug, 5=detail
    switch (logLevel) {
      case OFF:
        return 0;
      case ERROR:
        return 1;
      case WARNING:
        return 2;
      case INFO:
        return 3;
      case DEBUG:
        return 4;
      case TRACE:
        return 5;
      default:
        return 3;
    }
  }

  public static final class Builder {
    private String serviceName;
    private boolean isBesuMode;
    private String hostname;
    private int rpcPort;
    private int p2pPort;
    private String ipAddress;
    private String privateKey;
    private String staticNodeFileName;
    private Level logLevel;

    private Builder() {}

    public static Builder aDockerComposeService() {
      return new Builder();
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withIsBesuMode(boolean isBesuMode) {
      this.isBesuMode = isBesuMode;
      return this;
    }

    public Builder withHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder withRpcPort(int rpcPort) {
      this.rpcPort = rpcPort;
      return this;
    }

    public Builder withP2pPort(int p2pPort) {
      this.p2pPort = p2pPort;
      return this;
    }

    public Builder withIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public Builder withPrivateKey(String privateKey) {
      this.privateKey = privateKey;
      return this;
    }

    public Builder withStaticNodeFileName(String staticNodeFileName) {
      this.staticNodeFileName = staticNodeFileName;
      return this;
    }

    public Builder withLogLevel(Level logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public DockerComposeService build() {
      DockerComposeService dockerComposeService = new DockerComposeService();
      dockerComposeService.serviceName = this.serviceName;
      dockerComposeService.rpcPort = this.rpcPort;
      dockerComposeService.hostname = this.hostname;
      dockerComposeService.isBesuMode = this.isBesuMode;
      dockerComposeService.p2pPort = this.p2pPort;
      dockerComposeService.ipAddress = this.ipAddress;
      dockerComposeService.privateKey = this.privateKey;
      dockerComposeService.staticNodeFileName = this.staticNodeFileName;
      dockerComposeService.logLevel = this.logLevel;
      return dockerComposeService;
    }
  }
}
