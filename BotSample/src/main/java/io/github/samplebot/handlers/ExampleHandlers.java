package io.github.samplebot.handlers;

import io.github.yurasulima.annotations.*;
import io.github.yurasulima.types.Message;


public class ExampleHandlers {

    @MessageHandler
    @Command(commands = {"ping"}, prefix = "!/.")
    public void ping(Message message) {
        message.answer("Pong!");
    }


    @MessageHandler
    @Command(commands = {"start"}, prefix = "!/.")
    public void start(Message message) {
        message.answer("Hello!");
    }


    @ChannelPostHandler
    @Command(commands = {"start"}, prefix = "!/.")
    public void startPost(Message message) {
        message.answer("Hello!");
    }


    @MessageHandler(textStartsWith = {"/echo "})
    public void echoText(Message message) {
        message.answer(message.requireText().substring("/echo ".length()));
    }


    @MessageHandler()
    @HasPhoto
    public void echo(Message message) {
        System.out.println( message.photo().toString());
    }


    @MessageHandler()
    @HasPhoto
    public void photoHandler(Message message) {
        System.out.println(message.photo().toString());
    }

}
