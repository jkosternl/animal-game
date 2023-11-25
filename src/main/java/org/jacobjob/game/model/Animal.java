package org.jacobjob.game.model;

import java.util.Random;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class Animal {
  private static final int FROM_EDGE = 50;
  private static final Random random = new Random();

  // Orientation
  public static final double NORTH = 1.5d * Math.PI;
  public static final double EAST = 0d;
  public static final double SOUTH = Math.PI / 2d;
  public static final double WEST = Math.PI;

  private int x = 0;
  private int y = 0;

  private final int number;
  private final int maxX;
  private final int maxY;

  private double orientation = 0;
  private int quadrant = 1;
  private int corrections;
  private boolean avoiding = false; // avoiding edges
  private final AnimalType animalType;

  private double speed = 5;
  private int size = 8;
  private int score;
  private boolean alive;
  private int stepsAlive;

  public Animal(int number, final AnimalType type, final int maxX, final int maxY) {
    this.maxX = maxX;
    this.maxY = maxY;
    createAnimal();
    this.number = number;
    animalType = type;
    if (isPolice()) speed -= 1;
    if (isPlayer()) speed -= 2;
  }

  public void createAnimal() {
    x = random.nextInt(maxX - FROM_EDGE) + 15;
    y = random.nextInt(maxY - FROM_EDGE) + 15;
    orientation = Math.random() * 2d * Math.PI;
    changeOrientation(0d); // fix quadrant
    correctStartPosition();
    alive = true;
  }

  private void correctStartPosition() {
    if (x < FROM_EDGE && (quadrant == 3 || quadrant == 2)) {
      x += FROM_EDGE;
    }
    if (y < FROM_EDGE && (quadrant == 3 || quadrant == 4)) {
      y += FROM_EDGE;
    }
    if (x + FROM_EDGE > maxX && (quadrant == 1 || quadrant == 4)) {
      x -= FROM_EDGE;
    }
    if (y + FROM_EDGE > maxY && (quadrant == 1 || quadrant == 2)) {
      y -= FROM_EDGE;
    }
  }

  public void step() {
    if (!alive || isGold()) return;
    stepsAlive += 1;
    x += (int) (Math.cos(orientation) * speed);
    y += (int) (Math.sin(orientation) * speed);

    // Add random rotation sometimes
    if (stepsAlive % 12 == 0 && !avoiding && !isPlayer()) {
      changeOrientation(Math.random() / 2d);
    }
  }

  public void safeGuardEdges() {
    if (!alive || isGold() || isPlayer()) return;
    final double stepSize = (Math.PI / 10d);

    if (x + FROM_EDGE > maxX) {
      if (quadrant == 1) {
        changeOrientation(stepSize); // rotate clockwise
        avoiding = true;
        return;
      }
      if (quadrant == 4) {
        changeOrientation(-stepSize); // rotate anti clockwise
        avoiding = true;
        return;
      }
    }

    if (y + FROM_EDGE > maxY) {
      if (quadrant == 1) {
        changeOrientation(-stepSize); // rotate anti clockwise
        avoiding = true;
        return;
      }
      if (quadrant == 2) {
        changeOrientation(stepSize); // rotate clockwise
        avoiding = true;
        return;
      }
    }

    if (x < FROM_EDGE) {
      if (quadrant == 3) {
        changeOrientation(stepSize); // rotate clockwise
        avoiding = true;
        return;
      }
      if (quadrant == 2) {
        changeOrientation(-stepSize); // rotate anti clockwise
        avoiding = true;
        return;
      }
    }
    if (y < FROM_EDGE) {
      if (quadrant == 3) {
        changeOrientation(-stepSize); // rotate clockwise
        avoiding = true;
        return;
      }
      if (quadrant == 4) {
        changeOrientation(stepSize); // rotate anti clockwise
        avoiding = true;
        return;
      }
    }
    avoiding = false;
  }

  public void changeOrientation(final double delta) {
    orientation += delta;
    corrections += 1;
    // fix bigger than 360 degrees or lower than 0 degrees
    if (orientation >= (Math.PI * 2d)) orientation -= (Math.PI * 2d);
    if (orientation < 0) orientation += (Math.PI * 2d);

    if (orientation >= EAST && orientation < SOUTH) quadrant = 1;
    else if (orientation >= SOUTH && orientation < WEST) quadrant = 2;
    else if (orientation >= WEST && orientation < NORTH) quadrant = 3;
    else quadrant = 4;
  }

  public void kill() {
    alive = false;
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x + size > maxX) x = maxX - size;
    if (y + size > maxY) y = maxY - size;
  }

  public void scored() {
    score++;
  }

  public void log() {
    if (corrections == 0) return;
    log.info(
        "Snake {} is at position[{}, {}], orientation {}, quadrant {}, corrections: {}, steps: {}",
        number,
        x,
        y,
        orientation,
        quadrant,
        corrections,
        stepsAlive);
  }

  public boolean isPolice() {
    return animalType == AnimalType.POLICE;
  }

  public boolean isGold() {
    return animalType == AnimalType.GOLD;
  }

  public boolean isSnake() {
    return animalType == AnimalType.SNAKE || animalType == AnimalType.PLAYER;
  }

  public boolean isPlayer() {
    return animalType == AnimalType.PLAYER;
  }

  public void changeSize(final int delta) {
    size += delta;
  }

  public void changeSpeed(final int delta) {
    speed += delta;
  }
}
