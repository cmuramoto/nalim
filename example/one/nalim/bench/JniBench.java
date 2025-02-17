package one.nalim.bench;

import one.nalim.Link;
import one.nalim.Linker;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class JniBench {

    int a = 123;
    int b = 45678;

    @Param({"10", "100", "1000"})
    int length;

    long[] array;

    @Setup
    public void setup() {
        array = ThreadLocalRandom.current().longs(length).toArray();
    }

    @Benchmark
    public long add_jni() {
        return add(a, b);
    }

    @Benchmark
    public long add_nalim() {
        return raw_add(a, b);
    }

    @Benchmark
    public long max_jni() {
        return max(array, array.length);
    }

    @Benchmark
    public long max_nalim() {
        return raw_max(array, array.length);
    }

    static native int add(int a, int b);

    static native long max(long[] array, int length);

    @Link
    static native int raw_add(int a, int b);

    @Link
    static native long raw_max(long[] array, int length);

    static {
        System.loadLibrary("jnibench");
        Linker.linkClass(JniBench.class);
    }
}
