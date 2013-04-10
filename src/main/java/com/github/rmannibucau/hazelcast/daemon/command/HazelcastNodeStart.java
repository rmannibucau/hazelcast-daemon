package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.CommandException;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.airlift.command.Command;
import io.airlift.command.Option;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

@Command(name = "start", description = "Start a hazelcast node")
public class HazelcastNodeStart extends RemoteCommand implements Runnable {
    @Option(title = "configuration", name = { "--configuration", "-c" }, description = "the path to the hazelcast xml configuration")
    private String configuration;

    @Option(title = "instance name", name = { "--name", "-n" }, description = "the hazelcast instance name")
    private String instance;

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

        final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        final Thread shutdownThread = new ShutdownThread(hazelcastInstance);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        System.out.println("Node started with configuration " + hazelcastInstance.getConfig().getConfigurationUrl());

        final Random random = new Random();
        try {
            final ServerSocket serverSocket = new ServerSocket(port, 1, InetAddress.getByName(host));
            try {
                while (true) {
                    Socket socket = null;
                    StringBuilder sendCmd = new StringBuilder();
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
                            sendCmd.append((char) ch);
                            expected--;
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

                    if (command.equals(sendCmd.toString())) {
                        Runtime.getRuntime().removeShutdownHook(shutdownThread);
                        shutdownThread.run();
                        break;
                    } else {
                        System.err.println("Command " + sendCmd.toString() + " received.");
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

    private static class ShutdownThread extends Thread {
        private final HazelcastInstance instance;

        public ShutdownThread(final HazelcastInstance hazelcastInstance) {
            this.instance = hazelcastInstance;
        }

        @Override
        public void run() {
            instance.getLifecycleService().shutdown();
        }
    }
}
