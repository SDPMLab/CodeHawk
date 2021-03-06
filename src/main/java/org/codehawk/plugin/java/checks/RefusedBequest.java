package org.codehawk.plugin.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehawk.plugin.java.functioningclass.GetClass;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "refused bequest")
/**
 * To use subsctiption visitor, just extend the IssuableSubscriptionVisitor.
 */
public class RefusedBequest extends IssuableSubscriptionVisitor {
	// save two classNames which have Inheritance relationship
	private ArrayList<String> extendList = new ArrayList<>();
	// save classTree in ExtendClassTree
	Map<String, ExtendClassTree> classList = new HashMap<>();
	private int classCount = 0;
	ExtendClassTree extendTree;

	@Override
	public List<Tree.Kind> nodesToVisit() {
		// Register to the kind of nodes you want to be called upon visit.
		return Collections.singletonList(Tree.Kind.CLASS);
	}

	@Override
	public void visitNode(Tree tree) {

		// to count the number of classTrees
		if (classCount == 0) {
			classCount = GetClass.setClassCount(context.getTree().types());
		}

		/*
		 * use ExtendClassTree to save method & members and save className in classList
		 * if classtree has superClass ,save two name of the class in extendList
		 */
		ClassTree ct = (ClassTree) tree;
		if (ct.superClass() != null) {
			extendTree = new ExtendClassTree(ct, ct.superClass().symbolType().name(), checkAMW(ct));
			extendList.add(extendTree.getName());
			extendList.add(extendTree.getExtendClassName());
		} else {
			if (ct.simpleName() != null) {
				extendTree = new ExtendClassTree(ct, checkAMW(ct));
			}
		}
		if (ct.simpleName() != null) {
			classList.put(ct.simpleName().name(), extendTree);
		}

		// save the method & members of classtree into extendTree
		for (Tree t : ct.members()) {
			if (t.is(Tree.Kind.VARIABLE)) {
				VariableTree vt = (VariableTree) t;
				ModifiersTree modifiersOfVT = vt.modifiers();
				List<ModifierKeywordTree> modifiers = modifiersOfVT.modifiers();
				for (ModifierKeywordTree mtkt : modifiers) {
					if (mtkt.modifier().equals(Modifier.PROTECTED)) {
						extendTree.addMember(vt);
					}
				}
			} else if (t.is(Tree.Kind.METHOD)) {
				MethodTree mt = (MethodTree) t;
				extendTree.addMethod(mt);
			}
		}

		if (--classCount == 0 && !extendList.isEmpty()) {
			for (int i = 0; i < extendList.size(); i += 2) {
				if (classList.get(extendList.get(i)) != null && classList.get(extendList.get(i + 1)) != null) {
					if (classList.get(extendList.get(i)).getAMV() && classList.get(extendList.get(i + 1)).getAMV()) {
						if (extendUse(classList.get(extendList.get(i)), classList.get(extendList.get(i + 1)))) {
							addIssue(classList.get(extendList.get(i)).getLine(), "this class refuse bequest");
						}
					}
				}
			}
		}
	}

	public boolean extendUse(ExtendClassTree classT, ExtendClassTree extendT) {
		int bovrthreshold = 0;
		int burthreshold = 0;

		// check MethodUse
		for (String str1 : classT.getMethod()) {
			for (String str2 : extendT.getMethod()) {
				if (str1.equals(str2)) {
					bovrthreshold++;
					break;
				}
			}
		}

		// check MemberUse
		for (VariableTree vt : extendT.getMembers()) {
			if (vt.symbol().usages() != null) {
				List<IdentifierTree> lit = vt.symbol().usages();
				for (IdentifierTree target : lit) {
					Tree target2 = target.parent();
					while (true) {
						if (target2.is(Tree.Kind.CLASS)) {
							if (((ClassTree) target2).simpleName().name().equals(classT.getName())) {
								burthreshold++;
							}
							break;
						}
						target2 = target2.parent();
					}
				}
			}
		}

		if (classT.getMethod().size() > 7 && extendT.getMethod().size() > 7) {
			if (bovrthreshold * 3 < extendT.getMethod().size() && burthreshold * 3 < extendT.getMembers().size()) {
				return true;
			}
		}

		return false;
	}

