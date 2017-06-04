package mysoot;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.*;
import java.util.*;

/**
 * InvokeStaticInstrumenter inserts count instructions before INVOKESTATIC
 * bytecode in a program. The instrumented program will report how many static
 * invocations happen in a run.
 * 
 * Goal: Insert counter instruction before static invocation instruction. Report
 * counters before program’s normal exit point. Approach: 1. Create a counter
 * class which has a counter field, and a reporting method. 2. Take each method
 * body, go through each instruction, and insert count instructions before
 * INVOKESTATIC. 3. Make a call of reporting method of the counter class.
 * 
 * Things to learn from this example: 1. How to use Soot to examine a Java
 * class. 2. How to insert profiling instructions in a class.
 * InvokeStaticInstrumenter extends the abstract class BodyTransformer, and
 * implements
 * 
 * <pre>
 * internalTransform
 * </pre>
 * 
 * method.
 */
public class InvokeStaticInstrumenter extends BodyTransformer {
	
	/* some internal fields */
	static SootClass counterClass;
	static SootMethod increaseCounter, reportCounter;
	
   //在soot中注册我们的辅助类
	static {
		counterClass = Scene.v().loadClassAndSupport("MyCounter");
		System.out.println(counterClass.getJavaPackageName().toString());
		System.out.println(counterClass.getMethodCount());
		increaseCounter = counterClass.getMethod("void increase(int)");
		reportCounter = counterClass.getMethod("void report()");
	}

	/** internalTransform goes through a method body and inserts
	 * counter instructions before an INVOKESTATIC instruction
	 */
	@Override
	protected void internalTransform(Body body, String arg1, Map arg2) {
		
		 // 得到该方法
		SootMethod method = body.getMethod();
		
		// 调试
		System.out.println("instrumenting method : " + method.getSignature());
		System.out.println("MethodName: " + method.getName());
		
		// 得到该方法的UnitChain
		Chain units = body.getUnits();
		
		
		//当遍历它的时候，改变它的语句链
		Iterator stmtIt = units.snapshotIterator();

		
		 // 遍历每一条语句
		while (stmtIt.hasNext()) {
			
			// 得到statement
			Stmt stmt = (Stmt) stmtIt.next();
			if (!stmt.containsInvokeExpr()) {
				continue;
			}
			// take out the invoke expression
			InvokeExpr expr = (InvokeExpr) stmt.getInvokeExpr();
			
			// 跳过 non-static invocations
			if (!(expr instanceof StaticInvokeExpr)) {
				continue;
			}
			
			// now we reach the real instruction
			// call Chain.insertBefore() to insert instructions
			//插入一条Expr,调用我们构造的辅助类的方法，参数为1（显然）
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(
					increaseCounter.makeRef(), IntConstant.v(1));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			
			// 3. insert new statement into the chain
		    //we are mutating the unit chain)
			
			units.insertBefore(incStmt, stmt);
		}
		
		
		//最后，我们要插入一条语句来输出最后的结果
		
		//判断当前是不是main方法
		String signature = method.getSubSignature();
		boolean isMain = signature.equals("void main(java.lang.String[])");
		if (isMain) {
			stmtIt = units.snapshotIterator();
			while (stmtIt.hasNext()) {
				Stmt stmt = (Stmt) stmtIt.next();
				
				 // check if the instruction is a return with/without value
				if ((stmt instanceof ReturnStmt)
						|| (stmt instanceof ReturnVoidStmt)) {
					
					// 2. then, make a invoke statement
					InvokeExpr reportExpr = Jimple.v().newStaticInvokeExpr(
							reportCounter.makeRef());
					
					// 3. insert new statement into the chain
					Stmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
					units.insertBefore(reportStmt, stmt);
				}
			}
		}

	}



}
