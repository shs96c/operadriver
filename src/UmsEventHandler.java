package com.opera.core.systems;

import java.util.List;
import java.util.Map;

import com.opera.core.systems.model.FilterRule;
import com.opera.core.systems.scope.protos.ConsoleLoggerProtos.ConsoleMessage;
import com.opera.core.systems.scope.protos.EsdbgProtos.RuntimeInfo;
import com.opera.core.systems.scope.protos.WmProtos.WindowInfo;


public class UmsEventHandler extends EventHandler {

	public UmsEventHandler(ScopeServices services) {
		super(services);
	}
	
	@Override
	public void onRuntimeStarted(RuntimeInfo info) {
		if(info.getHtmlFramePath().equals("_top") && info.getWindowID() == windowManager.getActiveWindowId()) {
			//check if we already have such a runtime and clean them up if needed
			debugger.cleanUpRuntimes();
			debugger.setRuntime(info);
		}
		debugger.addRuntime(info);
	}
	
	@Override
	public void onUpdatedWindow(WindowInfo info) {
		//logger.log(Level.INFO, window.toString());
		if(info.getOpenerID() == 0)
			windowManager.addWindow(info);
	}
	
	@Override
	public void onMessage(ConsoleMessage message) {
	}
	
	private boolean isFilteredMessage(Map<String, FilterRule> filters, ConsoleMessage message) {
		return false;
	}

}