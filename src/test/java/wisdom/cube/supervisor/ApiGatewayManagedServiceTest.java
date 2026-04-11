package wisdom.cube.supervisor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wisdom.cube.core.ApiGateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiGatewayManagedServiceTest {

    @Mock
    ApiGateway gateway;

    @Test
    void startStopAndHealth() {
        ApiGatewayManagedService svc = new ApiGatewayManagedService("api", gateway);
        assertFalse(svc.healthy());
        svc.start();
        assertTrue(svc.healthy());
        svc.stop();
        assertFalse(svc.healthy());
        verify(gateway).start();
        verify(gateway).stop();
    }
}
