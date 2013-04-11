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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Command(name = "start", description = "Start a hazelcast node")
public class HazelcastNodeStart extends AdminRemoteCommand implements Runnable {
    private static final String STATUS_FORMAT = "| %1$-36s | %2$-30s | %3$-15s | %4$-11s | %5$-12s |";
    private static final char LN = '\n';
    private static final String[][] JAVA_CTRL_CHARS_ESCAPE = {
            {"\b", "\\b"},
            {"\n", "\\n"},
            {"\t", "\\t"},
            {"\f", "\\f"},
            {"\r", "\\r"}
    };

    @Option(title = "configuration", name = {"--configuration", "-c"}, description = "the path to the hazelcast xml configuration")
    private String configuration;

    @Option(title = "instance name", name = {"--name", "-n"}, description = "the hazelcast instance name")
    private String instance;

    private HazelcastInstance hazelcastInstance;

    private static void appendProps(final StringBuilder builder, final Class<?> clazz, final Object instance) {
        final Map<String, String> props = new HashMap<String, String>();
        findInfo(clazz, instance, props);
        final List<String> sortedKeys = new ArrayList<String>(props.keySet());
        Collections.sort(sortedKeys);
        for (final String key : sortedKeys) {
            appendProperty(builder, key, props.get(key));
        }
    }

    private static void findInfo(final Class<?> clazz, final Object instance, final Map<String, String> out) {
        for (final Method m : clazz.getMethods()) {
            final String name = m.getName();
            if (Modifier.isPublic(m.getModifiers()) && m.getParameterTypes().length == 0
                    && (name.startsWith("get") || name.startsWith("is"))
                    // exclusions for runtime mx bean
                    && !"getSystemProperties".equals(name) && !name.endsWith("Path") && !"getInputArguments".equals(name)) {
                final String key;
                if (name.startsWith("get")) {
                    key = name.substring("get".length());
                } else {
                    key = name.substring("is".length());
                }

                try {
                    out.put(key, m.invoke(instance).toString());
                } catch (final Exception e) {
                    // no-op
                }
            }
        }
    }

    private static void appendProperty(final StringBuilder builder, final String key, final String value) {
        builder.append(key).append(" = ").append(escape(value)).append(LN);
    }

    private static String escape(final String value) {
        if (value == null) {
            return value;
        }

        String escaped = value;
        for (final String[] item : JAVA_CTRL_CHARS_ESCAPE) {
            escaped = escaped.replace(item[0], item[1]);
        }
        return escaped;
    }

    private static void write(final Socket socket, final String text) throws IOException {
        final OutputStream outputStream = socket.getOutputStream();
        outputStream.write((text + LN).getBytes());
        outputStream.flush();
    }

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

                            if (ch < 32) {
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
                        } else if ("jvm".equals(cmd)) {
                            write(socket, jvm());
                        } else {
                            System.err.println("Invalid command '" + escape(cmd) + "' received.");
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

        final StringBuilder builder = new StringBuilder();
        builder.append("----- Hazelcast Information -----").append(LN);
        builder.append("Instance: ").append(hazelcastInstance.getName()).append(LN);
        builder.append("#Members: ").append(hazelcastInstance.getCluster().getMembers().size()).append(LN);
        builder.append("Configuration url: ").append(hazelcastInstance.getConfig().getConfigurationUrl()).append(LN);
        builder.append("Configuration:").append(LN).append(slurpAndFormat(hazelcastInstance.getConfig().getConfigurationUrl(), "    %2d. ")).append(LN);
        return builder.toString();
    }

    private String slurpAndFormat(final URL configurationUrl, final String prefix) {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);

        InputStream is = null;
        try {
            is = configurationUrl.openStream();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(is);
            removeComments(doc);

            // format
            String xml;
            try {
                final StreamResult xmlOutput = new StreamResult(new StringWriter());
                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                try { // old versions
                    transformerFactory.setAttribute("indent-number", 2);
                } catch (final IllegalArgumentException e) {
                    // no-op
                }

                final Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                try { // new versions
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
                } catch (final IllegalArgumentException e) {
                    // no-op
                }
                transformer.transform(new DOMSource(doc), xmlOutput);
                xml = xmlOutput.getWriter().toString();
            } catch (final Exception e) {
                return doc.toString();
            }

            final StringBuilder prefixed = new StringBuilder();
            final BufferedReader reader = new BufferedReader(new StringReader(xml));

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                prefixed.append(String.format(prefix, lineNumber++)).append(line).append(LN);
            }
            return prefixed.toString();
        } catch (final Exception e) {
            return "!!!Can't read the configuration!!!";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // no-op
                }
            }
        }
    }

    private String jvm() throws IOException {
        if (hazelcastInstance == null) {
            return "No instance available";
        }

        final Runtime runtime = Runtime.getRuntime();
        final long maxMemory = runtime.maxMemory();
        final long totalMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final int cpu = runtime.availableProcessors();

        final StringBuilder builder = new StringBuilder();

        builder.append("----- Quick Summary -----").append(LN);
        builder.append("Free memory: ").append(freeMemory / 1024).append(LN);
        builder.append("Total memory: ").append(totalMemory / 1024).append(LN);
        builder.append("Max memory: ").append(maxMemory / 1024).append(LN);
        builder.append("Total free memory: ").append((freeMemory + (maxMemory - totalMemory)) / 1024).append(LN);
        builder.append("#CPU: ").append(cpu).append(LN);

        builder.append("----- Runtime Properties -----").append(LN);
        appendProps(builder, RuntimeMXBean.class, ManagementFactory.getRuntimeMXBean());

        builder.append("----- Thread Properties -----").append(LN);
        appendProps(builder, ThreadMXBean.class, ManagementFactory.getThreadMXBean());

        builder.append("----- Memory Properties -----").append(LN);
        appendProps(builder, MemoryMXBean.class, ManagementFactory.getMemoryMXBean());

        builder.append("----- ClassLoading Properties -----").append(LN);
        appendProps(builder, ClassLoadingMXBean.class, ManagementFactory.getClassLoadingMXBean());

        builder.append("----- OperatingSystem Properties -----").append(LN);
        appendProps(builder, OperatingSystemMXBean.class, ManagementFactory.getOperatingSystemMXBean());

        builder.append("----- Compilation Properties -----").append(LN);
        appendProps(builder, CompilationMXBean.class, ManagementFactory.getCompilationMXBean());

        builder.append("----- System Properties -----").append(LN);
        final List<String> systPropKeys = new ArrayList<String>(System.getProperties().stringPropertyNames());
        Collections.sort(systPropKeys); // easier to search then
        for (final String key : systPropKeys) {
            appendProperty(builder, key, System.getProperty(key));
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

    private static boolean removeComments(final Node node) {
        if (node.getNodeType() == Node.COMMENT_NODE) {
            removeChildren(node);
            node.getParentNode().removeChild(node);
            return true;
        } else {
            final NodeList list = node.getChildNodes();

            boolean removed = false;
            int idx = 0;
            while (node.getChildNodes().getLength() > idx) {
                if (!removeComments(list.item(idx))) {
                    idx++;
                } else {
                    removed = true;
                }
            }
            return removed;
        }
    }

    private static void removeChildren(final Node node) { // avoid blank lines
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
        final Node sibling = node.getNextSibling();
        if (sibling != null && sibling.getNodeType() == Node.TEXT_NODE && sibling.getNodeValue().trim().isEmpty()) {
            node.getParentNode().removeChild(sibling);
        }
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
