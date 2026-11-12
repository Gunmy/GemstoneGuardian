package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

//The network under the 1v1 (see duel), on the local network only.
//- Challenges are found by UDP broadcast: whoever creates one says so on DISCOVERY_PORT every second (host), and
//  everyone on the 1v1 page listens (finder).
//- The fight itself is one TCP connection (this class): lines of tab separated fields, the first the kind of message.
//  A thread reads them into a queue the game takes them from each frame (poll), so the network never blocks a frame
public class duelLink {

    public static final int DISCOVERY_PORT = 47821;
    private static final String TAG = "GGDUEL1";

    private Socket socket;
    private PrintWriter out;
    private ConcurrentLinkedQueue<String[]> in = new ConcurrentLinkedQueue<String[]>();
    private volatile boolean closed = false;

    private duelLink (Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) in.add(line.split("\t", -1));
            } catch (IOException e) {
                //Closed, or the other side went away
            }
            closed = true;
        }, "duel-read");
        t.setDaemon(true);
        t.start();
    }

    //Joins a challenge (from finder). Null if it can not be reached
    public static duelLink connect (InetAddress address, int port) {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(address, port), 3000);
            return new duelLink(s);
        } catch (IOException e) {
            System.out.println("Fant ikke utfordringen: " + e);
            return null;
        }
    }

    public synchronized void send (String... fields) {
        if (closed) return;
        out.println(String.join("\t", fields));
        if (out.checkError()) closed = true;
    }

    //The next message, or null if none has come
    public String[] poll () {return in.poll();}

    //Closed by either side (messages that came before can still be polled)
    public boolean isClosed () {return closed && in.isEmpty();}

    public void close () {
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            //Already gone
        }
    }

    //An open challenge: says it is there over UDP every second (the player's name and their build, duelBuild.encode,
    //so the list can show its rocks) and waits for one player to join
    public static class host {
        private ServerSocket server;
        private DatagramSocket announcer;
        private volatile duelLink joined;
        private volatile boolean stopped = false;

        public host (String player, String build) throws IOException {
            server = new ServerSocket(0);
            announcer = new DatagramSocket();
            announcer.setBroadcast(true);
            String message = TAG + "\t" + duelBuild.enc(player) + "\t" + server.getLocalPort() + "\t" + duelBuild.enc(build);
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

            Thread announce = new Thread(() -> {
                while (!stopped && joined == null) {
                    for (InetAddress to : broadcastAddresses()) {
                        try {
                            announcer.send(new DatagramPacket(bytes, bytes.length, to, DISCOVERY_PORT));
                        } catch (IOException e) {
                            //That network is not up
                        }
                    }
                    try {Thread.sleep(1000);} catch (InterruptedException e) {return;}
                }
            }, "duel-announce");
            announce.setDaemon(true);
            announce.start();

            Thread accept = new Thread(() -> {
                try {
                    Socket s = server.accept();
                    joined = new duelLink(s);
                } catch (IOException e) {
                    //Stopped
                }
            }, "duel-accept");
            accept.setDaemon(true);
            accept.start();
        }

        //The connection once someone has joined, else null
        public duelLink getJoined () {return this.joined;}

        //Stops saying it is there and waiting (the connection, if any, is not closed)
        public void stop () {
            stopped = true;
            announcer.close();
            try {
                server.close();
            } catch (IOException e) {
                //Already closed
            }
        }
    }

    //Every network's broadcast address, and this machine itself, so two games on one machine find each other too
    private static List<InetAddress> broadcastAddresses () {
        List<InetAddress> list = new ArrayList<InetAddress>();
        try {
            list.add(InetAddress.getByName("255.255.255.255"));
            list.add(InetAddress.getLoopbackAddress());
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress a : ni.getInterfaceAddresses()) {
                    if (a.getBroadcast() != null && !list.contains(a.getBroadcast())) list.add(a.getBroadcast());
                }
            }
        } catch (IOException e) {
            //Whatever was found so far
        }
        return list;
    }

    //A challenge seen on the network
    public static class challenge {
        public final InetAddress address;
        public final int port;
        public final String player;
        //The challenger's build (duelBuild.encode)
        public final String build;
        private long seen;

        challenge (InetAddress address, int port, String player, String build) {
            this.address = address;
            this.port = port;
            this.player = player;
            this.build = build;
        }
    }

    //Listens for challenges while the 1v1 page is open. Ones not heard from for a few seconds are gone
    public static class finder {
        private DatagramSocket socket;
        private Map<String, challenge> found = new LinkedHashMap<String, challenge>();
        private volatile boolean stopped = false;

        public finder () {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.setSoTimeout(500);
                socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            } catch (IOException e) {
                System.out.println("Kan ikke lytte etter utfordringer: " + e);
                socket = null;
                return;
            }
            Thread t = new Thread(() -> {
                byte[] buffer = new byte[8192];
                while (!stopped) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String[] f = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).split("\t", -1);
                        if (f.length < 4 || !f[0].equals(TAG)) continue;
                        int port = Integer.parseInt(f[2]);
                        //The loopback copy and the network copy of one challenge on this machine are the same one
                        InetAddress from = packet.getAddress();
                        String key = duelBuild.dec(f[1]) + "@" + port;
                        synchronized (found) {
                            challenge c = found.get(key);
                            if (c == null || from.isLoopbackAddress()) {
                                c = new challenge(from, port, duelBuild.dec(f[1]), duelBuild.dec(f[3]));
                                found.put(key, c);
                            }
                            c.seen = System.currentTimeMillis();
                        }
                    } catch (SocketTimeoutException e) {
                        //Look again
                    } catch (IOException | NumberFormatException e) {
                        if (stopped) return;
                    }
                }
            }, "duel-find");
            t.setDaemon(true);
            t.start();
        }

        public boolean isListening () {return socket != null;}

        public List<challenge> list () {
            long now = System.currentTimeMillis();
            synchronized (found) {
                found.values().removeIf(c -> now - c.seen > 3500);
                return new ArrayList<challenge>(found.values());
            }
        }

        public void stop () {
            stopped = true;
            if (socket != null) socket.close();
        }
    }
}
