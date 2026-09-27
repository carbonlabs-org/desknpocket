package com.limelight.network;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginUrlTest {
    @Test public void acceptsHostedAuthEndpoints() {
        assertTrue(LoginUrl.allowed("https://login.tailscale.com/a/test"));
        assertTrue(LoginUrl.allowed("https://controlplane.tailscale.com/a/test"));
        assertTrue(LoginUrl.allowed("https://tailscale.com/a/test"));
    }
    @Test public void rejectsLookalikeAndSuffixHosts() {
        assertFalse(LoginUrl.allowed("https://login.tailscale.com.example.org/a/test"));
        assertFalse(LoginUrl.allowed("https://eviltailscale.com/a/test"));
    }
    @Test public void rejectsCredentialsAndFragments() {
        assertFalse(LoginUrl.allowed("https://user@login.tailscale.com/a/test"));
        assertFalse(LoginUrl.allowed("https://login.tailscale.com/a/test#data"));
    }
    @Test public void rejectsInsecureSchemesAndAlternatePorts() {
        assertFalse(LoginUrl.allowed("http://login.tailscale.com/a/test"));
        assertFalse(LoginUrl.allowed("intent://login.tailscale.com/a/test"));
        assertFalse(LoginUrl.allowed("https://login.tailscale.com:8443/a/test"));
    }
    @Test public void rejectsEmptyMalformedAndRelativeLinks() {
        assertFalse(LoginUrl.allowed("")); assertFalse(LoginUrl.allowed(null));
        assertFalse(LoginUrl.allowed("/a/test")); assertFalse(LoginUrl.allowed("https://login.tailscale.com/a/ test"));
    }
}
