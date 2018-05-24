package pt.runtime;


public interface GuiThreadProxy {
	void init();
	Thread getEventDispatchThread();
	boolean isEventDispatchThread();
	void invokeLater(Runnable r);
}
