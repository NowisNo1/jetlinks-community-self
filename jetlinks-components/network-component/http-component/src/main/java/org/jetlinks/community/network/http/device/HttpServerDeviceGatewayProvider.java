package org.jetlinks.community.network.http.device;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.http.server.HttpServer;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * HTTP服务设备网关提供商
 *
 * @author LuoXingyue
 * @since 1.0
 */
@Component
public class HttpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;


    public HttpServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler messageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "http-server-gateway";
    }

    @Override
    public String getName() {
        return "HTTP 透传接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<HttpServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(server -> {
                String protocol = (String) properties.getConfiguration().get("protocol");

                Assert.hasText(protocol, "protocol can not be empty");

                return new HttpServerDeviceGateway(properties.getId(),
                    protocol,
                    protocolSupports,
                    registry,
                    messageHandler,
                    sessionManager,
                    server
                );
            });
    }
}
