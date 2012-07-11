package pl.mbassara.kolowrotek;
import java.io.PrintWriter;
import java.io.StringWriter;


public class ExceptionsUtilities {
	public static String printStackTraceToString(Throwable ex){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		ex.printStackTrace(pw);
		pw.flush();
		sw.flush();
		return sw.toString();
	}
}
