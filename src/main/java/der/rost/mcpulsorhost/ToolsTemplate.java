package der.rost.mcpulsorhost;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.System.lineSeparator;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

@Slf4j
@Service
@RequiredArgsConstructor
class ToolsTemplate {

    private final McpSyncClient mcpSyncClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern toolCallPattern =
            Pattern.compile("<tool_call>\\s*(\\{.*?})\\s*</tool_call>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    Optional<String> callTools(String message) {
        var matcher = toolCallPattern.matcher(message);
        if (!matcher.find()) return Optional.empty();
        log.info("The model asks you to call the tool:\n{}", message);
        var response = mcpSyncClient.callTool(extractTool(matcher.group(1))).content().stream()
                .map("<tool_response>%n%s%n</tool_response>"::formatted)
                .collect(joining(lineSeparator()));
        log.info("MCP server answer:\n{}", response);
        return Optional.of(response);
    }


    @SneakyThrows
    private McpSchema.CallToolRequest extractTool(String toolCallRequestJson) {
        var tool = mapper.readTree(toolCallRequestJson.trim());
        return McpSchema.CallToolRequest.builder()
                .name(tool.path("name").asText())
                .arguments(mapper.convertValue(tool.path("parameters"), Map.class))
                .build();

    }


    String listTools() {
        return mcpSyncClient.listTools().tools().stream()
                .map(this::formatTools)
                .collect(joining(lineSeparator()));
    }


    private String formatTools(McpSchema.Tool tool) {
        var sb = new StringBuilder();
        sb.append("\n- name: ").append(tool.name());

        appendIfPresent(sb, "\n  title: ", tool.title());
        appendIfPresent(sb, "\n  description: ", tool.description());
        appendIfPresent(sb, "\n  inputSchema: ", tool.inputSchema());
        appendIfNotEmpty(sb, "\n  outputSchema: ", tool.outputSchema());
        appendIfPresent(sb, "\n  annotations: ", tool.annotations());
        appendIfNotEmpty(sb, "\n  meta: ", tool.meta());

        sb.append("\n");
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String prefix, Object value) {
        Optional.ofNullable(value)
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .ifPresent(s -> sb.append(prefix).append(s));
    }

    private void appendIfNotEmpty(StringBuilder sb, String prefix, Map<?, ?> map) {
        Optional.ofNullable(map)
                .filter(not(Map::isEmpty))
                .ifPresent(m -> sb.append(prefix).append(m));
    }
}
