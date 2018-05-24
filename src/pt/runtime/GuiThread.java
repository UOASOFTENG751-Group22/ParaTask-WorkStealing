package pt.runtime;


public class GuiThread {

	private static GuiThreadProxy proxy;
	
	private static boolean initialized = false;
	
	public static void init() {
		if (initialized)
			return;
		
		String vmName = System.getProperty("java.vm.name");
		if (vmName.equals("Dalvik")) {
			proxy = GuiThreadAndroidDynamic.getInstance();
		} else {
			proxy = GuiThreadSwing.getInstance();
		}
		
		proxy.init();
		initialized = true;
	}
	
	public static Thread getEventDispatchThread() {
		return proxy.getEventDispatchThread();
	}

	public static boolean isEventDispatchThread() {
		return proxy.isEventDispatchThread();
	}

	public static void invokeLater(Runnable r) {
		proxy.invokeLater(r);
	}
}
