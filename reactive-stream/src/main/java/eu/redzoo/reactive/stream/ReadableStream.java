package eu.redzoo.reactive.stream;

import java.io.Closeable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Subscriber;


public interface ReadableStream<T> extends Closeable {
        
    <V> ReadableStream<V> map(Function<? super T, ? extends V> fn);
    
    void consume(Subscriber<? super T> subscriber);
    
    void consume(Consumer<? super T> consumer);
    
    void consume(Consumer<? super T> consumer, Consumer<? super Throwable> errorConsumer);
}
    