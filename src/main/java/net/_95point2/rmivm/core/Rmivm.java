package net._95point2.rmivm.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Rmivm 
{
	public static String[] getParameterTypeNames(Method method)
	{
		Class<?>[] parameterTypes = method.getParameterTypes();
		String[] typeNames = new String[parameterTypes.length];
		for(int i=0;i<parameterTypes.length;i++){
			typeNames[i] = parameterTypes[i].getCanonicalName();
		}
		return typeNames;
	}
	
	public static Class<?>[] getParameterTypes(String[] classNames) throws ClassNotFoundException
	{
		Class<?>[] parameterTypes = new Class<?>[classNames.length];
		for(int i=0;i<classNames.length;i++){
			try {
				parameterTypes[i] = Class.forName(classNames[i]);
			}
			catch(ClassNotFoundException cnfe){
				Class<?> primitive = tryPrimitives(classNames[i]);
				if(primitive != null){
					parameterTypes[i] = primitive;
				}
				else {
					throw cnfe;
				}
			}
			
		}
		return parameterTypes;
	}
	
	private static Class<?> tryPrimitives(String className){
		return primitivesLookup.get(className);
	}
	
	private static final Map<String,Class<?>> primitivesLookup;
	
	static {
		HashMap<String,Class<?>> primitivesMap = new HashMap<String, Class<?>>();
		
		Class<?>[] primitives = new Class<?>[]{ int.class, long.class, short.class, byte.class, boolean.class, float.class, double.class, char.class };
		
		for(Class<?> primitive : primitives){
			primitivesMap.put(primitive.getCanonicalName(), primitive);
		}
		
		primitivesLookup = Collections.unmodifiableMap(primitivesMap);
		
	}
	
	public static byte[] serialize(Object responseObject) throws IOException
	{
		Kryo kryoOut = new Kryo();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Output output = new Output(baos);
		kryoOut.writeClassAndObject(output, responseObject);
		output.close();
		baos.flush();
		baos.close();
		return baos.toByteArray();
		
	}
	
	public static SendPackage serialize(Method method, Object[] args) throws IOException
	{
		String methodName = method.getName();
		String className = RmivmTest2.TestInterface.class.getCanonicalName();
		String[] paramTypeNames = getParameterTypeNames(method);
		
		Kryo kryoOut = new Kryo();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Output output = new Output(baos);
		
		kryoOut.writeObject(output, className);
		kryoOut.writeObject(output, methodName);
		kryoOut.writeObject(output, paramTypeNames);
		
		if(args != null && args.length > 0)
		for(Object arg : args){
			kryoOut.writeClassAndObject(output, arg);
		}
		
		
		output.close();
		baos.flush();
		baos.close();
		byte[] bytes = baos.toByteArray();
		
		return new SendPackage(className, methodName, paramTypeNames, bytes);
	}
	
	public static Object deserialise(byte[] sendPackage, RemoteObjectRegistry registry) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(sendPackage);
		Input input = new Input(bais);
		
		Kryo kryoIn = new Kryo();
		
		String clazzName = kryoIn.readObject(input, String.class);
		String mmethodName = kryoIn.readObject(input, String.class);
		String[] paramTypeNames = kryoIn.readObject(input, String[].class);
		
		Class<?>[] types = getParameterTypes(paramTypeNames);
		
		Object[] argsIn = new Object[types.length];
		for(int i=0;i < types.length; i++){
			Class<?> type = types[i];
			Object o = kryoIn.readClassAndObject(input);
			argsIn[i] = o;
		}
		
		Object target = registry.getRemoteObject(clazzName);
		
		Method methodToCall = target.getClass().getMethod(mmethodName, types);
		
		if(argsIn.length == 0){
			return methodToCall.invoke(target);
		}
		
		return methodToCall.invoke(target, argsIn);
		
	}
	
	
	
	public static interface RemoteObjectRegistry 
	{
		Object getRemoteObject(String type);
	}
	
	public static class DefaultRemoteObjectRegistry implements RemoteObjectRegistry
	{
		HashMap<String, Object> objects = new HashMap<String, Object>();
		
		public <T> void register(Class<T> type, T object)
		{
			objects.put(type.getCanonicalName(), object);
		}

		public Object getRemoteObject(String type) {
			return objects.get(type);
		}
	}
	
	
	public static class SendPackage 
	{
		String className;
		String methodName;
		String[] parameterTypeNames;
		public byte[] params;
		
		public SendPackage(String className2, String methodName2, String[] paramTypeNames, byte[] bytes) 
		{
			this.className = className2;
			this.methodName = methodName2;
			this.parameterTypeNames = paramTypeNames;
			this.params = bytes;
		}
		
		
	}

}
