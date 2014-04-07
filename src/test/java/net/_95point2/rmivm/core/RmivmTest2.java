package net._95point2.rmivm.core;

import java.io.IOException;

public class RmivmTest2 {

	public static interface TestInterface
	{
		public void doActionSomething();
		
		public String getSoemthingElse();
		
		public String getSomeotherThing(int input);
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException 
	{
		Server server = new Server();
		server.start();
		
		final RmivmTest2.TestInterface testInterfaceImpl = new RmivmTest2.TestInterface() {
			
			public String getSomeotherThing(int input) {
				return "You called: " + input;
			}
			
			public String getSoemthingElse() {
				return "HiRRR";
			}
			
			public void doActionSomething() {
				System.out.println("Called me");
			}
		};
		
		server.registerObject(TestInterface.class, testInterfaceImpl);
		
		
		RMIVMClient client = new RMIVMClient();
		
		TestInterface proxy = client.getProxy(TestInterface.class);
		
		proxy.doActionSomething();
		System.out.println( proxy.getSoemthingElse() );
		System.out.println( proxy.getSomeotherThing(333) );
		
		server.shutdown();
		System.exit(0);
		
	}

}
