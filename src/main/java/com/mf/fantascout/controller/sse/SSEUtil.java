package com.mf.fantascout.controller.sse;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class SSEUtil<E> {

    private final Sinks.Many<ServerSentEvent<E>> sink;

    public SSEUtil() { this.sink = Sinks.many().multicast().onBackpressureBuffer();}

    private ServerSentEvent<E> buildSSE(E signal) { return ServerSentEvent.<E>builder(signal).build();}

    public void emitSignal(E signal) {
        try {
            sink.tryEmitNext(buildSSE(signal));
        } catch(Sinks.EmissionException e) {
            e.printStackTrace();
        }
    }

    public Flux<ServerSentEvent<E>> subscribe() {return sink.asFlux();}


}