package dev.gamecrash.verteiler;

import dev.gamecrash.verteiler.logging.Logger;

public class Main {
    private static Logger logger;

    public static void main(String[] args) {
        logger = Logger.getInstance();
        logger.info("yay");
        logger.warn("hm");
        logger.error("bad");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            logger.flushQueue();
        }));
    }
}
