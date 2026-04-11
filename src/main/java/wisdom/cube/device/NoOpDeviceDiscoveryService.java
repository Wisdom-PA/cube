package wisdom.cube.device;

/**
 * Stub discovery: no network calls; marks in-memory devices reachable (F6.T2 / F6.T3 health hook).
 */
public final class NoOpDeviceDiscoveryService implements DeviceDiscoveryService {

    @Override
    public int refreshDiscoveries(LightDeviceRegistry registry) {
        if (registry instanceof InMemoryLightDeviceRegistry mem) {
            mem.refreshReachabilityAll(true);
        }
        return 0;
    }
}
