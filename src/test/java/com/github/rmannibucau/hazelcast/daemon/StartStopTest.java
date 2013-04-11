package com.github.rmannibucau.hazelcast.daemon;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StartStopTest extends LifecycleTest {
    @Test
    public void check() {
        final HazelcastInstance test = Hazelcast.getHazelcastInstanceByName("test");
        assertNotNull(test);

        final IMap<Object,Object> map = test.getMap("map");
        assertNotNull(map);
        map.put("entry", "value");
        assertEquals("value", map.get("entry"));
    }
}
