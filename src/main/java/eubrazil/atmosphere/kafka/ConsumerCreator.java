package eubrazil.atmosphere.kafka;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import eubrazil.atmosphere.config.appconfig.PropertiesManager;

public class ConsumerCreator {

	private static String TOPIC = "topic-planning";
	private static Consumer<Long, String> consumer = null;
	
	private ConsumerCreator() {
	}
	
	public static Consumer<Long, String> createConsumer() {
		if (consumer == null) {
			consumer = ConsumerCreator.create();
		}
		return consumer;
	}

    public static Consumer<Long, String> create() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PropertiesManager.getInstance().getProperty("bootstrapServers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, PropertiesManager.getInstance().getProperty("groupIdConfig"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.parseInt(PropertiesManager.getInstance().getProperty("maxPollRecords")));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, PropertiesManager.getInstance().getProperty("offsetResetEarlier"));

        Consumer<Long, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));

        return consumer;
    }
	
}
