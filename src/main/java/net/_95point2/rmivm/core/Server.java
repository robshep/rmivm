package net._95point2.rmivm.core;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import net._95point2.rmivm.core.Rmivm.DefaultRemoteObjectRegistry;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class Server extends Thread 
{
	DefaultRemoteObjectRegistry registry;;
	Connection connection;
	Channel channel;
	QueueingConsumer consumer;
	
	AtomicBoolean run = new AtomicBoolean(true);
	
	public Server() throws IOException
	{
		registry = new DefaultRemoteObjectRegistry();
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");

		connection = factory.newConnection();
		channel = connection.createChannel();
		consumer = new QueueingConsumer(channel);
		
		setDaemon(true);
	}
	
	public <T> void registerObject(Class<T> type, T object) throws IOException
	{
		synchronized (registry) {
			registry.register(type, object);
		}
		
		synchronized (channel) {
			channel.queueDeclare(type.getCanonicalName(), false, false, true, null);
			channel.basicConsume(type.getCanonicalName(), false, consumer);
		}
	}
	
	public void shutdown(){
		run.set(false);
		synchronized (channel) {
			try {
				
				channel.close();
				connection.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void run() 
	{
		System.out.println(" [x] Awaiting RPC requests");

		while (run.get()) {
			
			QueueingConsumer.Delivery delivery;
			try {
				delivery = consumer.nextDelivery(2000);
			} catch (ShutdownSignalException e2) {
				break;
			} catch (ConsumerCancelledException e2) {
				break;
			} catch (InterruptedException e2) {
				continue;
			}
			
		    BasicProperties props = delivery.getProperties();
		    BasicProperties replyProps = new BasicProperties
		                                     .Builder()
		                                     .correlationId(props.getCorrelationId())
		                                     .build();
			
			try
			{
				Object o;
				synchronized (registry) {
					o = Rmivm.deserialise(delivery.getBody(), registry);
				}
				
			    byte[] outBytes = Rmivm.serialize(o);
			    
			    synchronized (channel) {
			    	channel.basicPublish( "", props.getReplyTo(), replyProps, outBytes);
				    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
				}
			    
			    
			}
			catch(Exception e){
				try {
					synchronized (channel) {
						channel.basicPublish( "", props.getReplyTo(), replyProps, Rmivm.serialize(new Exception("Boom!")));
					}
					
				} catch (IOException e1) {
					// panic
				}
			}
		}
		
		System.out.println(" [x] Shutting Down");
	}

}
