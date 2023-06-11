package com.sothatsit.royalur.ai;

import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sothatsit.royalur.simulation.Agent;
import com.sothatsit.royalur.simulation.Game;
import com.sothatsit.royalur.simulation.MoveList;
import com.sothatsit.royalur.simulation.Player;
import com.sothatsit.royalur.simulation.Pos;
import com.sothatsit.royalur.simulation.Tile;

/**
 * An agent that calls the Go-ur (https://github.com/qwertyuu/go-ur) NEAT Api to get sweet sweet ML AI juice
 *
 * @author Raphaël Côté
 */
public class NeatoAgent extends Agent {
    private static Map<Integer, Integer> analysisToNeatoPos = new HashMap<Integer, Integer>() {{
        put(Pos.pack(0, 3), 0);
        put(Pos.pack(0, 2), 1);
        put(Pos.pack(0, 1), 2);
        put(Pos.pack(0, 0), 3);
        put(Pos.pack(1, 0), 4);
        put(Pos.pack(1, 1), 5);
        put(Pos.pack(1, 2), 6);
        put(Pos.pack(1, 3), 7);
        put(Pos.pack(1, 4), 8);
        put(Pos.pack(1, 5), 9);
        put(Pos.pack(1, 6), 10);
        put(Pos.pack(1, 7), 11);
        put(Pos.pack(0, 7), 12);
        put(Pos.pack(0, 6), 13);
        put(Pos.pack(2, 3), 0);
        put(Pos.pack(2, 2), 1);
        put(Pos.pack(2, 1), 2);
        put(Pos.pack(2, 0), 3);
        put(Pos.pack(2, 7), 12);
        put(Pos.pack(2, 6), 13);
        put(Pos.pack(0, 4), -1);
        put(Pos.pack(2, 4), -1);
    }};
    private static Map<Integer, Integer> neatoPosToAnalysisLight = new HashMap<Integer, Integer>() {{
        put(0, Pos.pack(0, 3));
        put(1, Pos.pack(0, 2));
        put(2, Pos.pack(0, 1));
        put(3, Pos.pack(0, 0));
        put(4, Pos.pack(1, 0));
        put(5, Pos.pack(1, 1));
        put(6, Pos.pack(1, 2));
        put(7, Pos.pack(1, 3));
        put(8, Pos.pack(1, 4));
        put(9, Pos.pack(1, 5));
        put(10, Pos.pack(1, 6));
        put(11, Pos.pack(1, 7));
        put(12, Pos.pack(0, 7));
        put(13, Pos.pack(0, 6));
        put(-1, Pos.pack(0, 4));
    }};
    private static Map<Integer, Integer> neatoPosToAnalysisDark = new HashMap<Integer, Integer>() {{
        put(0, Pos.pack(2, 3));
        put(1, Pos.pack(2, 2));
        put(2, Pos.pack(2, 1));
        put(3, Pos.pack(2, 0));
        put(4, Pos.pack(1, 0));
        put(5, Pos.pack(1, 1));
        put(6, Pos.pack(1, 2));
        put(7, Pos.pack(1, 3));
        put(8, Pos.pack(1, 4));
        put(9, Pos.pack(1, 5));
        put(10, Pos.pack(1, 6));
        put(11, Pos.pack(1, 7));
        put(12, Pos.pack(2, 7));
        put(13, Pos.pack(2, 6));
        put(-1, Pos.pack(2, 4));
    }};

    public NeatoAgent() {
        super("Neato");
    }

    @Override
    public NeatoAgent clone() {
        return new NeatoAgent();
    }

    @Override
    public int determineMove(Game game, int roll, MoveList legalMoves) {
        JSONObject obj = new JSONObject();
        Player ap = game.getActivePlayer();
        List<Integer> aiPawns = new ArrayList<Integer>();
        List<Integer> enemyPawns = new ArrayList<Integer>();
        
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 3; ++x) {
                int packedPos = Pos.pack(x, y);
                int tile = game.board.get(packedPos);
                if (tile == 0) {
                    continue;
                }
                if (tile == ap.tile) {
                    aiPawns.add(analysisToNeatoPos.get(packedPos));
                } else {
                    enemyPawns.add(analysisToNeatoPos.get(packedPos));
                }
            }
        }
        obj.put("pawn_per_player", 7);
        obj.put("ai_pawn_out", ap.score);
        obj.put("enemy_pawn_out", game.getInactivePlayer().score);
        obj.put("dice", roll);
        obj.put("ai_pawn_positions", aiPawns);
        obj.put("enemy_pawn_positions", enemyPawns);
        HttpResponse<JsonNode> response = Unirest.post("http://localhost:8090/infer")
            .header("accept", "application/json")
            .body(obj.toString())
            .asJson();
        int neatoPawnToPlay = response.getBody().getObject().getInt("pawn");
        Map<Integer, Integer> playerNeatoPosToAnalysis = null;
        if (ap.tile == Tile.LIGHT) {
            playerNeatoPosToAnalysis = neatoPosToAnalysisLight;
        } else {
            playerNeatoPosToAnalysis = neatoPosToAnalysisDark;
        }
        
        int toPlay = -1;
        if (neatoPawnToPlay == -1) {
            toPlay = playerNeatoPosToAnalysis.get(neatoPawnToPlay);
        } else {
            toPlay = playerNeatoPosToAnalysis.get(aiPawns.get(neatoPawnToPlay));
        }
        return toPlay;
    }
}
