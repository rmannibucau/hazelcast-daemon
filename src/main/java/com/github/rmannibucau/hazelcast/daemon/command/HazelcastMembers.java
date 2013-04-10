package com.github.rmannibucau.hazelcast.daemon.command;

import com.github.rmannibucau.hazelcast.daemon.command.base.ConstantRemoteCommand;
import io.airlift.command.Command;

@Command(name = "members", description = "Show members of the cluster")
public class HazelcastMembers extends ConstantRemoteCommand {
}
