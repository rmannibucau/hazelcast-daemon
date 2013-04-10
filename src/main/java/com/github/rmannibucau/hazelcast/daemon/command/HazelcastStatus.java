package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.command.base.ConstantRemoteCommand;
import io.airlift.command.Command;

@Command(name = "status", description = "Status of the JVM")
public class HazelcastStatus extends ConstantRemoteCommand {
}
