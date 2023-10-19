/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.collections.haa;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.gds.mem.MemoryUsage;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static io.qala.datagen.RandomShortApi.bool;
import static io.qala.datagen.RandomShortApi.integer;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.gds.mem.HugeArrays.PAGE_SIZE;

/**
 * Many of the following tests were taken from the AtomicLongArray test from the OpenJDK sources, and then adjusted for
 * `byte`.
 * We also include a check that we don't leak threads.
 *
 * @see <a href="https://hg.openjdk.java.net/jdk/jdk13/file/9e0c80381e32/jdk/test/java/util/concurrent/tck/AtomicLongArrayTest.java">OpenJDK sources for AtomicLongArrayTest.java</a>
 * @see <a href="https://hg.openjdk.java.net/jdk/jdk13/file/9e0c80381e32/jdk/test/java/util/concurrent/tck/Atomic8Test.java">OpenJDK sources for Atomic8Test.java</a>
 * @see <a href="https://hg.openjdk.java.net/jdk/jdk13/file/9e0c80381e32/jdk/test/java/util/concurrent/tck/JSR166TestCase.java">OpenJDK sources for JSR166TestCase.java</a>
 * @see <a href="https://hg.openjdk.java.net/jdk/jdk13/file/9e0c80381e32/jdk/test/java/util/concurrent/atomic/LongAdderDemo.java">OpenJDK sources for LongAdderDemo.java</a>
 */
final class HugeAtomicIntArrayTest {

    private static final int SIZE = 20;
    private static final long LONG_DELAY_MS = 10_000L;

    /**
     * constructor creates array of given size with all elements zero
     */
    @Test
    void testConstructor() {
        testArray(SIZE, array -> {
            for (int i = 0; i < SIZE; i++) {
                Assertions.assertEquals(0, array.get(i));
            }
        });
    }

