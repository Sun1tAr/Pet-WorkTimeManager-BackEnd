package ru.proj3ct5.settings;

import lombok.extern.slf4j.Slf4j;
import ru.proj3ct5.network.Message;
import ru.proj3ct5.network.Subscriber;

import java.util.LinkedList;

@Slf4j
public abstract class Handler {

    protected Subscriber subscriber;
    private Thread t;
    private boolean running;
    protected String command;

    
    public Handler(Subscriber subscriber) {
        this.subscriber = subscriber;
        running = true;
    }

    public void process() {

        t = new Thread(() -> {
            log.info("{} was start processing", this.getClass().getSimpleName());
            LinkedList<String> messagesList;
            int oldMsgCount = 0;

            if (!subscriber.isRunning()) {
                subscriber.start();
            }

            while (running) {
                Message deserialized;
                messagesList = subscriber.getMessages();
                log.debug("Received list of messages: {}", messagesList);
                if (messagesList.size() > oldMsgCount) {

                    for (int i = oldMsgCount; i < messagesList.size(); i++) {
                        String message = messagesList.get(i);
                        log.debug("New message received: {}", message);
                        deserialized = Message.deserialize(message);
                        command = deserialized.getData();
                        log.debug("Deserialized command: {}", command);
                        executeCommand();
                        oldMsgCount++;
                    }
                    log.debug("Messages list updates to {} size", oldMsgCount);
                } else {
                    log.debug("No new messages received");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Thread interrupted: {}", e.getMessage());
                    }
                }
            }
        });
        t.start();
    }

    public void close() {
        subscriber.close();
        running = false;
        log.info("{}'s thread was stopped", this.getClass().getSimpleName());

    }

    protected abstract void executeCommand();
    
    
    
}
