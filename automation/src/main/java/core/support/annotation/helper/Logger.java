package core.support.annotation.helper;

public class Logger {

	// enable to debug
	public static void debug(String value) {
		//if (Config.getBooleanValue("console.annotation.debug"))
			System.out.println(value);
	}
}
