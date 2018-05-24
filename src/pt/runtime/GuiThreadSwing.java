package pt.runtime;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

public class GuiThreadSwing implements GuiThreadProxy {

	private GuiThreadSwing() {
	}

	private static GuiThreadSwing instance;

	public static GuiThreadSwing getInstance() {
		if (instance == null)
			instance = new GuiThreadSwing();
		return instance;
	}
	
	private Thread edt;
	
	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					edt = Thread.currentThread();
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Thread getEventDispatchThread() {
		return edt;
	}

	public boolean isEventDispatchThread() {
		return SwingUtilities.isEventDispatchThread();
	}

	public void invokeLater(Runnable r) {
		SwingUtilities.invokeLater(r);
	}
}
