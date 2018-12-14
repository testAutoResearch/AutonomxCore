package core.helpers;

import org.openqa.selenium.interactions.Actions;

import core.uiCore.drivers.AbstractDriver;
import core.uiCore.webElement.EnhancedBy;
import core.uiCore.webElement.EnhancedWebElement;

public class ElementActionHelper {

	/**
	 * hover over element
	 * 
	 * @param by
	 */
	protected static void hoverBy(EnhancedBy by) {

		Actions actions = new Actions(AbstractDriver.getWebDriver());
		EnhancedWebElement targetElement = Element.findElements(by);
		actions.moveToElement(targetElement.get(0)).build().perform();
	}

	/*
	 * Enter text to an element by action
	 */
	protected void inputTextByAction(EnhancedBy by, String text) {
		EnhancedWebElement targetElement = Element.findElements(by);
		Actions action = new Actions(AbstractDriver.getWebDriver());
		action.moveToElement(targetElement.get(0)).click().sendKeys(text).build().perform();
	}

	/*
	 * Double click an element
	 */
	public static void doubleClickBy(EnhancedBy by) {
		EnhancedWebElement targetElement = Element.findElements(by);
		Helper.wait.waitForElementToBeClickable(by);
		Actions action = new Actions(AbstractDriver.getWebDriver());
		action.doubleClick(targetElement.get(0)).perform();
	}
}