package moe.caa.multilogin.velocity.injector.handler;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.AuthSessionHandler;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;

@FunctionalInterface
interface AuthSessionFactory {
    AuthSessionHandler create(
            VelocityServer server,
            LoginInboundConnection inbound,
            GameProfile profile,
            boolean onlineMode,
            String serverIdHash);
}
