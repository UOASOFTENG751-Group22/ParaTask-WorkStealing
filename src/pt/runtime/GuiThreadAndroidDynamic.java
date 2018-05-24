package pt.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class GuiThreadAndroidDynamic implements GuiThreadProxy {

	private GuiThreadAndroidDynamic() {
	}

	private static GuiThreadAndroidDynamic instance;

	public static GuiThreadAndroidDynamic getInstance() {
		if (instance == null)
			instance = new GuiThreadAndroidDynamic();
		return instance;
	}

	private static Class<?> handlerClass;
	private static Object handler;
	private static Method handlerPostMethod;

	private static Thread mainThread;

	public void init() {
		try {
			// implement the following three lines using reflection
			// Looper mainLooper = Looper.getMainLooper();
			// handler = new Handler(mainLooper);
			// mainThread = mainLooper.getThread();
			
			Class<?> looperClass = Class.forName("android.os.Looper");
			Method getMainLooperMethod = looperClass.getMethod("getMainLooper");
			Object mainLooper = getMainLooperMethod.invoke(looperClass);
			
			handlerClass = Class.forName("android.os.Handler");
			Constructor<?> handlerCtor = handlerClass.getDeclaredConstructor(looperClass);
			handler = handlerCtor.newInstance(mainLooper);

			Method getThreadMethod = looperClass.getMethod("getThread");
			mainThread = (Thread)getThreadMethod.invoke(mainLooper);
		} catch (Exception e) {
			// fatal error
			throw new RuntimeException(e);
		}
	}
	
	public Thread getEventDispatchThread() {
		return mainThread;
	}

	public boolean isEventDispatchThread() {
		return Thread.currentThread().equals(mainThread);
	}

	public void invokeLater(Runnable r) {
		try {
			if (handlerPostMethod == null) {
				handlerPostMethod = handlerClass.getMethod("post", Runnable.class);
			}
			handlerPostMethod.invoke(handler, r);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}