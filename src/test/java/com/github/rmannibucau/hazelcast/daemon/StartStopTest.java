package com.github.rmannibucau.hazelcast.daemon;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class StartStopTest {
    private static final int WAIT_MS = 500;

    @Test(timeout = 10000)
    public void check() {
        final Thread startThread = new Thread() {
            @Override
            public void run() {
                HazelcastNodeRunner.main(new String[] { "start", "--name", "test" });
            }
        };
        startThread.start();

        while (Hazelcast.getHazelcastInstanceByName("test") == null) {
            try {
                Thread.sleep(WAIT_MS);
            } catch (final InterruptedException e) {
                // no-op
            }
        }

        final HazelcastInstance test = Hazelcast.getHazelcastInstanceByName("test");
        assertNotNull(test);

        final IMap<Object,Object> map = test.getMap("map");
        assertNotNull(map);
        map.put("entry", "value");
        assertEquals("value", map.get("entry"));

        HazelcastNodeRunner.main(new String[] { "stop" });

        while (Hazelcast.getHazelcastInstanceByName("test") != null) {
            try {
                Thread.sleep(WAIT_MS);
            } catch (final InterruptedException e) {
                // no-op
            }
        }

        assertNull(Hazelcast.getHazelcastInstanceByName("test"));
    }
}
