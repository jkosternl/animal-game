package org.jacobjob.game.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GameState {
    public int deadAnimalCounter = 0;
    public int highestAnimalNumber = 1;
    public final List<Animal> animals = new ArrayList<>();
    public Animal player = null;
    public boolean resetBoard = true;
    public int gameState = 0;
}
