package ca.mcgill.hs.plugin;

public class ScreenOutput implements OutputPlugin{
	
	private String printer = "";

	/**
	 * @override
	 */
	public void closePlugin() {
		return;
	}

	/**
	 * @override
	 */
	public void receiveByte(byte data) {
		char c = (char) data;
		if (Character.isLetter(c) || c == '.' || (char) data == ' ' || (char) data == '-'){
			printer = printer.concat(Character.toString(c));
		} else if (printer.length() > 1){
			System.out.println(printer);
			printer = "";
		} else {
			printer = "";
		}
	}

}