	public boolean checkAMW(ClassTree ct) {
		int sum = 0;
		int methodNum = 0;
		for (Tree t : ct.members()) {
			if (t.is(Tree.Kind.METHOD)) {
				methodNum++;
				MethodTree mt = (MethodTree) t;
				if (mt.block() != null ) {
					BlockTree bt = mt.block();
					if (bt.body() != null ) {
						List<StatementTree> lst = bt.body();
						for (StatementTree st : lst) {
							sum = cycleCheck(sum, st);
						}
					}
				}
			}
		}

		if (2 * methodNum <= sum) {
			return true;
		}
		return false;
	}

	public int cycleCheck(int num, StatementTree statementTree) {
		switch (statementTree.kind()) {

		case SWITCH_STATEMENT:
			SwitchStatementTree switchStatementTree = (SwitchStatementTree) statementTree;
			List<CaseGroupTree> switchList = switchStatementTree.cases();
			num += switchList.size();
			for (CaseGroupTree caseT : switchList) {
				List<StatementTree> switchSList = caseT.body();
				for (StatementTree st : switchSList) {
					num = cycleCheck(num, st);
				}
			}
			break;

		case IF_STATEMENT:
			StatementTree statementTree2 = ((IfStatementTree) statementTree).thenStatement();
			num += expressionTreeCheck(((IfStatementTree) statementTree).condition(), 1);
			num = cycleCheck(num, statementTree2);
			break;

		case WHILE_STATEMENT:
			StatementTree statementTree6 = ((WhileStatementTree) statementTree).statement();
			num += expressionTreeCheck(((WhileStatementTree) statementTree).condition(), 1);
			num = cycleCheck(num, statementTree6);
			break;

		case FOR_STATEMENT:
			num += 10;
			StatementTree statementTree7 = ((ForStatementTree) statementTree).statement();
			num = cycleCheck(num, statementTree7);
			break;

		case DO_STATEMENT:
			StatementTree statementTree8 = ((DoWhileStatementTree) statementTree).statement();
			num += expressionTreeCheck(((DoWhileStatementTree) statementTree).condition(), 1);
			num = cycleCheck(num, statementTree8);
			break;

		case FOR_EACH_STATEMENT:
			num++;
			StatementTree statementTree9 = ((ForEachStatement) statementTree).statement();
			num = cycleCheck(num, statementTree9);
			break;

		case TRY_STATEMENT:
			num++;
			BlockTree blockTree2 = ((TryStatementTree) statementTree).block();
			List<StatementTree> list = blockTree2.body();
			for (StatementTree st : list) {
				num = cycleCheck(num, st);
			}
			break;

		default:
			break;
		}

		return num;
	}

	public int expressionTreeCheck(ExpressionTree expt, int conditionNum) {
		if (expt.is(Tree.Kind.STRING_LITERAL) || expt.is(Tree.Kind.NULL_LITERAL) || expt.is(Tree.Kind.INT_LITERAL)
				|| expt.is(Tree.Kind.BOOLEAN_LITERAL)) {
			conditionNum++;
		} else if (expt.is(Tree.Kind.CONDITIONAL_AND) || expt.is(Tree.Kind.CONDITIONAL_OR)) {
			conditionNum++;
			conditionNum = expressionTreeCheck(((BinaryExpressionTree) expt).leftOperand(), conditionNum);
			conditionNum = expressionTreeCheck(((BinaryExpressionTree) expt).rightOperand(), conditionNum);
		}
		return conditionNum;
	}

}

//the tree to save the data in classtree
class ExtendClassTree {
	private String className;
	private String extendClassName;
	private boolean classAMV;
	private int startingLine;
	private ArrayList<VariableTree> classMembers = new ArrayList<>();
	private ArrayList<String> classMethod = new ArrayList<>();

	ExtendClassTree(ClassTree tree, String extendName, boolean amv) {
		className = tree.simpleName().name();
		startingLine = tree.openBraceToken().line();
		extendClassName = extendName;
		classAMV = amv;
	}

	ExtendClassTree(ClassTree tree, boolean amv) {
		className = tree.simpleName().name();
		startingLine = tree.openBraceToken().line();
		classAMV = amv;
	}

	public int getLine() {
		return startingLine;
	}

	public String getName() {
		return className;
	}

	public String getExtendClassName() {
		return extendClassName;
	}

	public boolean getAMV() {
		return classAMV;
	}

	public void addMember(VariableTree vt) {
		classMembers.add(vt);
	}

	public List<VariableTree> getMembers() {
		return classMembers;
	}

	public void addMethod(MethodTree mt) {
		classMethod.add(mt.simpleName().name());
	}

	public List<String> getMethod() {
		return classMethod;
	}

}
