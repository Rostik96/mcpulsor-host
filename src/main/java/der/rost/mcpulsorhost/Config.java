package der.rost.mcpulsorhost;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;


@Configuration
class Config {

    @Bean
    McpSyncClient mcpSyncClient(
            @Value("${mcp-client.base-uri}") String baseUri,
            @Value("${mcp-client.endpoint}") String endpoint) {
        return McpClient.sync(HttpClientStreamableHttpTransport
                        .builder(baseUri)
                        .endpoint(endpoint)
                        .build())
                .build();
    }


    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          @Value("system-prompt.txt") ClassPathResource systemPrompt,
                          ToolsTemplate toolsTemplate) {
        return builder
                .defaultSystem(prompt -> prompt
                                .text(systemPrompt)
                                .param("tools", toolsTemplate.listTools()))
                .build();
    }
}
