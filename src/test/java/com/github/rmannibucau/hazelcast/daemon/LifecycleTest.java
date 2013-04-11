package com.github.rmannibucau.hazelcast.daemon;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class LifecycleTest {
    private static final int WAIT_MS = 500;

    protected static HazelcastInstance hazelcastInstance;

    @BeforeClass
    public static void start() {
        final Thread startThread = new Thread() {
            @Override
            public void run() {
                HazelcastNodeRunner.main(new String[] { "start", "--name", "test" });
            }
        };
        startThread.start();


        while ((hazelcastInstance = Hazelcast.getHazelcastInstanceByName("test")) == null) {
            try {
                Thread.sleep(WAIT_MS);
            } catch (final InterruptedException e) {
                // no-op
            }
        }
    }

    @AfterClass
    public static void stop() {
        HazelcastNodeRunner.main(new String[] { "stop" });

        while (Hazelcast.getHazelcastInstanceByName("test") != null) {
            try {
                Thread.sleep(WAIT_MS);
            } catch (final InterruptedException e) {
                // no-op
            }
        }
    }
}
