package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.command.base.AdminRemoteCommand;
import io.airlift.command.Command;

@Command(name = "stop", description = "Stop a hazelcast node")
public class HazelcastNodeStop extends AdminRemoteCommand implements Runnable {
    @Override
    public String command() {
        return command + (char) 0;
    }
}
