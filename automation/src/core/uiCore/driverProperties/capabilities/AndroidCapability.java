package core.uiCore.driverProperties.capabilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openqa.selenium.remote.DesiredCapabilities;

import core.helpers.Helper;
import core.support.configReader.Config;
import core.support.configReader.PropertiesReader;
import core.support.logger.TestLog;
import core.support.objects.DeviceManager;
import core.support.objects.DeviceObject.DeviceType;
import core.support.objects.TestObject;
import core.uiCore.AppiumServer;
import core.uiCore.driverProperties.globalProperties.CrossPlatformProperties;
import core.uiCore.drivers.AbstractDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * @author ehsan.matean
 *
 */
public class AndroidCapability {

	public DesiredCapabilities capabilities;
	public static String APP_DIR_PATH = "android.appDir";
	public static String APP_NAME = "android.app";
	public static String CHROME_VERSION = "appium.chromeVersion";
	public static String iS_CHROME_AUTO_MANAGE = "appium.chromeAutoManage";
	public static String CHROME_DRIVER_PATH = "appium.chromeDriverPath";
	public static String ANDROID_ENGINE = "android.capabilties.automationName";
	public static String UIAUTOMATOR2 = "UiAutomator2";
	public static boolean ANDROID_INIT = false; // first time android is setup
	public static String ANDROID_HOME = "android.home";

	private static final String CAPABILITIES_PREFIX = "android.capabilties.";

	public List<String> simulatorList = new ArrayList<String>();
	public static int SYSTEM_PORT = 8200;

	public AndroidCapability() {
		capabilities = new DesiredCapabilities();
	}

	public AndroidCapability withCapability(DesiredCapabilities Capabilities) {
		this.capabilities = Capabilities;
		return this;
	}
	
	/**
	 * device: property name from property file. eg. device1, device2
	 * @param device
	 * @return
	 */
	public AndroidCapability withDevice(String device) {
		this.simulatorList = Config.getValueList(device);
		return this;
	}

	public String getChromeDriverVersion() {
		String value = Config.getValue(CHROME_VERSION);
		return value;
	}

	public boolean isChromeAutoManager() {
		boolean value = Config.getBooleanValue(iS_CHROME_AUTO_MANAGE);
		return value;
	}

	public String chromeDriverPath() {
		String value = Config.getValue(CHROME_DRIVER_PATH);
		return value;
	}

	public DesiredCapabilities getCapability() {
		return capabilities;
	}

	public String getAppPath() {
		String appRootPath = PropertiesReader.getLocalRootPath() + Config.getValue(APP_DIR_PATH);
		File appPath = new File(appRootPath, Config.getValue(APP_NAME));
		
		if(!appPath.exists())
			TestLog.ConsoleLogWarn("app not found at: " + appPath.getAbsolutePath());
		
		return appPath.getAbsolutePath();
	}

	/**
	 * sets android capabilities values are from maven or properties file maven has
	 * higher priority than properties
	 * 
	 * @return
	 */
	public AndroidCapability withAndroidCapability() {
		
		// sets capabilties from properties files
		capabilities = setAndroidCapabilties();
		
		capabilities.setCapability(MobileCapabilityType.APP, getAppPath());

		// mandatory capabilities
		// capabilities.setCapability("session-override", true);

		// set chrome version if value set in properties file
		if (!getChromeDriverVersion().equals("DEFAULT")) {
			WebDriverManager.chromedriver().version(getChromeDriverVersion()).setup();
			String chromePath = WebDriverManager.chromedriver().getBinaryPath();
			capabilities.setCapability("chromedriverExecutable", chromePath);
		}
		
		// sets android home value if not already set in properties
		setAndroidHome();

		// set device using device manager. device manager handles multiple devices in parallel
		setAndroidDevice();
		
		// set port for appium 
		setPort(TestObject.getTestInfo().deviceName);	
		
		// if single signin is set, Then do not reset the app after each test
		setSingleSignIn();
		
        return this;
	}
	
	/**
	 * set capabilties with prefix android.capabilties.
	 * eg. android.capabilties.fullReset="false
	 * iterates through all property values with such prefix And adds them to android desired capabilities
	 * @return 
	 */
	public DesiredCapabilities setAndroidCapabilties() {

		// run only once per test run
		uninstallUiAutomator2();
		
		// get all keys from config 
		Map<String, Object> propertiesMap = TestObject.getTestInfo().config;

		// load config/properties values from entries with "android.capabilties." prefix
		for (Entry<String, Object> entry : propertiesMap.entrySet()) {
			boolean isAndroidCapability = entry.getKey().toString().startsWith(CAPABILITIES_PREFIX);
			if (isAndroidCapability) {
				String fullKey = entry.getKey().toString();
				String key = fullKey.substring(fullKey.lastIndexOf(".") + 1).trim();
				String value = entry.getValue().toString().trim();
				
				capabilities.setCapability(key, value);
			}
		}
		return capabilities;
	}

