package org.jacobjob.game.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jacobjob.game.model.Animal;
import org.jacobjob.game.model.AnimalType;
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

    private static int deadAnimalCounter = 0;
    private static int highestAnimalNumber = 1;

    private final WebSocketService webSocketService;
    private static final List<Animal> animals = new ArrayList<>();
    private static Animal player = null;
    private static boolean resetBoard = true;

    @Scheduled(initialDelay = 3_000, fixedRate = 5_000) // Runs every 10 seconds; wait before starting the first time
    public void start() {
        log.info("Start with run loop");
        if (animals.isEmpty() || resetBoard) {
            log.info("Reset player board");
            webSocketService.sendNews("reset");
            resetBoard = false;
            deadAnimalCounter = 0;
            highestAnimalNumber = 1;
            animals.clear();
            addAnimals(AnimalType.PLAYER, 1);
            addAnimals(AnimalType.SNAKE, AMOUNT_OF_SNAKES);
            addAnimals(AnimalType.POLICE, AMOUNT_OF_SNAKES / 4);
            addAnimals(AnimalType.GOLD, AMOUNT_OF_SNAKES / 3);
            webSocketService.updateScore(player);
        }
        try {
            for (int i = 0; i < 48; i++) {
                loop();
                if (animals.isEmpty() || resetBoard) {
                    log.info("Finished after {} steps", i);
                    break;
                }
            }
        } catch (final InterruptedException e) {
            log.warn("Interrupted by user.");
            Thread.currentThread().interrupt();
        }
        log.info("Amount of animals survived: {}", animals.size());
    }

    private void loop() throws InterruptedException {
        // move animals 1 step
        animals.forEach(Animal::step);
        animals.forEach(Animal::safeGuardEdges);

        // check for illegal positions: change status if so.
        animals.forEach(this::verifyEdges);

        // Send new coordinates of animals to front-end
        animals.forEach(webSocketService::updateAnimal);

        // Check for collisions and avoid them or find gold
        animals.forEach(this::findCloseAnimals);

        // Clean up the board
        handleDeadAnimals();

        Thread.sleep(REFRESH_DELAY);
    }

    private void handleDeadAnimals() {
        List<Animal> deadAnimals = new ArrayList<>();
        animals.stream()
            .filter(animal -> !animal.isAlive() && !animal.isGold())
            .forEach(deadAnimals::add);

        if (!deadAnimals.isEmpty()) {
            deadAnimalCounter += deadAnimals.size();
            animals.removeAll(deadAnimals);
            if (deadAnimalCounter == 4) {
                addAnimals(AnimalType.SNAKE, 2);
                deadAnimalCounter = 0;
            }
        }
    }

    private void findCloseAnimals(final Animal animal) {
        if (!animal.isAlive()) return;
        final int detectDistance = 40;
        animals.stream()
            .filter(other -> (
                other.isAlive() &&
                    animal.getNumber() != other.getNumber() &&
                    animal.getX() + detectDistance >= other.getX() &&
                    animal.getX() - detectDistance <= other.getX() &&
                    animal.getY() + detectDistance >= other.getY() &&
                    animal.getY() - detectDistance <= other.getY()
            ))
            .forEach(other -> {
                handleCollision(animal, other);
                avoidCollision(animal, other);
            });
    }

    private void handleCollision(final Animal animal, final Animal other) {
        if (Math.abs(animal.getX() - other.getX()) < animal.getSize() &&
            Math.abs(animal.getY() - other.getY()) < animal.getSize()) {

            if (other.isGold()) {
                killAnimal(other, animal);
                return;
            } else if (animal.isGold()) {
                killAnimal(animal, other);
                return;
            }
            if (!animal.isPolice() && other.isPolice() ||
                animal.isPolice() && other.isPolice() ||
                !animal.isPolice() && !other.isPolice()) {
                killAnimal(animal, other);
                return;
            }
            if (animal.isPolice() && !other.isPolice()) {
                killAnimal(other, animal);
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

    private void verifyEdges(final Animal animal) {
        if (!animal.isAlive() || animal.isGold()) return;
        if (animal.getX() < 0 || animal.getY() < 0) killAnimal(animal, null);
        if (animal.getX() + animal.getSize() > MAX_X || animal.getY() + animal.getSize() > MAX_Y) killAnimal(animal, null);
        if (!animal.isAlive()) {
            webSocketService.sendNews(animal.getAnimalType().toString() + " " + animal.getNumber() + " was killed by the edge");
            if (animal.isPlayer())
                webSocketService.sendNews("You scored: " + animal.getScore() + " points!");
        }
    }

    private void killAnimal(final Animal animal, final Animal killer) {
        if (animal.isGold()) {
            animal.createAnimal(); // Refresh Gold to different location; do not kill
            killer.scored();
            if (killer.isPlayer())
                webSocketService.updateScore(killer);
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
            resetBoard = true;
            webSocketService.sendNews("You scored: " + animal.getScore() + " points!");
        }
    }

    private void addAnimals(final AnimalType type, final int amount) {
        if (amount == 0) return;
        for (int i = 1; i <= amount; i++) {
            animals.add(new Animal(highestAnimalNumber++, type, MAX_X, MAX_Y));
        }
        if (type != AnimalType.GOLD) {
            log.info("Creating {} {}", amount, type);
            webSocketService.sendNews("Added " + amount + " new " + type);
        }
        if (type.equals(AnimalType.PLAYER)) {
            player = animals.get(animals.size() - 1);
        }
    }

    public void processKeys(final String controlCode) {
        double correction = Math.PI / 6d;
        if ("left".equals(controlCode)) {
            player.changeOrientation(-correction);
        } else if ("right".equals(controlCode)) {
            player.changeOrientation(correction);
        } else if ("up".equals(controlCode)) {
            player.changeSpeed(2);
        } else if ("down".equals(controlCode)) {
            player.changeSpeed(-2);
        } else if ("reset".equals(controlCode)) {
            resetBoard = true;
        }
    }
}
