package ru.proj3ct5.network;

import com.sun.jna.NativeLibrary;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import ru.proj3ct5.settings.Configurator;

import java.util.*;

@Slf4j
public class Subscriber {

    public Subscriber(String THIS_SERVICE_NAME) {
        this.THIS_SERVICE_NAME = THIS_SERVICE_NAME;
    }

    static {
        NativeLibrary.addSearchPath("wpcap", "C:\\Windows\\System32\\Npcap");
    }

    private PcapHandle pcapHandle;
    private final LinkedList<String> messages = new LinkedList<>();
    private int port;
    private Thread t;
    private boolean run;
    private String THIS_SERVICE_NAME;

    

    public void start() {
        run = true;
        Configurator config = new Configurator();
        port = config.getPort();

        List<PcapNetworkInterface> allDevs = null;
        try {
            allDevs = Pcaps.findAllDevs();
        } catch (PcapNativeException e) {
            log.error("Failed to find all devs: {}", e.getMessage());
        }
        PcapNetworkInterface pcapNetworkInterface = allDevs.stream()
                .filter(e -> e.getName().equals("\\Device\\NPF_Loopback"))
                .findAny()
                .orElseThrow();

        try {
            pcapHandle = pcapNetworkInterface.openLive(1500, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 50);
        } catch (PcapNativeException e) {
            log.error("Failed to open pcap handle: {}", e.getMessage());
        }

        try {
            pcapHandle.setFilter("ip proto \\udp && dst port " + port, BpfProgram.BpfCompileMode.NONOPTIMIZE);
        } catch (PcapNativeException e) {
            throw new RuntimeException(e);
        } catch (NotOpenException e) {
            log.error("Failed to open pcap handle: {}", e.getMessage());
        }

        t = new Thread(() -> {
            try {
                pcapHandle.loop(-1, (PacketListener) packet -> {
                    byte[] rawData = packet.getRawData();
                    String newMessage = new String(rawData, 32, rawData.length - 32);
                    if (Message.isMyMessage(newMessage, THIS_SERVICE_NAME)) {
                        messages.add(newMessage);
                        log.debug("New message received: {}", newMessage);
                    } else {
                        log.debug("Received message will be resend: {}", newMessage);
                        Message.resendMessage(newMessage);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to handle packet: {}", e.getMessage());
            }
        });
        t.start();
    }

    public synchronized void close() {
        run = false;
        try {
            pcapHandle.breakLoop();
        } catch (NotOpenException e) {
            log.error("Failed to break loop: {} OF {}", e.getMessage(), this.getClass().getSimpleName());
        }
        t.interrupt();
        pcapHandle.close();
        log.info("Subscriber's thread was stopped");
        log.info("Stopping subscriber");
    }

    public synchronized LinkedList<String> getMessages() {
        LinkedList<String> messages = new LinkedList<>();
        messages.addAll(this.messages);
        log.debug("Messages was got: {}", messages);
        return messages;
    }

    public boolean isRunning() {
        return run;
    }

}
