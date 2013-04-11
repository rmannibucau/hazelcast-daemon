package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.command.base.ConstantRemoteCommand;
import io.airlift.command.Command;

@Command(name = "jvm", description = "Summary of the JVM")
public class HazelcastJVM extends ConstantRemoteCommand {
}
