package org.jacobjob.game.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jacobjob.game.model.Animal;
import org.jacobjob.game.model.AnimalType;
import org.jacobjob.game.model.GameState;
import org.jacobjob.game.repository.GameStateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class GameBoardService {

    private static final int MAX_X = 1200;
    private static final int MAX_Y = 700;
    private static final int AMOUNT_OF_SNAKES = 20;
    private static final long REFRESH_DELAY = 100;

    private final WebSocketService webSocketService;
    private final GameStateRepository gameState;

    @Scheduled(initialDelay = 3_000, fixedRate = 5_000) // Runs every 10 seconds; wait before starting the first time
    public void start() {
        GameState state = gameState.getState();
        log.info("Start with run loop");
        if (state.animals.isEmpty() || state.resetBoard) {
            log.info("Reset player board");
            webSocketService.sendNews("reset");
            state.resetBoard = false;
            state.deadAnimalCounter = 0;
            state.highestAnimalNumber = 1;
            state.animals.clear();
            addAnimals(state, AnimalType.PLAYER, 1);
            addAnimals(state, AnimalType.SNAKE, AMOUNT_OF_SNAKES);
            addAnimals(state, AnimalType.POLICE, AMOUNT_OF_SNAKES / 4);
            addAnimals(state, AnimalType.GOLD, AMOUNT_OF_SNAKES / 3);
            webSocketService.updateScore(state.player);
        }
        try {
            for (int i = 0; i < 48; i++) {
                loop(state);
                if (state.animals.isEmpty() || state.resetBoard) {
                    log.info("Finished after {} steps", i);
                    break;
                }
            }
        } catch (final InterruptedException e) {
            log.warn("Interrupted by user.");
            Thread.currentThread().interrupt();
        }
        log.info("Amount of animals survived: {}", state.animals.size());
    }

    private void loop(final GameState state) throws InterruptedException {
        // move animals 1 step
        state.animals.forEach(Animal::step);
        state.animals.forEach(Animal::safeGuardEdges);

        // check for illegal positions: change status if so.
        state.animals.forEach(animal -> verifyEdges(state, animal));

        // Send new coordinates of animals to front-end
        state.animals.forEach(webSocketService::updateAnimal);

        // Check for collisions and avoid them or find gold
        state.animals.forEach(animal -> findCloseAnimals(state, animal));

        // Clean up the board
        handleDeadAnimals(state);

        Thread.sleep(REFRESH_DELAY);
    }

    private void handleDeadAnimals(final GameState state) {
        List<Animal> deadAnimals = new ArrayList<>();
        state.animals.stream()
            .filter(animal -> !animal.isAlive() && !animal.isGold())
            .forEach(deadAnimals::add);

        if (!deadAnimals.isEmpty()) {
            state.deadAnimalCounter += deadAnimals.size();
            state.animals.removeAll(deadAnimals);
            if (state.deadAnimalCounter == 4) {
                addAnimals(state, AnimalType.SNAKE, 2);
                state.deadAnimalCounter = 0;
            }
        }
    }

    private void findCloseAnimals(final GameState state, final Animal animal) {
        if (!animal.isAlive()) return;
        final int detectDistance = 40;
        state.animals.stream()
            .filter(other -> (
                other.isAlive() &&
                    animal.getNumber() != other.getNumber() &&
                    animal.getX() + detectDistance >= other.getX() &&
                    animal.getX() - detectDistance <= other.getX() &&
                    animal.getY() + detectDistance >= other.getY() &&
                    animal.getY() - detectDistance <= other.getY()
            ))
            .forEach(other -> {
                handleCollision(state, animal, other);
                avoidCollision(animal, other);
            });
    }

    private void handleCollision(final GameState state, final Animal animal, final Animal other) {
        if (Math.abs(animal.getX() - other.getX()) < animal.getSize() &&
            Math.abs(animal.getY() - other.getY()) < animal.getSize()) {

            if (other.isGold()) {
                killAnimal(state, other, animal);
                return;
            } else if (animal.isGold()) {
                killAnimal(state, animal, other);
                return;
            }
            if (!animal.isPolice() && other.isPolice() ||
                animal.isPolice() && other.isPolice() ||
                !animal.isPolice() && !other.isPolice()) {
                killAnimal(state, animal, other);
                return;
            }
            if (animal.isPolice() && !other.isPolice()) {
                killAnimal(state, other, animal);
            }
        }
    }

    private void avoidCollision(final Animal animal, final Animal other) {
        if (animal.isPlayer() ||
            animal.isAvoiding() && !other.isPlayer()) return;
        double correction = Math.PI / 6d;

        final int nextDistance1 = getNextDistance(animal, other, correction);
        final int nextDistance2 = getNextDistance(animal, other, 0);
        final int nextDistance3 = getNextDistance(animal, other, -correction);

        if (animal.isPolice() && !other.isPolice() ||
            animal.isSnake() && other.isGold()) {
            if (nextDistance2 <= nextDistance1 && nextDistance2 <= nextDistance3) correction = 0;
            if (nextDistance3 <= nextDistance1 && nextDistance3 <= nextDistance2) correction *= -1;
            animal.changeOrientation(correction);
            return;
        }

        if (nextDistance2 >= nextDistance1 && nextDistance2 >= nextDistance3) correction = 0;
        if (nextDistance3 >= nextDistance1 && nextDistance3 >= nextDistance2) correction *= -1;
        animal.changeOrientation(correction);
    }

    private int getNextDistance(final Animal animal, final Animal other, final double correction) {
        final int nextX = animal.getX() + (int) (Math.cos(animal.getOrientation() + correction) * animal.getSpeed());
        final int nextY = animal.getY() + (int) (Math.sin(animal.getOrientation() + correction) * animal.getSpeed());
        return Math.abs(nextX - other.getX()) + Math.abs(nextY - other.getY());
    }

    private void verifyEdges(final GameState state, final Animal animal) {
        if (!animal.isAlive() || animal.isGold()) return;
        if (animal.getX() < 0 || animal.getY() < 0) killAnimal(state, animal, null);
        if (animal.getX() + animal.getSize() > MAX_X || animal.getY() + animal.getSize() > MAX_Y) killAnimal(state, animal, null);
        if (!animal.isAlive()) {
            webSocketService.sendNews(animal.getAnimalType().toString() + " " + animal.getNumber() + " was killed by the edge");
            if (animal.isPlayer())
                webSocketService.sendNews("You scored: " + animal.getScore() + " points!");
        }
    }

    private void killAnimal(final GameState state, final Animal animal, final Animal killer) {
        if (animal.isGold()) {
            animal.createAnimal(); // Refresh Gold to different location; do not kill
            if (killer != null) {
                killer.scored();
                if (killer.isPlayer())
                    webSocketService.updateScore(killer);
            }
            return;
        }
        animal.kill();
        if (killer != null) {
            webSocketService.sendNews(animal.getAnimalType() + " " + animal.getNumber() + " was killed by " + killer.getAnimalType());
            killer.changeSize(2); // Reward killing
            webSocketService.updateAnimal(killer);
        }
        webSocketService.updateAnimal(animal);
        if (animal.isPlayer()) {
            state.resetBoard = true;
            webSocketService.sendNews("You scored: " + animal.getScore() + " points!");
        }
    }

    private void addAnimals(final GameState state, final AnimalType type, final int amount) {
        if (amount == 0) return;
        for (int i = 1; i <= amount; i++) {
            state.animals.add(new Animal(state.highestAnimalNumber++, type, MAX_X, MAX_Y));
        }
        if (type != AnimalType.GOLD) {
            log.info("Creating {} {}", amount, type);
            webSocketService.sendNews("Added " + amount + " new " + type);
        }
        if (type.equals(AnimalType.PLAYER)) {
            state.player = state.animals.get(state.animals.size() - 1);
        }
    }

    public void processKeys(final String controlCode) {
        GameState state = gameState.getState();
        double correction = Math.PI / 6d;
        if ("left".equals(controlCode)) {
            state.player.changeOrientation(-correction);
        } else if ("right".equals(controlCode)) {
            state.player.changeOrientation(correction);
        } else if ("up".equals(controlCode)) {
            state.player.changeSpeed(2);
        } else if ("down".equals(controlCode)) {
            state.player.changeSpeed(-2);
        } else if ("reset".equals(controlCode)) {
            state.resetBoard = true;
        }
    }
}
