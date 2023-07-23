package org.jacobjob.game.model;

public enum AnimalType {
  SNAKE("Snake"),
  GOLD("Gold"),
  PLAYER("Player"),
  POLICE("Police");

  private final String name;

  AnimalType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
