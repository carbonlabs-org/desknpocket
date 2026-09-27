package com.limelight.network;

import java.net.URI;
import java.util.Locale;

/** Only open hosted Tailscale authentication links returned by our embedded backend. */
final class LoginUrl {
    static boolean allowed(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (host == null || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("login.tailscale.com") || host.equals("controlplane.tailscale.com")
                    || host.equals("tailscale.com");
        } catch (Exception invalid) { return false; }
    }
}
