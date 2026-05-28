package der.rost.mcpulsorhost;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Host {

    private final ChatClient chatClient;
    private final ToolsTemplate toolsTemplate;


    public void printAnswerToUser(String question) {
        log.info("Host говорит, что пользователь задал вот  такой вопрос: {}", question);
        var assistantMessage = chatClient.prompt().user(question).call().chatResponse().getResult().getOutput();
        log.info("Первичный ответ модели:\n{}", assistantMessage.getText());
        toolsTemplate.callTools(assistantMessage)
                .ifPresent(toolResult -> {
                    var answer = chatClient.prompt()
                            .messages(List.of(
                                    new UserMessage(question),
                                    assistantMessage,
                                    new UserMessage(toolResult)
                            ))
                            .call().chatResponse().getResult().getOutput().getText();
                    log.info("Итоговый ответ от модели:\n{}", answer);
                });
    }
}
