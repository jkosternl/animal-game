package org.jacobjob.game.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimalDTO {
  private int x, y;
  private final int size, number;
  private final AnimalType animalType;
  private final boolean alive;

  public AnimalDTO(final Animal animal) {
    x = animal.getX();
    y = animal.getY();
    size = animal.getSize();
    number = animal.getNumber();
    animalType = animal.getAnimalType();
    alive = animal.isAlive();
  }
}
