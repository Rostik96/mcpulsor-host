package der.rost.mcpulsorhost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
class App {

	static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}


	@Bean
	ApplicationListener<ApplicationReadyEvent> onStartup(Host host) {
	    return _ -> {
			var firstQuestion = "какой у меня пульс?";
			var secondQuestion = "как дела?";
			var thirdQuestion = "какой у меня будет пульс если к нему прибавить 1000?";
			host.printAnswerToUser(firstQuestion);
			host.printAnswerToUser(secondQuestion);
			host.printAnswerToUser(thirdQuestion);
		};
	}
}
