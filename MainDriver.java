package mysoot;
import soot.*;
import soot.options.Options;
/* import necessary soot packages */

public class MainDriver {
	
	/**
	 * 这里放入: TestInvoke.java
	 * @param args
	 */
    public static void main(String[] args) {
    	
    	 /* check the arguments */
        if (args.length == 0){
            System.err.println("Usage: java MainDriver [options] classname");
			  System.err.println("Usage: java MainDriver [options] classname");
            System.exit(0);
        }
        
        /* add a phase to transformer pack by call Pack.add */ 
        Options.v().set_allow_phantom_refs(true);
        
        Pack jtp = PackManager.v().getPack("jtp");
        
        jtp.add(new Transform("jtp.instrumanter", 
                new InvokeStaticInstrumenter()));
        /* Give control to Soot to process all options,
         * InvokeStaticInstrumenter.internalTransform will get called.
         */
        soot.Main.main(args);
    }
}