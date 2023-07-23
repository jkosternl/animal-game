package org.jacobjob.game.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GameState {
  public int deadAnimalCounter = 0;
  public int highestAnimalNumber = 1;
  public final List<Animal> animals = new ArrayList<>();
  public Animal player = null;
  public boolean resetBoard = true;
  public int gameState = 0;

  public int viewPortX = 0;
  public int viewPortY = 0;
  public boolean pause = false;
}
