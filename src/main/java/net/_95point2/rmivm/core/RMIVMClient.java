package net._95point2.rmivm.core;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net._95point2.rmivm.core.Rmivm;
import net._95point2.rmivm.core.Rmivm.SendPackage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class RMIVMClient 
{
	private Connection connection;
	private Channel channel;
	private String replyQueueName;
	private QueueingConsumer consumer;

	public RMIVMClient() throws IOException
	{
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    connection = factory.newConnection();
	    channel = connection.createChannel();

	    replyQueueName = channel.queueDeclare().getQueue(); 
	    consumer = new QueueingConsumer(channel);
	    channel.basicConsume(replyQueueName, true, consumer);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProxy(final Class<T> type)
	{
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, new InvocationHandler() {
			
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
			{
				SendPackage sendPackage = Rmivm.serialize(method, args);
				
			    String corrId = java.util.UUID.randomUUID().toString();

			    BasicProperties props = new BasicProperties
			                                .Builder()
			                                .correlationId(corrId)
			                                .replyTo(replyQueueName)
			                                .build();

			    channel.basicPublish("", type.getCanonicalName(), props, sendPackage.params);
			    
			    Kryo kryo = new Kryo();
			    
			    Object response;

			    while (true) {
			        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
			            
			            Input input = new Input(new ByteArrayInputStream(delivery.getBody()));
			            if(delivery.getBody() == null || delivery.getBody().length == 0){
			            	input.close();
			            	return null;
			            }
			            response = kryo.readClassAndObject(input);
			            input.close();
			           
			            break;
			        }
			    }

			    return response; 
				
			}
		});
	}
	

}
