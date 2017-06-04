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
 * counters before program��s normal exit point. Approach: 1. Create a counter
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
	
   //��soot��ע�����ǵĸ�����
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
		
		 // �õ��÷���
		SootMethod method = body.getMethod();
		
		// ����
		System.out.println("instrumenting method : " + method.getSignature());
		System.out.println("MethodName: " + method.getName());
		
		// �õ��÷�����UnitChain
		Chain units = body.getUnits();
		
		
		//����������ʱ�򣬸ı����������
		Iterator stmtIt = units.snapshotIterator();

		
		 // ����ÿһ�����
		while (stmtIt.hasNext()) {
			
			// �õ�statement
			Stmt stmt = (Stmt) stmtIt.next();
			if (!stmt.containsInvokeExpr()) {
				continue;
			}
			// take out the invoke expression
			InvokeExpr expr = (InvokeExpr) stmt.getInvokeExpr();
			
			// ���� non-static invocations
			if (!(expr instanceof StaticInvokeExpr)) {
				continue;
			}
			
			// now we reach the real instruction
			// call Chain.insertBefore() to insert instructions
			//����һ��Expr,�������ǹ���ĸ�����ķ���������Ϊ1����Ȼ��
			InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(
					increaseCounter.makeRef(), IntConstant.v(1));
			Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
			
			// 3. insert new statement into the chain
		    //we are mutating the unit chain)
			
			units.insertBefore(incStmt, stmt);
		}
		
		
		//�������Ҫ����һ�������������Ľ��
		
		//�жϵ�ǰ�ǲ���main����
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
