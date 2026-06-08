package io.github.samplebot;


import io.github.samplebot.handlers.ExampleHandlers;
import io.github.yurasulima.Bot;
import io.github.yurasulima.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        new Main().run();
    }


    private void startup(Bot bot) {
        log.info("Started @{}", bot.username());
    }

    private void shutdown(Bot bot) {
        log.info("Stopped @{}", bot.username());
    }

    private void run() {
        var bot = new Bot("6819128935:AAE6mRCHhs84hUeg66BobCBt2xGJuwMzNgw");
        var dp = new Dispatcher();

        dp.onStartup(this::startup);
        dp.onShutdown(this::shutdown);
        dp.onError((ctx, ex) -> ex.printStackTrace());
        dp.register(new ExampleHandlers());
        dp.startPolling(bot);
    }
}
