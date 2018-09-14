package syncbridge;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

/**
 * Allows to build reactive sync API for async services.
 * Complete methods should be called from separate thread
 * (listening for Kafka topic or polling Rest endpoint)
 *
 * @param <K>
 * @param <V>
 */
public class SyncBridge<K, V> {

	private final Duration maxWaitTime;

	private final ConcurrentHashMap<K, List<CompletableFuture<V>>> futuresMap = new ConcurrentHashMap<>();

	public SyncBridge(Duration maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public Mono<V> wait(K key){
		CompletableFuture<V> future = new CompletableFuture<>();
		futuresMap.merge(key, singletonList(future), (list1, list2) ->
				Stream.of(list1, list2)
						.flatMap(Collection::stream)
						.collect(Collectors.toList()));
		return Mono.fromFuture(future)
				//wait and then remove from map and throw
				.or(Mono.delay(maxWaitTime)
						.doOnNext(time -> futuresMap.compute(key, (k, list) -> {
							if(list.size() > 1) {
								list.remove(future);
								return list;
							} else {
								return null;
							}
						}))
						.flatMap(time -> {
							TimeoutException error = new TimeoutException(
									"Max waiting time exceeded for key=" + key);
							return Mono.error(error);
						}));
	}

	public void complete(K key, V value){
		futuresMap.compute(key,	(k, list) -> {
			//complete the first future in the queue
			list.get(0).complete(value);
			//don't care about reference to first element
			// as list will be recreated on next wait() call
			return list.size() > 1 ? list.subList(1, list.size()) : null;
		});
	}

	public void completeExceptionally(K key, Throwable throwable){
		futuresMap.compute(key,	(k, list) -> {
			list.get(0).completeExceptionally(throwable);
			//don't care about reference to first element
			// as list will be recreated on next wait() call
			return list.size() > 1 ? list.subList(1, list.size()) : null;
		});
	}

}
