package org.jacobjob.game.model;

import lombok.Getter;

@Getter
public class AnimalDTO {
    private final int x, y, size, number;
    private final AnimalType animalType;
    private final boolean alive;

    public AnimalDTO(final Animal animal){
        x = animal.getX();
        y = animal.getY();
        size = animal.getSize();
        number = animal.getNumber();
        animalType = animal.getAnimalType();
        alive = animal.isAlive();
    }
}
