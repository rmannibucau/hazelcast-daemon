package com.github.rmannibucau.hazelcast.daemon.command.base;

import io.airlift.command.Command;

public abstract class ConstantRemoteCommand extends RemoteCommand implements InfoCommand {
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
        return getClass().getAnnotation(Command.class).name() + (char) 0;
    }
}