	/**
	 * set chrome version if value set in properties file to use: set
	 * appiumChromeVersion, Then appiumChromeAutoManage if appiumChromeAutoManage =
	 * true : version will automatically download if appiumChromeAutoManage = false
	 * : set the path to the location
	 */
	public void setChromePath() {

		if (!getChromeDriverVersion().equals("DEFAULT")) {
			String chromePath;
			if (isChromeAutoManager()) {
				WebDriverManager.chromedriver().version(getChromeDriverVersion()).setup();
				chromePath = WebDriverManager.chromedriver().getBinaryPath();
			} else {
				chromePath = PropertiesReader.getLocalRootPath() + chromeDriverPath();
			}
			capabilities.setCapability("chromedriverExecutable", chromePath);
		}
	}

	/**
	 * runs subsequent tests without restarting the app removes the need to sign in
	 * on every test if tests fail, it will restart the app
	 */
	public void setSingleSignIn() {
		if (CrossPlatformProperties.isSingleSignIn()) {
			if (AbstractDriver.isFirstRun()) {
				capabilities.setCapability(MobileCapabilityType.NO_RESET, false);
			} else {
				capabilities.setCapability(MobileCapabilityType.NO_RESET, true);
			}
		}
	}

	/**
	 * if adb device contains emulator, Then its an emulator otherwise, its a
	 * connected real device
	 * 
	 * @param devices
	 * @return
	 */
	public boolean isRealDeviceConnected() {
		List<String> devices = getAndroidDeviceList();
		for (String device : devices) {
			if (!device.contains("emulator")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * gets the name of the first real device connected
	 * 
	 * @param devices
	 * @return
	 */
	public static List<String> getRealDevices(List<String> devices) {
		ArrayList<String> realDeviceList = new ArrayList<String>();
		for (String device : devices) {
			if (!device.contains("emulator")) {
				realDeviceList.add(device);
			}
		}
		return realDeviceList;
	}

	/**
	 * gets the list of android devices including real devices + emulators skips the
	 * first item, as it is not a device
	 * 
	 * @return
	 */
	public List<String> getAndroidDeviceList() {
		String cmd;

		if (!Config.getValue(AppiumServer.ANDROID_HOME).isEmpty())
			cmd = Config.getValue(AppiumServer.ANDROID_HOME) + "/platform-tools/adb devices";
		else
			cmd = "adb devices";

		// get list of device udid
		List<String> results = new ArrayList<String>();
		results = Config.getValueList("android.UDID");

		// if no device is set in properties, attempt to auto detect
		if (results.size() == 0) {

			results = Helper.runShellCommand(cmd);
			if (!results.isEmpty())
				results.remove(0);
		}
		List<String> devices = new ArrayList<String>();
		for (String list : results) {
			String arr[] = list.split("\t", 2);
			String device = arr[0];
			if (!device.isEmpty())
				devices.add(device);
		}
		
		if(results.size() > 0)
			TestLog.ConsoleLogDebug("Android device list: " + Arrays.toString(results.toArray()));

		return devices;
	}
	
	public List<String> getAndroidRealDeviceList() {
		List<String> devices = getAndroidDeviceList();
		return getRealDevices(devices);
	}
	
	

	/**
	 * sets ios device number of devices must be equal or greater than number of
	 * threads for parallel run
	 */
	public void setSimulator() {
		List<String> devices = this.simulatorList;

		if (devices == null || devices.isEmpty())
			Helper.assertFalse("set device first");

		// check if more threads are called than devices under test
		int threads = CrossPlatformProperties.getParallelTests();
		if (threads > devices.size())
			Helper.assertFalse(
					"there are more threads than devices. global.parallelTestCount: " + threads + " devices: " + devices.size());

		// adds all devices
		DeviceManager.loadDevices(devices, DeviceType.Android);
		capabilities.setCapability("avd", DeviceManager.getFirstAvailableDevice(DeviceType.Android));
	}
	
	/**
	 * if device has port assigned, use assigned port
	 * else generate new port number
	 * @param deviceName
	 */
	public synchronized void setPort(String deviceName) {
		
		// if device port is already set
		if(DeviceManager.devices.get(deviceName) != null && (DeviceManager.devices.get(deviceName).devicePort != -1))
			capabilities.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT, DeviceManager.devices.get(deviceName).devicePort);
		else {
			int systemPort = ++SYSTEM_PORT;
			capabilities.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT, systemPort);
			DeviceManager.devices.get(deviceName).withDevicePort(systemPort);
		}
		
		TestLog.ConsoleLog("deviceName " + deviceName + " systemPort: " + DeviceManager.devices.get(deviceName).devicePort);
	}
	
	public static void restartAdb() {
	    Helper.runShellCommand("adb kill-server");
	    Helper.runShellCommand("adb start-server");
	}

	/**
	 * uninstalls the uiautomator2 server
	 * only runs the first time android test is run
	 */
	public static void uninstallUiAutomator2() {
		// runs the first time android test is run
		if(!ANDROID_INIT) {
			ANDROID_INIT = true;
			if(Config.getValue(ANDROID_ENGINE).equals(UIAUTOMATOR2)) {
				TestLog.ConsoleLog("Uninstalling uiautomator2.server");
				TestLog.ConsoleLog("Uninstalling uiautomator2.server.test");
				
				Helper.runShellCommand("adb uninstall io.appium.uiautomator2.server");
				Helper.runShellCommand("adb uninstall io.appium.uiautomator2.server.test");
			}
		}
	}

	/**
	 * sets real device
	 * 
	 */
	public void setRealDevices() {
		List<String> devices = getAndroidRealDeviceList();
		int threads = CrossPlatformProperties.getParallelTests();
		if (threads > devices.size())
			Helper.assertFalse(
					"there are more threads than devices. thread count: " + threads + " devices: " + devices.size());

		// adds all devices
		DeviceManager.loadDevices(devices, DeviceType.Android);
		capabilities.setCapability("udid", DeviceManager.getFirstAvailableDevice(DeviceType.Android));
	}

	/**
	 * sets device by following strategy: if device is connected, selects device if
	 * emulator is not specified with "withDevice1() or with Device2() function,
	 * select device 1 by default from properties else select emulator from
	 * properties specified in panel config page
	 */
	public void setAndroidDevice() {
		if (!PropertiesReader.isUsingCloud()) {
			if (isRealDeviceConnected()) {
				setRealDevices();
			} else {
				setSimulator();
			}
		}
	}
	
	public static void printAndroidHelp(Exception e) {
		String androidError = "It is impossible to create a new session";
		String androidSolution = "*******************************************************************\r\n" + "\r\n"
				+ "\r\n" + "\r\n" + "*******************************************************************\r\n" + "\r\n"
				+ "1. this could be an environment issue. Try the following solutions:\r\n"
				+ "    1. Turn on debugging in properties at resource folder for more info:\r\n"
				+ "        1. appiumLogging = true\r\n" + "    2. set android home environment in properties\r\n"
				+ "        1. androidHome = \"/Users/username/Library/Android/sdk\"\r\n"
				+ "    3. please download appium doctor https://github.com/appium/appium-doctor\r\n"
				+ "        1. download with command: npm install appium-doctor -g\r\n"
				+ "        2. Run: appium-doctor -android\r\n"
				+ "        3. Ensure the environment is setup properly\r\n" + "        4. Restart eclipse\r\n"
				+ "    4. is appium terminal installation correct?\r\n" + "        1. command line: appium\r\n"
				+ "            1. Does it start. If not install: “npm install -g appium”  or “sudo npm install -g appium --unsafe-perm=true --allow-root”\r\n"
				+ "            2. Run against appium terminal\r\n" + "                1. In properties set:\r\n"
				+ "                    1. useExternalAppiumServer = true\r\n"
				+ "                    2. appiumExternalPort = 4723\r\n"
				+ "                2. run test And see if it passes\r\n"
				+ "    5. is simulator working correctly: Run\r\n"
				+ "        1. adb uninstall io.appium.uiautomator2.server\r\n"
				+ "        2. adb uninstall io.appium.uiautomator2.server.test \r\n"
				+ "    6. Try running against appium desktop server\r\n"
				+ "        1. Download And run appium desktop\r\n" + "        2. Start the server\r\n"
				+ "        3. In properties at resource folder, set values\r\n"
				+ "            1. useExternalAppiumServer = true\r\n" 
				+ "            2. appiumExternalPort = 4723\r\n"
				+ "            3. run test\r\n" + "*******************************************************************\r\n"
				+ "\r\n" + "\r\n" + "\r\n" + "*******************************************************************";
		if (e.getMessage().contains(androidError)) {
			System.out.println(androidSolution);
		}
	}
	
	/**
	 * sets the value for android home based on its default location
	 */
	public static void setAndroidHome() {
		String userHome = System.getProperty("user.home");
		String androidHome = Config.getValue(ANDROID_HOME);
		String javaHomePath = "";
		if (androidHome.isEmpty()) {
			if (Helper.isMac()) {
				 javaHomePath = userHome + File.separator + "Library" + File.separator + "Android" + File.separator + "sdk/";
			}
				boolean isAndroidHome = new File(javaHomePath).exists();
				if (isAndroidHome) { 
					Config.putValue(ANDROID_HOME, javaHomePath);	
				}
		}
	}
}