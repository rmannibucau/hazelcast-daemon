package com.github.rmannibucau.hazelcast.daemon.command.base;

public interface InfoCommand {
    String command();
    String host();
    int port();
}
