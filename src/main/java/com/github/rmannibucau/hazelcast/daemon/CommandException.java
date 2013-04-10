package com.github.rmannibucau.hazelcast.daemon;

public class CommandException extends RuntimeException {
    public CommandException(final String message, final Exception e) {
        super(message, e);
    }
}
