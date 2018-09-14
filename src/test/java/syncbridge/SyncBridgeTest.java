package syncbridge;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class SyncBridgeTest {

	private SyncBridge<String, String> syncBridge = new SyncBridge<>(Duration.ofMillis(100));

	@Test
	public void shouldComplete(){
		Mono<String> mono = syncBridge.wait("completeKey");

		syncBridge.complete("completeKey", "completeValue");

		StepVerifier.create(mono)
				.expectNext("completeValue")
				.verifyComplete();
	}

	@Test
	public void shouldCompleteWithError(){
		Mono<String> mono = syncBridge.wait("completeKey");

		syncBridge.completeExceptionally("completeKey", new RuntimeException());

		StepVerifier.create(mono)
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test
	public void shouldThrowOnTimeout(){
		StepVerifier.create(syncBridge.wait("timeoutTest"))
				.expectError(TimeoutException.class)
				.verify();
	}

}
