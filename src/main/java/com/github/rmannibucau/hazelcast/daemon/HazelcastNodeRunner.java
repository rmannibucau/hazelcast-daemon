package com.github.rmannibucau.hazelcast.daemon;

import com.github.rmannibucau.hazelcast.daemon.command.HazelcastJVM;
import com.github.rmannibucau.hazelcast.daemon.command.HazelcastMembers;
import com.github.rmannibucau.hazelcast.daemon.command.HazelcastNodeStart;
import com.github.rmannibucau.hazelcast.daemon.command.HazelcastNodeStop;
import com.github.rmannibucau.hazelcast.daemon.command.HazelcastStatus;
import io.airlift.command.Cli;
import io.airlift.command.Help;
import io.airlift.command.ParseException;

import java.util.Arrays;
import java.util.Collections;

public class HazelcastNodeRunner {
    public static void main(final String[] args) {
        final Cli<Runnable> cli = Cli.buildCli("hazelcast-daemon", Runnable.class)
                .withDescription("A simple hazelcast start/stop program")
                .withCommand(HazelcastNodeStart.class)
                .withCommand(HazelcastNodeStop.class)
                .withCommand(HazelcastStatus.class)
                .withCommand(HazelcastMembers.class)
                .withCommand(HazelcastJVM.class)
                .build();

        try {
            cli.parse(args).run();
        } catch (final ParseException iae) {
            if (args.length > 1 && "help".equals(args[0])) {
                Help.help(cli.getMetadata(), Arrays.asList(args[1]));
            } else {
                Help.help(cli.getMetadata(), Collections.<String>emptyList());
            }
        } catch (final CommandException ce) {
            System.err.println(ce.getMessage());
        }
    }

    private HazelcastNodeRunner() {
        // no-op
    }
}
