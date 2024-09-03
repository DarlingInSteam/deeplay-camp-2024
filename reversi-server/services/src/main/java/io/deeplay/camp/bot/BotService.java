package io.deeplay.camp.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.deeplay.camp.board.BoardService;
import io.deeplay.camp.dto.BotMoveRequest;
import io.deeplay.camp.dto.BotMoveResponse;
import io.deeplay.camp.entity.Board;
import io.deeplay.camp.entity.Tile;
import io.deeplay.camp.enums.Bots;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class BotService extends BotStrategy {
    private final String botFactoryUrl = "http://localhost:8082/bot";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient;

    public BotService(int id, String name, Bots bot) {
        super(id, name, bot);
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(100); 
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }
    
    // Регистрация ошибки
    public Tile getBotMove(Board board, int currentPlayerId) throws IOException {
        BotMoveRequest request = new BotMoveRequest(board, currentPlayerId);

        String requestBody = objectMapper.writeValueAsString(request);
        HttpPost httpPost = new HttpPost(botFactoryUrl + "/darling/minimax/move");
        
        switch (bot) {
            case DARLING -> httpPost = new HttpPost(botFactoryUrl + "/darling/minimax/move");
            case VIOLA -> httpPost = new HttpPost(botFactoryUrl + "/viola/minimax/move");
            case ANDREY -> httpPost = new HttpPost(botFactoryUrl + "/andrey/minimax/move");
            default -> httpPost = new HttpPost(botFactoryUrl + "/random/minimax/move");
        }
        
        httpPost.setEntity(new StringEntity(requestBody));
        httpPost.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getCode() == 200) {
                BotMoveResponse botMoveResponse = objectMapper.readValue(response.getEntity().getContent(), BotMoveResponse.class);
                return botMoveResponse.getMove();
            } else {
                // Как-то так
//                return new RandomBot(currentPlayerId, "Bot", Bots.RANDOM).getMove();
                throw new IOException("Failed to get bot move: " + response.getReasonPhrase());
            }
        }
    }

    @Override
    public Tile getMove(int currentPlayerId, @NotNull BoardService boardLogic) {
        try {
            return getBotMove(boardLogic.getBoard(), currentPlayerId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Tile> getAllValidMoves(int currentPlayerId, @NotNull BoardService boardLogic) {
        return List.of();
    }
}