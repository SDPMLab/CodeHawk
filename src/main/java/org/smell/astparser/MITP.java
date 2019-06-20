package org.smell.astparser;

import org.smell.astmodeler.ClassNode;
import org.smell.metricruler.Laa;
import org.smell.metricruler.LaaData;
import org.smell.metricruler.Metric;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

/*
 *  Method Invocation Tree Processor 
 *  �B�zmethod invocation tree
 */
public class MITP extends MethodParser{
	private ClassNode ownerClass;
	
	public MITP(ClassNode node) {
		this.ownerClass = node;
	}
	
	private void callMSETParser(Metric metric , ExpressionTree expressionTree) {			
		ASTTreeParser msetp = new MSETP(this.ownerClass);
		msetp.parse(metric,expressionTree);
	}

	@Override
	public void parse(Metric metric , ExpressionTree expressionTree) {
		// �ݭn���ѥ��T��binary�ɤ~����X������ExpressionTree
		// �p�Gbinary�ɤ����T�ӥBExpressionTree����type�S�O�ۤv�w�q�����O���� ���X�ӴN���|�O!unknown!
		// ��L����API�h�i�H���X������type		
		if (isMethodInvocationTree(expressionTree) && metric.is( Metric.Type.ATFD) ) {		
			if (isMemberSelectExpressionTree(getMethodSelect(expressionTree))) {				
				callMSETParser(metric,expressionTree);		
			}
		}else if(isMethodInvocationTree(expressionTree) && metric.is(Metric.Type.LAA) ) {
			if (isMemberSelectExpressionTree(getMethodSelect(expressionTree))) {				
				callMSETParser(metric,expressionTree);
			}else if(notNull(getMethodSelect(expressionTree)) ) {
				//getXXX();	
				//�i�H�qsymbol��owner�P�_�O�_��local attribute accessed
				//���|�X�{atfd������ �u�ݭn�P�_�O�_��local attribute accessed
				detectLAA(metric,expressionTree);			
			}
		}else if(isMethodInvocationTree(expressionTree) && metric.is(Metric.Type.FDP)) {					
			ExpressionTree methodInvocation = ((MethodInvocationTree) expressionTree).methodSelect();			
			if (isMemberSelectExpressionTree(methodInvocation)) {				
				callMSETParser(metric,expressionTree);
			}					
		}	
	}
	
	private boolean isMethodInvocationTree(ExpressionTree expressionTree) {
		return notNull(expressionTree) && expressionTree.is(Tree.Kind.METHOD_INVOCATION);
	}

	private boolean isMemberSelectExpressionTree(ExpressionTree expressionTree) {
		return notNull(expressionTree) && expressionTree.is(Tree.Kind.MEMBER_SELECT);
	}
	
	private void detectLAA(Metric metric, ExpressionTree expressionTree) {
		ExpressionTree methodInvocation = ((MethodInvocationTree) expressionTree).methodSelect();
		Symbol symbol = ((MethodInvocationTree) expressionTree).symbol();
		String owner = symbol.owner().toString();
		String methodName = methodInvocation.toString();
		if(owner.equals(ownerClass.getName())  && methodNameBeginWithGetOrSet(methodName)) {
			LaaData laaData =   (LaaData)((Laa)metric).getData();
			int localAttributesAccessed = laaData.getLocalAttributesAccessed() + 1;
			laaData.setLocalAttributesAccessed(localAttributesAccessed);
			//local data accessed
		}			
	}	
}