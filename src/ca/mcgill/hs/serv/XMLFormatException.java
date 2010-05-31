package ca.mcgill.hs.serv;

public class XMLFormatException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public XMLFormatException(){
		super("Wrong format used in the XML file.");
	}

}
