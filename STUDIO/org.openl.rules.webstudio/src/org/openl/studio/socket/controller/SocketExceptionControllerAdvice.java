package org.openl.studio.socket.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class SocketExceptionControllerAdvice {

    @MessageExceptionHandler
    @SendToUser("/queue/errors")  // client must subscribe to /user/queue/errors
    public String handleAny(Throwable ex) {
        return ex.getMessage();
    }

}
