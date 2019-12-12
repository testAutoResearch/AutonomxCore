package core.support.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.azure.servicebus.primitives.StringUtil;

public class MessageObject {
	
	public enum messageType { KAFKA, RABBITMQ, SERVICEBUS }

	public messageType messageType = null;
	public String message = StringUtil.EMPTY;
	public String messageId = StringUtil.EMPTY;
	public String label = StringUtil.EMPTY;
	public String correlationId = StringUtil.EMPTY;
	public String topic = StringUtil.EMPTY;
	public List<String> headers = new ArrayList<String>();
	
	public static Map<MessageObject, Boolean> outboundMessages = new ConcurrentHashMap<MessageObject, Boolean>();
	
	public MessageObject withMessageType(messageType messageType) {
		this.messageType = messageType;
		return this;
	}
	
	public MessageObject withMessage(String message) {
		this.message = message;
		return this;
	}
	
	public MessageObject withMessageId(String messageId) {
		this.messageId = messageId;
		return this;
	}
	
	public MessageObject withLabel(String label) {
		this.label = label;
		return this;
	}
	
	public MessageObject withCorrelationId(String correlationId) {
		this.correlationId = correlationId;
		return this;
	}
	
	public MessageObject withTopic(String topic) {
		this.topic = topic;
		return this;
	}
	
	public MessageObject withHeader(List<String> header) {
		this.headers = header;
		return this;
	}
	
	public messageType getMessageType() {
		return this.messageType;
	}

	public String getMessage() {
		return this.message;
	}
	
	public String getMessageId() {
		return this.messageId;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public String getCorrelationId() {
		return this.correlationId;
	}
	
	public String getTopic() {
		return this.topic;
	}
	
	public List<String> getHeader() {
		return this.headers;
	}
}