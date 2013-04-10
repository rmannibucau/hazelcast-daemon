package com.github.rmannibucau.hazelcast.daemon.command;

import io.airlift.command.Option;

public abstract class RemoteCommand {
    @Option(title = "admin port", name = { "--port", "-p" }, description = "the port used to listen shutdown command")
    protected int port = 8005;

    @Option(title = "admin host", name = { "--host", "-h" }, description = "the host used to listen shutdown command")
    protected String host = "localhost";

    @Option(title = "shutdown command", name = { "--command", "-cmd" }, description = "the shutdown command")
    protected String command = "SHUTDOWN";
}
