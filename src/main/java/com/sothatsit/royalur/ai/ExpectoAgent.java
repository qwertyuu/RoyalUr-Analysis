package com.sothatsit.royalur.ai;

import com.sothatsit.royalur.simulation.Agent;
import com.sothatsit.royalur.simulation.Game;
import com.sothatsit.royalur.simulation.MoveList;
import com.sothatsit.royalur.simulation.Player;
import com.sothatsit.royalur.simulation.Pos;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import kong.unirest.json.JSONArray;

public class ExpectoAgent extends Agent {

    private final String baseUrl;

    public ExpectoAgent(String baseUrl) {
        super("Expecto");
        this.baseUrl = baseUrl;
    }

    @Override
    public Agent clone() {
        return new ExpectoAgent(baseUrl);
    }

    @Override
    public int determineMove(Game game, int roll, MoveList legalMoves) {
        if (legalMoves.count == 0)
            return -1;
        if (legalMoves.count == 1)
            return legalMoves.positions[0];
        Player aiPlayer = game.getActivePlayer();
        Player enemyPlayer = game.getInactivePlayer();

        String gameString = game.board.toString();
        JSONArray payloadAll = new JSONArray();

        for (int i = 0; i < legalMoves.count; i++) {
            int playerChip = legalMoves.positions[i];
            payloadAll.put(new JSONObject()
                    .put("game", gameString)
                    .put("light_score", aiPlayer.score)
                    .put("dark_score", enemyPlayer.score)
                    .put("roll", roll)
                    .put("light_left", aiPlayer.tiles)
                    .put("dark_left", enemyPlayer.tiles)
                    .put("x", Pos.getX(playerChip))
                    .put("y", Pos.getY(playerChip))
                    .put("light_turn", true));
        }

        HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/infer")
                .header("Content-Type", "application/json")
                .body(payloadAll)
                .asJson();

        JSONArray expectoPawnToPlay = response.getBody().getObject().getJSONArray("utilities");

        // convert to double array
        double[] utilities = new double[expectoPawnToPlay.length()];
        for (int i = 0; i < utilities.length; i++) {
            utilities[i] = expectoPawnToPlay.getDouble(i);
        }

        int aiPickedMoveIndex = getMaxUtilityIndex(utilities);
        return legalMoves.positions[aiPickedMoveIndex];
    }

    private int getMaxUtilityIndex(double[] utilities) {
        double maxUtility = Double.NEGATIVE_INFINITY;
        int maxUtilityIndex = 0;
        for (int i = 0; i < utilities.length; i++) {
            if (utilities[i] > maxUtility) {
                maxUtility = utilities[i];
                maxUtilityIndex = i;
            }
        }
        return maxUtilityIndex;
    }
}