    @Test
    void testIndexing() {
        testArray(SIZE, array -> {
            for (int index : new int[]{-1, SIZE}) {
                assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(index));
                assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.set(index, 1));
                assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.compareAndSet(index, 1, 2));
            }
        });
    }

    /**
     * get returns the last value set at index
     */
    @Test
    void testGetSet() {
        testArray(SIZE, array -> {
            for (int i = 0; i < SIZE; i++) {
                array.set(i, 1);
                Assertions.assertEquals(1, array.get(i));
                array.set(i, 2);
                Assertions.assertEquals(2, array.get(i));
                array.set(i, -3);
                Assertions.assertEquals(-3, array.get(i));
            }
        });
    }

    /**
     * compareAndSet succeeds in changing value if equal to expected else fails
     */
    @Test
    void testCompareAndSet() {
        testArray(SIZE, array -> {
            for (int i = 0; i < SIZE; i++) {
                array.set(i, 1);
                Assertions.assertTrue(array.compareAndSet(i, 1, 2));
                Assertions.assertTrue(array.compareAndSet(i, 2, -4));
                Assertions.assertEquals(-4, array.get(i));
                Assertions.assertFalse(array.compareAndSet(i, -5, 7));
                Assertions.assertEquals(-4, array.get(i));
                Assertions.assertTrue(array.compareAndSet(i, -4, 7));
                Assertions.assertEquals(7, array.get(i));
            }
        });
    }

    /**
     * compareAndSet in one thread enables another waiting for value
     * to succeed
     */
    @Test
    void testCompareAndSetInMultipleThreads() throws InterruptedException {
        testArray(1, array -> {
            array.set(0, 1);
            Thread t = new Thread(new CheckedRunnable() {
                public void realRun() {
                    while (!array.compareAndSet(0, 2, 3)) {
                        Thread.yield();
                    }
                }
            });

            t.start();
            Assertions.assertTrue(array.compareAndSet(0, 1, 2));
            t.join(LONG_DELAY_MS);
            assertFalse(t.isAlive());
            Assertions.assertEquals(3, array.get(0));
        });
    }

    @Test
    void testCompareAndExchange() {
        testArray(SIZE, array -> {
            for (int i = 0; i < SIZE; i++) {
                array.set(i, 1);
                Assertions.assertEquals(1, array.compareAndExchange(i, 1, 2));
                Assertions.assertEquals(2, array.compareAndExchange(i, 2, -4));
                Assertions.assertEquals(-4, array.get(i));
                Assertions.assertEquals(-4, array.compareAndExchange(i, -5, 7));
                Assertions.assertEquals(-4, array.get(i));
                Assertions.assertEquals(-4, array.compareAndExchange(i, -4, 7));
                Assertions.assertEquals(7, array.get(i));
            }
        });
    }

    @Test
    void testCompareAndExchangeInMultipleThreads() throws InterruptedException {
        testArray(1, array -> {
            array.set(0, 1);
            Thread t = new Thread(new CheckedRunnable() {
                public void realRun() {
                    while (array.compareAndExchange(0, 2, 3) != 2) {
                        Thread.yield();
                    }
                }
            });

            t.start();
            Assertions.assertEquals(1L, array.compareAndExchange(0, 1, 2));
            t.join(LONG_DELAY_MS);
            assertFalse(t.isAlive());
            Assertions.assertEquals(3, array.get(0));
        });
    }

    static class Counter extends CheckedRunnable {
        final HugeAtomicIntArray array;
        int decs;

        Counter(HugeAtomicIntArray array) { this.array = array; }

        public void realRun() {
            for (; ; ) {
                boolean done = true;
                for (int i = 0; i < array.size(); i++) {
                    var v = array.get(i);
                    assertTrue(v >= 0);
                    if (v != 0) {
                        done = false;
                        if (array.compareAndSet(i, v, (v - 1))) {
                            decs++;
                        }
                    }
                }
                if (done) {
                    break;
                }
            }
        }
    }

    /**
     * Multiple threads using same array of counters successfully
     * update a number of times equal to total count
     */
    @Test
    void testCountingInMultipleThreads() throws InterruptedException {
        testArray(SIZE, array -> {
            byte countdown = 100;
            for (int i = 0; i < SIZE; i++) {
                array.set(i, countdown);
            }
            Counter c1 = new Counter(array);
            Counter c2 = new Counter(array);
            Thread t1 = newStartedThread(c1);
            Thread t2 = newStartedThread(c2);
            t1.join();
            t2.join();
            assertEquals(c1.decs + c2.decs, SIZE * countdown);
        });
    }

    private int randomInt(int min, int max) {
        return (ThreadLocalRandom.current().nextInt(min, max));
    }

    @Test
    void shouldSetAndGet() {
        testArray(10, array -> {
            int index = integer(2, 8);
            var value = randomInt(42, 120);

            array.set(index, value);
            assertEquals(value, array.get(index));
        });
    }

    @Test
    void shouldAddAndGet() {
        testArray(10, array -> {
            int index = integer(2, 8);
            var value = randomInt(42, 120);
            var delta = randomInt(0, 42);

            array.set(index, value);
            array.getAndAdd(index, delta);

            assertEquals((value + delta), array.get(index));
        });
    }

    @Test
    void shouldReportSize() {
        int size = integer(10, 20);
        testArray(size, array -> assertEquals(size, array.size()));
    }

    @Test
    void shouldFreeMemoryUsed() {
        int size = integer(10, 20);
        long expected = MemoryUsage.sizeOfIntArray(size);
        testArray(size, array -> {
            long freed = array.release();
            assertThat(freed).matches(v -> v == expected || v == expected + 24);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "0, 40",
        "100, 440",
        "100_000_000_000, 400_122_070_368",
    })
    void shouldComputeMemoryEstimation(long size, long estimation) {
        assertThat(HugeAtomicIntArray.memoryEstimation(size)).isEqualTo(estimation);
    }
    @Test
    void shouldFailForNegativeMemRecSize() {
        assertThrows(AssertionError.class, () -> HugeAtomicByteArray.memoryEstimation(-1L));
    }

    @Test
    void testSetAll() {
        var pool = Executors.newCachedThreadPool();
        try {
            int nthreads = Runtime.getRuntime().availableProcessors() * 2;
            var arraySize = 42_1337;
            var phaser = new Phaser(nthreads + 1);
            var aa = singleArray(arraySize); // 1 page
            var tasks = new ArrayList<GetTask>();
            aa.setAll(42);
            for (int i = 0; i < nthreads; ++i) {
                var t = new GetTask(aa, phaser);
                tasks.add(t);
                pool.execute(t);
            }
            phaser.arriveAndAwaitAdvance();

            tasks.forEach(t -> assertEquals(42 * arraySize, t.result));

        } finally {
            pool.shutdown();
        }
    }

    private static final class GetTask implements Runnable {
        private final HugeAtomicIntArray array;
        private final Phaser phaser;
        volatile long result;

        private GetTask(HugeAtomicIntArray array, Phaser phaser) {
            this.array = array;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            for (int i = 0; i < array.size(); i++) {
                result += array.get(i);
            }
            phaser.arrive();
        }
    }

    @FunctionalInterface
    interface HalaFunction {

        void apply(HugeAtomicIntArray array);
    }

    @Test
    void testGetAndAddIsWithinBoundsForPagedArray() {
        var size = PAGE_SIZE * 2 + 1; // We want an array with three pages
        var index = PAGE_SIZE + 1;    // and look up some index larger than what fits in a single page
        var array = pagedArray(size);
        assertDoesNotThrow(() -> array.getAndAdd(index,  1));
    }

    @Test
    void testGetAndAddParallel() {
        int incsPerThread = 10_000;
        int ncpu = Runtime.getRuntime().availableProcessors();
        int maxThreads = ncpu * 2;
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            testArray(1, array -> {
                for (int i = 1; i <= maxThreads; i <<= 1) {
                    array.set(0, 0);
                    casTest(i, incsPerThread, array, pool, a -> a.getAndAdd(0, 1));
                }
            });
        } finally {
            pool.shutdown();
            LockSupport.parkNanos(MILLISECONDS.toNanos(100));
        }
    }

    private static void casTest(
        int nthreads,
        int incs,
        HugeAtomicIntArray array,
        Executor pool,
        HalaFunction arrayFn
    ) {
        Phaser phaser = new Phaser(nthreads + 1);
        for (int i = 0; i < nthreads; ++i) {
            pool.execute(new CasTask(array, phaser, incs, arrayFn));
        }
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndAwaitAdvance();
        long total = (long) nthreads * incs;
        assertEquals(array.get(0), total);
    }

    private static final class CasTask implements Runnable {
        final HugeAtomicIntArray adder;
        final Phaser phaser;
        final int incs;
        volatile long result;
        final HalaFunction arrayFn;

        CasTask(HugeAtomicIntArray adder, Phaser phaser, int incs, HalaFunction arrayFn) {
            this.adder = adder;
            this.phaser = phaser;
            this.incs = incs;
            this.arrayFn = arrayFn;
        }

        public void run() {
            phaser.arriveAndAwaitAdvance();
            HugeAtomicIntArray array = adder;
            for (int i = 0; i < incs; ++i) {
                arrayFn.apply(array);
            }
            result = array.get(0);
            phaser.arrive();
        }
    }

    private void testArray(int size, ThrowingConsumer<HugeAtomicIntArray> block) {
        if (bool()) {
            block.accept(singleArray(size));
            block.accept(pagedArray(size));
        } else {
            block.accept(pagedArray(size));
            block.accept(singleArray(size));
        }
    }

    private HugeAtomicIntArray singleArray(final int size) {
        return HugeAtomicIntArrayFactory.Single.of(size, PassThroughPageCreator.INSTANCE);
    }

    private HugeAtomicIntArray pagedArray(final int size) {
        return HugeAtomicIntArrayFactory.Paged.of(size, PassThroughPageCreator.INSTANCE);
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    private Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * The first exception encountered if any threadAssertXXX method fails.
     */
    private static final AtomicReference<Throwable> threadFailure
        = new AtomicReference<>(null);

    /**
     * Records an exception so that it can be rethrown later in the test
     * harness thread, triggering a test case failure.  Only the first
     * failure is recorded; subsequent calls to this method from within
     * the same test have no effect.
     */
    private static void threadRecordFailure(Throwable t) {
        threadFailure.compareAndSet(null, t);
    }

    /**
     * Records the given exception using {@link #threadRecordFailure},
     * then rethrows the exception, wrapping it in an
     * AssertionFailedError if necessary.
     */
    private static void threadUnexpectedException(Throwable t) {
        threadRecordFailure(t);
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new AssertionFailedError("unexpected exception: " + t, t);
        }
    }

    private abstract static class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {
                realRun();
            } catch (Throwable fail) {
                threadUnexpectedException(fail);
            }
        }
    }

    private static class PassThroughPageCreator implements PageCreator.IntPageCreator {

        static PassThroughPageCreator INSTANCE = new PassThroughPageCreator();

        private PassThroughPageCreator() {}

        @Override
        public void fill(int[][] pages, int lastPageSize, int pageShift) {
            int lastPageIndex = pages.length - 1;
            int pageSize = 1 << pageShift;

            IntStream.range(0, lastPageIndex).forEach(idx -> pages[idx] = new int[pageSize] );

            pages[lastPageIndex] = new int[lastPageSize];
        }

        @Override
        public void fillPage(int[] page, long base) {
            // NO-OP
        }
    }
}
