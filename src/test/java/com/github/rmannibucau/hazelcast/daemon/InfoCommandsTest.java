package com.github.rmannibucau.hazelcast.daemon;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class InfoCommandsTest extends LifecycleTest {
    private ByteArrayOutputStream out;
    private PrintStream originalOut;

    @Before
    public void slurpOutput() {
        out = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @After
    public void resetOutput() {
        System.setOut(originalOut);
    }

    @Test
    public void status() {
        HazelcastNodeRunner.main(new String[] { "status" });

        final String value = new String(out.toByteArray());
        assertThat(value, containsString("#Members: 1"));
    }

    @Test
    public void jvm() {
        HazelcastNodeRunner.main(new String[] { "jvm" });

        final String value = new String(out.toByteArray());
        assertThat(value, containsString("os.name = " + System.getProperty("os.name")));
    }

    @Test
    public void members() {
        HazelcastNodeRunner.main(new String[] { "members" });

        final String value = new String(out.toByteArray());
        assertThat(value, containsString(hazelcastInstance.getCluster().getLocalMember().getUuid()));
        assertThat(value, containsString(hazelcastInstance.getCluster().getLocalMember().getInetSocketAddress().getHostName()));
        assertThat(value, containsString(hazelcastInstance.getCluster().getLocalMember().getInetSocketAddress().getAddress().getHostAddress()));
    }
}
