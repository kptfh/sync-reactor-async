# sync-reactor-async
Allows to build reactive sync API for async services. While migrating to async approach, some legacy services may still require sync interfaces. Basic approach for migration is to call some method and then wait for message in Kafka or periodically poll Rest endpoint. Traditional blocking approach may run out of threads on highly loaded service. Reactive approach guaranty much more higher capacity.


