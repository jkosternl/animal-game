package org.jacobjob.game.model;

public enum WebSocketTopic {
  SNAKE("snakes"),
  NEWS("news"),
  SCORE("score"),
  POLICE("police");

  private final String topic;

  WebSocketTopic(String name) {
    topic = name;
  }

  @Override
  public String toString() {
    return "/topic/" + topic;
  }
}
