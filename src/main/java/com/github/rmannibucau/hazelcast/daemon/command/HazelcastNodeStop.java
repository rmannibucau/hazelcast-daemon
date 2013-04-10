package com.github.rmannibucau.hazelcast.daemon.command;

import io.airlift.command.Command;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

@Command(name = "stop", description = "Stop a hazelcast node")
public class HazelcastNodeStop extends RemoteCommand implements Runnable {
    @Override
    public void run() {
        OutputStream stream = null;
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            stream = socket.getOutputStream();
            String shutdown = command;
            for (int i = 0; i < shutdown.length(); i++) {
                stream.write(shutdown.charAt(i));
            }
            stream.flush();
        } catch (final IOException e) {
            System.err.println(e.getMessage());
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

        System.out.println("Stop command sent");
    }
}
