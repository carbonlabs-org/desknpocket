package com.limelight.network;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhysicalNetworkTest {
    @Test public void vpnCannotBecomeItsOwnTransportEvenWhenValidated() {
        assertEquals(-1, PhysicalNetwork.rank(true, false, true, true, true));
    }
    @Test public void isolatedNetworkIsNotEligible() {
        assertEquals(-1, PhysicalNetwork.rank(false, true, true, true, true));
    }
    @Test public void validatedCellularBeatsUnvalidatedWifi() {
        assertTrue(PhysicalNetwork.rank(true, true, true, false, false)
                > PhysicalNetwork.rank(true, true, false, true, true));
    }
    @Test public void usableDnsBeatsMeteringPreference() {
        assertTrue(PhysicalNetwork.rank(true, true, true, true, false)
                > PhysicalNetwork.rank(true, true, true, false, true));
    }
    @Test public void unmeteredWinsWithinSameConnectivityTier() {
        assertTrue(PhysicalNetwork.rank(true, true, true, true, true)
                > PhysicalNetwork.rank(true, true, true, true, false));
    }
}
