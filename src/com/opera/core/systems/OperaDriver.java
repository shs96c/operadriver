/*
Copyright 2007-2009 WebDriver committers
Copyright 2007-2009 Google Inc.
Copyright 2009 Opera Software ASA.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.opera.core.systems;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.Speed;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByClassName;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByLinkText;
import org.openqa.selenium.internal.FindsByName;
import org.openqa.selenium.internal.FindsByTagName;
import org.openqa.selenium.internal.FindsByXPath;
import org.openqa.selenium.internal.ReturnedCookie;

import com.opera.core.systems.model.Action;
import com.opera.core.systems.model.Canvas;
import com.opera.core.systems.model.ScopeActions;
import com.opera.core.systems.model.ScreenShotReply;
import com.opera.core.systems.model.ScriptResult;
import com.opera.core.systems.model.UserInteraction;
import com.opera.core.systems.scope.handlers.PbActionHandler;
import com.opera.core.systems.scope.handlers.UmsActionHandler;
import com.opera.core.systems.scope.handlers.XmlActionHandler;
import com.opera.core.systems.scope.internal.OperaBinary;
import com.opera.core.systems.scope.internal.OperaIntervals;
import com.opera.core.systems.scope.services.model.IEcmaScriptDebugger;
import com.opera.core.systems.scope.services.model.IOperaExec;
import com.opera.core.systems.scope.services.model.IWindowManager;

public class OperaDriver implements WebDriver, FindsByLinkText, FindsById,FindsByXPath, FindsByName, FindsByTagName, FindsByClassName,
		FindsByCssSelector, SearchContext, JavascriptExecutor {
	private IEcmaScriptDebugger debugger;
	private IOperaExec exec;
	private IWindowManager windowManager;
	private ScopeServices services;

	protected ScopeActions actionHandler;
	
	private OperaBinary binary;
	
	public OperaBinary getBinary() {
		return binary;
	}

	public IEcmaScriptDebugger getScriptDebugger() {
		return debugger;
	}

	public IOperaExec getExecService() {
		return exec;
	}

	public IWindowManager getWindowManager() {
		return windowManager;
	}

	public ScopeServices getScopeServices() {
		return services;
	}

	// TODO
	// Profiling
	
	public OperaDriver() {
		this(null);
	}
	

	/**
	 * Creates an OperaDriver instance, creates an instance of 
	 * ScopeServices (common point for managing services) and creates
	 * the required services (such as exec, window manager and ecmascript-debugger).
	 * ScopeServices is also the event handler for scope events, and we need to
	 * initialize window-manager and set filters to be able to use ecmascript debugger
	 * (To skip that you can just set filter to *).
	 * This build of driver supports both Core 2.3 and 2.4+
	 * @param executableLocation The exact location of where Opera binary is located
	 * @param arguments Arguments to pass for execution
	 */
	public OperaDriver(String executableLocation, String... arguments) {
		services = new ScopeServices();
		
		String operaPath = System.getenv().get("OPERA_PATH");
		String operaArgs = System.getenv().get("OPERA_ARGS");
		
		if(operaPath != null && operaPath.length() > 0) {
			binary = new OperaBinary(operaPath, operaArgs.split(","));
		} else if(executableLocation != null) {
			binary = new OperaBinary(executableLocation, arguments);
		}
		
		services.init();
		
		this.debugger = services.getDebugger();
		this.windowManager = services.getWindowManager();
		this.exec = services.getExec();
		
		List<String> listedServices = services.getConnection().getListedServices();
		if(listedServices.contains("stp-1")) {
			actionHandler = new PbActionHandler(services);
		} else {
			if (services.getConnection().getListedServices().contains("core-2-4")) {
				actionHandler = new UmsActionHandler(services);
			} else {
				actionHandler = new XmlActionHandler(services);
			}
		}

		services.setActionHandler(actionHandler);
	}

	public void get(String url) {
		get(url, OperaIntervals.PAGE_LOAD_TIMEOUT.getValue());
		/*
		//int oldId = debugger.getRuntimeId();
		debugger.releaseObjects();
		windowManager.setLoadCompleteLatch(new CountDownLatch(1));
		actionHandler.get(url);
		/*
		long end = System.currentTimeMillis() + OperaIntervals.PAGE_LOAD_TIMEOUT.getValue();
		while(debugger.getRuntimeId() == oldId) {
			sleep(OperaIntervals.POLL_INVERVAL.getValue());
			if(System.currentTimeMillis() >= end)
				break;
		}
		waitForLoadToComplete();
		*/
		/*
		windowManager.waitForWindowLoaded();
		*/
	}
	
	
	public int get(String url, long timeout) {
		debugger.releaseObjects();
		windowManager.setLoadCompleteLatch(new CountDownLatch(1));
		actionHandler.get(url);
		windowManager.waitForWindowLoaded(timeout);
		return windowManager.getLastHttpResponseCode().getAndSet(0);
	}

	public String getCurrentUrl() {
		return debugger.executeJavascript("return document.location.href");
	}
	
	public void gc() {
		debugger.releaseObjects();
	}

	public Dimension getDimensions() {
		String[] dimensions = (debugger.executeJavascript("return (window.innerWidth + \",\" + window.innerHeight")).split(",");
		return new Dimension(Integer.valueOf(dimensions[0]), Integer.valueOf(dimensions[1]));
	}

	 //Chris' way
    public String getText(){
        return debugger.executeJavascript("var visibleText = \"\";\n"+
                "    var travers = function(ele)\n"+
                "    {\n"+
                "      var children = ele.childNodes, child = null, i = 0, computedStyle = null;\n"+
                "      for( ; child = children[i]; i++)\n"+
                "      {\n"+
                "        switch (child.nodeType)\n"+
                "        {\n"+
                "          case document.ELEMENT_NODE:\n"+
                "          {\n"+
                "            computedStyle = getComputedStyle(child, null);\n"+
                "            if( computedStyle.getPropertyValue('display') != \"none\" &&\n"+
                "                computedStyle.getPropertyValue('visibility') != \"hidden\" &&\n"+
                "                !/^select$/i.test(child.nodeName) )\n"+
                "            {\n"+
                "              travers(child);\n"+
                "            }\n"+
                "            break;\n"+
                "          }\n"+
                "          case document.CDATA_SECTION_NODE:\n"+
                "          case document.TEXT_NODE:\n"+
                "          {\n"+
                "            visibleText += child.nodeValue;\n"+
                "          }\n"+
                "        }\n"+
                "\n"+
                "      }\n"+
                "      if( /^select|input$/i.test(ele.nodeName) &&\n"+
                "                /^text|button|file|$/i.test(ele.type) )\n"+
                "      {\n"+
                "        visibleText += ele.value\n"+
                "      }\n"+
                "    };\n"+
                "    travers(document);\n"+
                "    return visibleText;");
    }

	public void close() {
		closeWindow();
		//FIXME implement a queuing system
		windowManager.filterActiveWindow();
	}
	
	public void closeAll() {
		int openWindows = windowManager.getOpenWindowCount();
		for (int i = 0; i < openWindows; i++) {
			closeWindow();
		}
		windowManager.filterActiveWindow();
	}
	
	private void closeWindow() {
		windowManager.setWindowClosedLatch(new CountDownLatch(1));
		exec.action("Close page");
		windowManager.waitForWindowClosed();
	}

	public void stop() {
		exec.action("Stop");
	}

	public WebElement findElement(By by) {
		return by.findElement((SearchContext) this);
	}

	public List<WebElement> findElements(By by) {
		return by.findElements((SearchContext) this);
	}

	public String getPageSource() {
		return debugger.executeJavascript("return document.documentElement.outerHTML");
	}

	public String getTitle() {
		return debugger.executeJavascript("return document.title;");
	}

	public String getWindowHandle() {
		return String.valueOf(windowManager.getActiveWindowId());
	}

	public Set<String> getWindowHandles() {
		return windowManager.getWindowHandles();
	}

	public Options manage() {
		return new OperaOptions();
	}

	public Navigation navigate() {
		return new OperaNavigation();
	}

	public void quit() {
		if(exec.getActionList().contains("Quit"))
			exec.action("Quit");
		else
			exec.action("Exit");
		services.getConnection().close();
		if(binary != null){
			binary.kill();
		}
	}

	public TargetLocator switchTo() {
		return new OperaTargetLocator();
	}
	
	private class OperaTargetLocator implements TargetLocator {

		public WebElement activeElement() {
			return OperaDriver.this.findActiveElement();
		}

		public WebDriver defaultContent() {
			//change to _top
			windowManager.filterActiveWindow();
			debugger.changeRuntime("");
			waitForLoadToComplete();
			return OperaDriver.this;
		}

		public WebDriver frame(int frameIndex) {
			debugger.changeRuntime(frameIndex);
			return OperaDriver.this;
		}

		public WebDriver frame(String frameName) {
			debugger.changeRuntime(frameName);
			return OperaDriver.this;
		}

		public WebDriver window(String windowName) {
			windowManager.setActiveWindow(windowName); //find by title
			defaultContent(); //set runtime to _top
			debugger.executeJavascript("window.focus()", false); //steal focus
			return OperaDriver.this;
		}
		
	}
	
	/**
	 * TODO: Add to official API?
	 * @return list of frames available for chosing
	 */
	public List<String> listFrames(){
		return debugger.listFramePaths();
	}
	
	public WebElement findActiveElement() {
		return findSingleElement("document.activeElement;", "active element");
	}
	

	// TODO Benchmark, XPath is supposed to be faster?
	public WebElement findElementByLinkText(String using) {	
		return findSingleElement("var elements = document.getElementsByTagName('a');\n" +
				"var element = null;\n" +
				"var i = 0;\n" +
				"for(;element = elements[i]; i++) {\n" +
				" if(element.textContent == '"+ using + "'){\n"+
				" return element; }\n"+
				"}", "link text");
	}

	public WebElement findElementByPartialLinkText(String using) {
		return findSingleElement("var elements = document.getElementsByTagName('a');\n" +
				"var element = null;\n" +
				"var i = 0;\n" +
				"for(;element = elements[i]; i++) {\n" +
				"if( element.textContent.indexOf('"+ using + "') > -1 ){\n"+
				"return element; }\n"+
				"} return \"No element found\";", "partial link text");
		
		
	}

	public List<WebElement> findElementsByLinkText(String using) {
		return findMultipleElements("var links = document.links, link = null, i = 0, elements = [];\n"+
				"for( ; link = links[i]; i++)\n"+
				"{\n"+
				"if(link.textContent == '" + using +"')\n"+
				"{\n"+
				"elements.push(link);\n"+
					"}\n"+
				"}\n" +
				"return elements;", "link text");

		
	}
	
	protected List<WebElement> processElements(Integer id){		
		List<Integer> ids = debugger.examineObjects(id);
		List<WebElement> toReturn = new ArrayList<WebElement>();
		for (Integer objectId : ids)
			toReturn.add(new OperaWebElement(this, objectId));
		return toReturn;
	}
	
	public List<WebElement> findElementsByPartialLinkText(String using) {
		return findMultipleElements("var links = document.links, link = null, i = 0, elements = [];\n" +
				"for( ; link = links[i]; i++)\n" +
				"{\n" +
				"if(link.textContent.indexOf('" + using +"') > -1)\n" +
				"{\n" +
				"elements.push(link);\n" +
					"}\n" +
				"}\n" +
				"return elements;", "partial link text");
	}

	public WebElement findElementById(String using) {
		//return findSingleElement("document.querySelector(\"#\" + " + using + ")", "id");
		return findSingleElement("document.getElementById('" + using + "');", "id");
	}

	/**
	 * This method breaks web standards
	 */
	public List<WebElement> findElementsById(String using) {
		return findMultipleElements("var alls = document.all, element = null, i = 0, elements = [];\n" +
				"for( ; element = alls[i]; i++)\n"+
				"{\n"+
				    "if(element.getAttribute('id') == '" + using +"')\n"+
				    "{\n"+
				      "elements.push(element);\n"+
				    "}\n"+
				  "}\n"+
				  "return elements;", "by id");

	}

	public WebElement findElementByXPath(String using) {
		return findSingleElement("document.evaluate(\"" + using +"\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE,  null).singleNodeValue;\n", "xpath");
	}

	public WebElement findElementByClassName(String using) {
		return findSingleElement("document.getElementsByClassName('" + using +"')[0];", "class name");
	}

	public List<WebElement> findElementsByClassName(String using) {
		return findMultipleElements("document.getElementsByClassName('"+ using + "');\n", "class name");
	}

	public List<WebElement> findElementsByXPath(String using) {
		return findMultipleElements("var result = document.evaluate(\""
						+ using
						+ "\", document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE,  null);\n"
						+ "var elements = new Array();\n"
						+ "var element = result.iterateNext();\n"
						+ "while (element) {\n" + "  elements.push(element);\n"
						+ "  element = result.iterateNext();\n" + "}\n"
						+ "return elements", "XPath");
	}

	//FIXME when timeout has completed, send 'stop' command?
	public void waitForLoadToComplete() {
		long endTime = System.currentTimeMillis() + OperaIntervals.PAGE_LOAD_TIMEOUT.getValue();
		while (!"complete".equals(debugger.executeJavascript("return document.readyState"))) {
			if(System.currentTimeMillis() < endTime) 
				sleep(OperaIntervals.POLL_INVERVAL.getValue());
			else
				throw new WebDriverException("Timeout while loading page");
		}
	}

	public WebElement findElementByName(String using) {
		return findSingleElement("document.getElementsByName('" + using +"')[0];","name");
	}

	public List<WebElement> findElementsByName(String using) {
		return findMultipleElements("document.getElementsByName('"+ using + "');", "name");
	}

	private class OperaNavigation implements Navigation {
		public void back() {
			exec.action("Back");
			waitForLoadToComplete();
		}

		public void forward() {
			exec.action("Forward");
			waitForLoadToComplete();
		}

		public void to(String url) {
			get(url);
		}

		public void to(URL url) {
			get(String.valueOf(url));
		}

		public void refresh() {
			exec.action("Reload");
			waitForLoadToComplete();
		}

	}

	private class OperaOptions implements Options {
		public void addCookie(Cookie cookie) {
			debugger.executeJavascript("document.cookie = '" + cookie.toString() + "'", false);
		}

		public void deleteCookieNamed(String name) {
			deleteCookie(new ReturnedCookie(name, "", getCurrentHost(), "", null, false));
		}

		public void deleteCookie(Cookie cookie) {
			Date dateInPast = new Date(0);
			Cookie toDelete = new ReturnedCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), dateInPast, false);
			addCookie(toDelete);
		}

		public void deleteAllCookies() {
			Set<Cookie> cookies = getCookies();
			for (Cookie cookie : cookies) {
				deleteCookie(cookie);
			}
		}

		public Set<Cookie> getCookies() {
			String currentUrl = getCurrentHost();

			Set<Cookie> toReturn = new HashSet<Cookie>();
			String allDomainCookies = debugger.executeJavascript("return document.cookie");

			String[] cookies = allDomainCookies.split(";");
			for (String cookie : cookies) {
				String[] parts = cookie.split("=");
				if (parts.length != 2) {
					continue;
				}

				toReturn.add(new ReturnedCookie(parts[0].trim(), parts[1].trim(), currentUrl,"", null, false));
			}

			return toReturn;
		}

		public Speed getSpeed() {
			throw new UnsupportedOperationException("getMouseSpeed");
		}

		public void setSpeed(Speed speed) {
			throw new UnsupportedOperationException("setMouseSpeed");
		}

		private String getCurrentHost() {
			try {
				URL url = new URL(getCurrentUrl());
				return url.getHost();
			} catch (MalformedURLException e) {
				return "";
			}
		}

		public Cookie getCookieNamed(String name) {
			String value = debugger.executeJavascript("var getCookieNamed = function(key)\n"+
														  "{"+
														  "var value = new RegExp(key + \"=([^;]*)\").exec(document.cookie);"+
														  "return value && decodeURIComponent(value[1]);"+
														  "}\n"+
														  "return getCookieNamed('" + name + "')");
			return (value == null) ? null : new Cookie(name, value);
		}
	}
	
	public void operaAction(String using, String... params) {
		exec.action(using, params);
	}
	
	private static void sleep(long timeInMillis) {
		try {
			Thread.sleep(timeInMillis);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	public WebElement findElementByTagName(String using) {
		return findSingleElement("document.getElementsByTagName('" + using +"')[0];", "tag name");
	}


	public List<WebElement> findElementsByTagName(String using) {
		return findMultipleElements("document.getElementsByTagName('"+ using + "');\n", "name");
	}

	public WebElement findElementByCssSelector(String using) {
		return findSingleElement("document.querySelector('" + using +"');", "selector");
	}

	public List<WebElement> findElementsByCssSelector(String using) {
		return findMultipleElements("document.querySelectorAll('"+ using + "'), returnValue = [], i=0;for(;returnValue[i]=results[i];i++); return returnValue;", "selector");
	}

	private final List<WebElement> findMultipleElements(String script, String type) {
		Integer id = debugger.getObject(script);

		if (id == null) {
			throw new NoSuchElementException("Cannot find element(s) with " + type);
		}
		return processElements(id);
	}
	
	private final WebElement findSingleElement(String script, String type) {
		Integer id = debugger.getObject(script);

		if (id != null) {
			return new OperaWebElement(this, id);
		}

		throw new NoSuchElementException("Cannot find element with " + type);
	}


	public void saveScreenshot(File pngFile) {
		actionHandler.saveScreenshot(pngFile);
	}

	public String saveScreenshot(String fileName, int timeout, String... hashes) {		
		return screenWatcher(fileName, timeout, true, hashes);
	}
	
	public String computeHash(int timeout, String... hashes) {
		return screenWatcher("", timeout, false, hashes);
	}
	
	private String screenWatcher(String fileName, int timeout, boolean saveFile, String... hashes){
		Canvas canvas = new Canvas();
		canvas.setX(0);
		canvas.setY(0);

		String[] dimensions = debugger.executeJavascript("return (window.innerWidth + \",\" + window.innerHeight);").split(",");
		canvas.setH(Integer.valueOf(dimensions[1]));
		canvas.setW(Integer.valueOf(dimensions[0]));
		canvas.setViewPortRelative(true);
		
        ScreenShotReply screenshot = exec.screenWatcher(canvas, timeout, saveFile, hashes);
        if(saveFile && screenshot.getPng() != null){
			FileOutputStream stream;
			try {
				stream = new FileOutputStream(fileName);
				stream.write(screenshot.getPng());
				stream.close();
			} catch (Exception e) {
				throw new WebDriverException("Failed to write file: " + e.getMessage());
			}
        }
		return screenshot.getMd5();
	}

	public Object executeScript(String script, Object... args) {
		Object object = debugger.scriptExecutor(script, args);

		//we probably have an element OR list
		if(object instanceof ScriptResult) {
			ScriptResult result = (ScriptResult) object;
			Integer objectId = result.getObjectId();
			if(objectId == null)
				return null;
			if(result.getClassName().endsWith("Element"))
				return new OperaWebElement(this, objectId);
			if(result.getClassName().equals("NodeList"))
				return processElements(objectId);
			if(result.getClassName().equals("Array"))
				return processObjects(objectId);
		}
		return object;
	}

	protected List<Object> processObjects(Integer id) {
		List<Integer> ids = debugger.examineObjects(id);
		List<Object> toReturn = new ArrayList<Object>();
		for (Integer objectId : ids)
			toReturn.add(debugger.callFunctionOnObject("locator", objectId, true));
		return toReturn;
		
	}
	
	public boolean isJavascriptEnabled() {
		// FIXME we always assume it is true
		// TODO it should not be possible to register esdbg if js is disabled?
		return true;
	}

	public void cleanUp() {
		services.getConnection().close();
	}

	public void executeActions(Action action) {
		List<UserInteraction> actions = action.getActions();
		for (UserInteraction userInteraction : actions) {
			userInteraction.execute(this);
		}
		waitForLoadToComplete();
	}

}