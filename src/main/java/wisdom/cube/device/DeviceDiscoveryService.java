package wisdom.cube.device;

/**
 * Discovers or refreshes devices on the network (Phase 7.2). Production integrations attach here;
 * default stub performs no I/O.
 */
public interface DeviceDiscoveryService {

    /**
     * Run discovery against the registry. Returns how many new devices were added (stub: always 0).
     */
    int refreshDiscoveries(LightDeviceRegistry registry);
}
