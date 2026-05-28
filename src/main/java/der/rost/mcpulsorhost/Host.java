package der.rost.mcpulsorhost;


import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ModelResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static org.apache.logging.log4j.util.Strings.EMPTY;

@Slf4j
@Service
@ExtensionMethod(Optional.class)
@RequiredArgsConstructor
public class Host {

    private final ChatClient chatClient;
    private final ToolsTemplate toolsTemplate;


    public void printAnswerToUser(String question) {
        log.info("Host говорит, что пользователь задал вот  такой вопрос: {}", question);
        var questionResponse = chatClient.prompt().user(question).call().chatResponse().ofNullable()
                .map(ModelResponse::getResult)
                .map(ModelResult::getOutput)
                .map(Message::getText)
                .orElse(EMPTY);
        log.info("Первичный ответ модели:\n{}", questionResponse);
        toolsTemplate.callTools(questionResponse)
                .ifPresent(toolResult ->
                        log.info("Итоговый ответ от модели:\n{}", chatClient.prompt()
                                .messages(List.of(
                                        new UserMessage(question),
                                        new AssistantMessage(questionResponse),
                                        new UserMessage(toolResult)
                                ))
                                .call().chatResponse().ofNullable()
                                .map(ModelResponse::getResult)
                                .map(ModelResult::getOutput)
                                .map(Message::getText)
                                .orElse(EMPTY)));
    }
}
