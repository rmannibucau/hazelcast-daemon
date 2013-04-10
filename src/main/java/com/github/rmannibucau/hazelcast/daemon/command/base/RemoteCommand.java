package com.github.rmannibucau.hazelcast.daemon.command.base;

import io.airlift.command.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class RemoteCommand implements InfoCommand, Runnable {
    @Option(title = "admin port", name = { "--port", "-p" }, description = "the port used to listen shutdown command")
    protected int port = 8005;

    @Option(title = "admin host", name = { "--host", "-h" }, description = "the host used to listen shutdown command")
    protected String host = "localhost";

    protected String sendCommand() {
        final StringBuilder out = new StringBuilder();
        final InfoCommand cmd = this;

        OutputStream stream = null;
        Socket socket = null;
        try {
            socket = new Socket(cmd.host(), cmd.port());
            stream = socket.getOutputStream();
            stream.write(cmd.command().getBytes());
            stream.flush();

            final InputStream is = socket.getInputStream();
            int i;
            while ((i = is.read()) != -1) {
                out.append(Character.toChars(i));
            }

            return out.toString();
        } catch (final IOException e) {
            return e.getMessage();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public void run() {
        System.out.println(sendCommand());
    }
}
