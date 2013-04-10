package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.CommandException;
import com.github.rmannibucau.hazelcast.daemon.command.base.AdminRemoteCommand;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

@Command(name = "start", description = "Start a hazelcast node")
public class HazelcastNodeStart extends AdminRemoteCommand implements Runnable {
    private static final String STATUS_FORMAT = "| %1$-36s | %2$-30s | %3$-15s | %4$-11s | %5$-12s |";
    private static final char LN = '\n';

    @Option(title = "configuration", name = { "--configuration", "-c" }, description = "the path to the hazelcast xml configuration")
    private String configuration;

    @Option(title = "instance name", name = { "--name", "-n" }, description = "the hazelcast instance name")
    private String instance;

    private HazelcastInstance hazelcastInstance;

    @Override
    public void run() {
        final Config config;
        if (configuration == null) {
            config = new XmlConfigBuilder().build();
        } else {
            try {
                config = new XmlConfigBuilder(configuration).build();
            } catch (final FileNotFoundException e) {
                throw new CommandException(e.getMessage(), e);
            }
        }

        if (instance != null) {
            config.setInstanceName(instance);
        }

        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        final Thread shutdownThread = new ShutdownThread(hazelcastInstance);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        System.out.println("Node started with configuration " + hazelcastInstance.getConfig().getConfigurationUrl());

        final Random random = new Random();
        try {
            final ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getByName(host));

            System.out.println("Starting admin socket on " + host + ":" + port);

            try {
                while (true) {
                    Socket socket = null;
                    final StringBuilder sentCmd = new StringBuilder();
                    try {
                        socket = serverSocket.accept();
                        socket.setSoTimeout(10 * 1000);

                        final InputStream stream = socket.getInputStream();

                        int expected = 1024;
                        while (expected < command.length()) {
                            expected += (random.nextInt() % 1024);
                        }

                        while (expected > 0) {
                            int ch;
                            try {
                                ch = stream.read();
                            } catch (final IOException e) {
                                ch = -1;
                            }

                            if (ch < 32)  {
                                break;
                            }
                            sentCmd.append((char) ch);
                            expected--;
                        }

                        final String cmd = sentCmd.toString();
                        if (command.equals(cmd)) {
                            write(socket, shuttingDown());
                            Runtime.getRuntime().removeShutdownHook(shutdownThread);
                            shutdownThread.run();
                            write(socket, shutdown());
                            break;
                        } else if ("status".equals(cmd)) {
                            write(socket, status());
                        } else if ("members".equals(cmd)) {
                            write(socket, members());
                        } else {
                            System.err.println("Command " + cmd + " received.");
                        }
                    } finally {
                        try {
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (final IOException e) {
                            // no-op
                        }
                    }
                }
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
        } catch (final IOException e) {
            throw new CommandException(e.getMessage(), e);
        }
    }

    private String shuttingDown() throws IOException {
        return "Instance shutting down...\n";
    }

    private String shutdown() throws IOException {
        return "Instance shutted down.";
    }

    private String status() throws IOException {
        if (hazelcastInstance == null) {
            return "No instance available";
        }

        final Runtime runtime = Runtime.getRuntime();
        final long maxMemory = runtime.maxMemory();
        final long totalMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();

        final int cpu = runtime.availableProcessors();
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        final StringBuilder builder = new StringBuilder();
        builder.append("----- Hazelcast Properties -----").append(LN);
        builder.append("Instance: ").append(hazelcastInstance.getName()).append(LN);
        builder.append("#Members: ").append(hazelcastInstance.getCluster().getMembers().size()).append(LN);
        builder.append("Configuration url: ").append(hazelcastInstance.getConfig().getConfigurationUrl()).append(LN);
        builder.append("Configuration: ").append(hazelcastInstance.getConfig()).append(LN);
        // TODO: dump config
        builder.append("----- Machine Properties -----").append(LN);
        builder.append("Free memory: ").append(freeMemory / 1024).append(LN);
        builder.append("Total memory: ").append(totalMemory / 1024).append(LN);
        builder.append("Max memory: ").append(maxMemory / 1024).append(LN);
        builder.append("Total free memory: ").append((freeMemory + (maxMemory - totalMemory)) / 1024).append(LN);
        builder.append("#CPU: ").append(cpu).append(LN);
        builder.append("----- System Properties -----").append(LN);
        for (final String key : System.getProperties().stringPropertyNames()) {
            builder.append(key).append(" = ").append(System.getProperty(key)).append(LN);
        }
        builder.append("----- Runtime Properties -----").append(LN);
        for (final Method m : RuntimeMXBean.class.getMethods()) {
            final String name = m.getName();
            if (Modifier.isPublic(m.getModifiers()) && name.startsWith("get") && m.getParameterTypes().length == 0
                    && !"getSystemProperties".equals(name) && !name.endsWith("Path") && !"getInputArguments".equals(name)) {
                try {
                    builder.append(name.substring("get".length())).append(" = ").append(m.invoke(runtimeMXBean)).append(LN);
                } catch (final Exception e) {
                    // no-op
                }
            }
        }
        return builder.toString();
    }

    private String members() throws IOException {
        if (hazelcastInstance == null) {
            return "No instance available";
        }

        final Set<Member> members = hazelcastInstance.getCluster().getMembers();
        final StringBuilder builder = new StringBuilder();
        final String header = String.format(STATUS_FORMAT, "UUID", "Host", "IP", "Lite member", "Local member");
        final char[] secondLine = new char[header.length()];
        final char[] firstAndLast = new char[header.length()];
        Arrays.fill(secondLine, '=');
        Arrays.fill(firstAndLast, '-');

        builder.append(firstAndLast).append(LN).append(header).append("\n").append(secondLine).append(LN);
        for (final Member m : members) {
            final InetSocketAddress address = m.getInetSocketAddress();
            builder.append(String.format(STATUS_FORMAT, m.getUuid(), address.getHostName(), address.getAddress().getHostAddress(), m.isLiteMember(), m.localMember()))
                    .append(LN);
        }
        builder.append(firstAndLast);
        return builder.toString();
    }

    private static void write(final Socket socket, final String text) throws IOException {
        final OutputStream outputStream = socket.getOutputStream();
        outputStream.write((text + LN).getBytes());
        outputStream.flush();
    }

    private static class ShutdownThread extends Thread {
        private final HazelcastInstance instance;

        public ShutdownThread(final HazelcastInstance hazelcastInstance) {
            this.instance = hazelcastInstance;
        }

        @Override
        public void run() {
            if (instance == null) {
                return;
            }
            instance.getLifecycleService().shutdown();
        }
    }
}
