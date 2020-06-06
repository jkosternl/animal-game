package org.jacobjob.game.service;

import lombok.extern.slf4j.Slf4j;
import org.jacobjob.game.model.Animal;
import org.jacobjob.game.model.AnimalDTO;
import org.jacobjob.game.model.AnimalType;
import org.jacobjob.game.model.GameState;
import org.jacobjob.game.model.WebSocketTopic;
import org.jacobjob.game.repository.GameStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class WebSocketService {

    private final SimpMessagingTemplate template;
    private final GameStateRepository gameState;

    @Autowired
    public WebSocketService(final SimpMessagingTemplate template, final GameStateRepository gameStateRepository) {
        this.template = template;
        gameState = gameStateRepository;
    }

    @Async
    public void updateAnimal(final Animal animal) {
        if (isOutsideViewPort(animal)) {
            return;
        }
        WebSocketTopic topic = WebSocketTopic.SNAKE;
        if (animal.getAnimalType() == AnimalType.POLICE) {
            topic = WebSocketTopic.POLICE;
        }
        // Reduce data sending to browser
        final AnimalDTO dto = new AnimalDTO(animal);

        // Correct X and Y to viewport coordinates
        final GameState state = gameState.getState();
        dto.setX(animal.getX() - state.viewPortX);
        dto.setY(animal.getY() - state.viewPortY);
        sendMessage(topic, dto);
    }

    private boolean isOutsideViewPort(final Animal animal) {
        final GameState state = gameState.getState();
        return animal.getX() > state.viewPortX + GameBoardService.VIEW_PORT_WIDTH
            || animal.getY() > state.viewPortY + GameBoardService.VIEW_PORT_HEIGHT
            || animal.getX() < state.viewPortX
            || animal.getY() < state.viewPortY;
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
