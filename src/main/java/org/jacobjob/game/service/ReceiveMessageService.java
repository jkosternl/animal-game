package org.jacobjob.game.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@AllArgsConstructor
@Controller
public class ReceiveMessageService {

    private GameBoardService gameBoardService;

    @MessageMapping("/controls")
    public void handle(final String controlCode) {
//        log.info("Received: {}", controlCode);
        gameBoardService.processKeys(controlCode);
    }
}
