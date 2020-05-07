package org.jacobjob.game.service;

import lombok.extern.slf4j.Slf4j;
import org.jacobjob.game.model.Animal;
import org.jacobjob.game.model.AnimalDTO;
import org.jacobjob.game.model.AnimalType;
import org.jacobjob.game.model.WebSocketTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class WebSocketService {

    private final SimpMessagingTemplate template;

    @Autowired
    public WebSocketService(final SimpMessagingTemplate template) {
        this.template = template;
    }

    @Async
    public void updateAnimal(final Animal animal) {
        WebSocketTopic topic = WebSocketTopic.SNAKE;
        if (animal.getAnimalType() == AnimalType.POLICE) {
            topic = WebSocketTopic.POLICE;
        }
        // Reduce data sending to browser
        final AnimalDTO dto = new AnimalDTO(animal);
        sendMessage(topic, dto);
    }

    @Async
    public void sendNews(final String message) {
        sendMessage(WebSocketTopic.NEWS, message);
    }

    @Async
    public void updateScore(final Animal animal) {
        sendMessage(WebSocketTopic.SCORE, animal.getScore());
    }

    private void sendMessage(final WebSocketTopic topic, final Object message) {
        try {
            template.convertAndSend(topic.toString(), message);
        } catch (final Exception e) {
            // This problem seems to occur if a client's token is no longer valid
            log.error("Unable to send socket message {} to {}: {}", message.toString(), topic, e.getMessage());
            log.debug("Trace information for socket message", e);
        }
    }


}
