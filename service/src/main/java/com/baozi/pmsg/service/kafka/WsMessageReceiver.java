package com.baozi.pmsg.service.kafka;

import com.baozi.pmsg.facade.model.WsMessage;
import com.baozi.pmsg.service.atom.IWsMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: baozi
 * @date: 2019/8/8 15:37
 * @description:
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "kafka.enable", havingValue = "true")
public class WsMessageReceiver {

	/**
	 * 反射对象缓存
	 */
	private Map<String, Class> wsMsgClassCache = new HashMap<>(2);

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	IWsMessageService wsMessageService;

	@KafkaListener(groupId = "${ws.group}", topics = "${ws.topic}")
	public void receive(ConsumerRecord<String, String> record) {

		// 1. 反序列化消息
		log.info("receiver {}:{}", record.key(), record.value());
		Object object;
		try {
			object = deserialize(record.key(), record.value());
		}
		catch (Exception e) {
			log.error("deserialize fail" + e.getMessage(), e);
			return;
		}

		// 2. 推送消息到ws
		if (object instanceof WsMessage) {
			WsMessage wsMessage = (WsMessage) object;
			log.info("handle topic message:{}", wsMessage.getPayload());
			wsMessageService.send(wsMessage);
		}
		else {
			log.warn("no care message type:{}", record.key());
		}

	}

	/**
	 * 反序列化
	 * @param className
	 * @param body
	 * @return
	 * @throws Exception
	 */
	private Object deserialize(String className, String body) throws Exception {
		Class aClass = wsMsgClassCache.get(className);
		if (aClass == null) {
			aClass = Class.forName(className);
			wsMsgClassCache.put(className, aClass);
		}
		Object object = objectMapper.readValue(body, aClass);
		return object;
	}

}
