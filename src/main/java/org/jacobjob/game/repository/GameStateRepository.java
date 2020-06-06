package org.jacobjob.game.repository;

import lombok.Getter;
import org.jacobjob.game.model.GameState;
import org.springframework.stereotype.Repository;

@Getter
@Repository
public class GameStateRepository {

    private GameState state = new GameState();

}
