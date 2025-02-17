package ru.proj3ct5;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

import org.apache.log4j.PropertyConfigurator;
import ru.proj3ct5.network.Subscriber;
import ru.proj3ct5.service.GUI;
import ru.proj3ct5.settings.Handler;
import ru.proj3ct5.service.timeTracker.TimeTrackerHandler;


@Log4j
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        PropertyConfigurator.configure(
                "C:\\0_DATA\\CodeProjects\\Java\\MyManager\\src\\main\\resources\\log4j.properties");

        Subscriber subscriber = new Subscriber();

        Handler h = new TimeTrackerHandler(subscriber);
        h.process();

        GUI gui = new GUI();
        gui.startRemoteGUI();


    }




}