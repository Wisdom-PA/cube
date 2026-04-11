package wisdom.cube.device;

/**
 * Stub discovery: no network calls; {@link #refreshDiscoveries} is a no-op.
 */
public final class NoOpDeviceDiscoveryService implements DeviceDiscoveryService {

    @Override
    public int refreshDiscoveries(LightDeviceRegistry registry) {
        return 0;
    }
}
