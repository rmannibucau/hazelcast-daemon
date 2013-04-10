package com.github.rmannibucau.hazelcast.daemon.command.base;

import io.airlift.command.Option;

public abstract class AdminRemoteCommand extends RemoteCommand implements InfoCommand {
    @Option(title = "shutdown command", name = { "--command", "-cmd" }, description = "the shutdown command")
    protected String command = "SHUTDOWN";

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String command() {
        return command;
    }
}
