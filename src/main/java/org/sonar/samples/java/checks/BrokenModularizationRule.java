package org.sonar.samples.java.checks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.smell.astmodeler.ClassNode;
import org.smell.astmodeler.MethodNode;
import org.smell.metricruler.Atfd;
import org.smell.metricruler.Fdp;
import org.smell.metricruler.Laa;
import org.smell.metricruler.NopaAndNoam;
import org.smell.metricruler.Wmc;
import org.smell.metricruler.Woc;
import org.smell.rule.pluginregister.MyJavaRulesDefinition;
import org.smell.smellruler.BrokenModularization;
import org.smell.smellruler.DataClass;
import org.smell.smellruler.FeatureEnvy;
import org.smell.smellruler.Smell;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

//TODO
//Not implement yet:
//method body�H�~��ATFD
//��k�ѼƤ���ATFD ex: setValue(B.getValue);
//return statement�������e
// +-*% ���t���B�⤸�����e
// �p��ק�Tag
//Check wmc�����ȹ藍��
//�O���Ҧ�method��LAA
//����api��FE�s�W���t�~�@��rule
//TCSE 2018-Software Smell Ontology Model and Detection Method for Architecture Smells, Design Smells and Code Smells
//Design and Implementation of Software Smell Ontology and Software Smell Detection SonarQube Extension
//Ontology���ѥ���
//�]�p�P��@�n�������ѥ���P�n����������SonarQube�X�R
//�������O����FE���ɭ� reportIssue����m���ӭn�b�������O�W
//�˲M��sonarQUbe WMC���w�q
//
@Rule(key = "S120")

public class BrokenModularizationRule extends BaseTreeVisitor implements Sensor, JavaFileScanner {

	private JavaFileScannerContext context;
	private static List<ClassNode> classes = new ArrayList<ClassNode>();
	private Smell smell = new BrokenModularization();

	// �v�@���X�M�פ����Ҧ�classes
	// ����@��file�����C��class�U�I�s�@��visitClass ���۹�o��file�I�s�@��scanFile

	// �Ĥ@���X�ݬY��class���ɭԥ���class��J�o�ӷǳƶi����R��list(classes)��
	// �b���R�C��file���ɭ��ˬd classes���s�񪺨C��ClassNode�O�_��smell

	// �@���ݭn�ˬd���X�Ӫ��F��O�_��null �Q�@�Ӥ��general���ѨM��k�Ө��N �@����if(XXx!=null)��check
	@Override
	public void visitClass(ClassTree classTree) {
		ClassNode classNode = new ClassNode(classTree);
		File file = context.getFile();
		int classComplexity = context.getComplexityNodes(classTree).size();
		classNode.setWMC(classComplexity);
		classNode.setFile(file);
		classes.add(classNode);
		super.visitClass(classTree);
	}

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("Compute number of files");
		descriptor.onlyOnLanguage("java");
		descriptor.createIssuesForRuleRepositories(MyJavaRulesDefinition.REPOSITORY_KEY);
	}

	// execute��k�|�bscanFile��k���槹��~����
	@Override
	public void execute(SensorContext context) {
		for (ClassNode classNode : classes) {
			if (classNode.haveSmell(smell)) {
				String filePath = "D:\\test\\Broken ModularizationSmell.txt";
				String info = "Broken Modularization detected in : " + classNode.getName() + "\r\n";
				String path = classNode.getFile().getPath();
				FileSystem fs = context.fileSystem();
				// InputFile�O�۹��sonar.sources ���ɮ׸��| Ex: src\chess\ChessBoard.java
				// File�h�O������| Ex: C:\Users\\user\eclipse-workspace\Expert
				// System\src\chess\\ChessBoard.java

				Iterable<InputFile> javaFiles = fs.inputFiles(fs.predicates().hasPath(path));
				for (InputFile javaFile : javaFiles) {
					try {
						logOnFile(filePath, info);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					// reportBroken Modularization
					NewIssue brokenModularizationIssue = context.newIssue().forRule(MyJavaRulesDefinition.RULE_ON_LINE_1)
							// gap is used to estimate the remediation cost to fix the debt
							.gap(2.0);
					int issueStartLine = classNode.getStartLine();
					NewIssueLocation brokenModularizationLocation = brokenModularizationIssue.newLocation().on(javaFile).at(javaFile.selectLine(issueStartLine)).message("Broken Modularization Location!");
					brokenModularizationIssue.at(brokenModularizationLocation);
					List<MethodNode> methods = classNode.getAllMethodNodes();
					if (classNode.getDataClass() != null) {
						int classStartLine = classNode.getStartLine();
						String dataClassMessage = "Data Class detected" + "\r\n" 
											  + "WOC: " + ((Woc)((DataClass)classNode.getDataClass()).getWoc()).getValue() + "\r\n" 
											  + "WMC: " + ((Wmc)((DataClass)classNode.getDataClass()).getWmc()).getValue() + "\r\n" 
											  + "NOPA + NOAM: " + ((NopaAndNoam)((DataClass)classNode.getDataClass()).getNopaAndNoam()).getValue() + "\r\n" 
								;
						NewIssueLocation dataClassLocation = brokenModularizationIssue.newLocation().on(javaFile).at(javaFile.selectLine(classStartLine)).message(dataClassMessage);
						brokenModularizationIssue.addLocation(dataClassLocation);
					}

					for (MethodNode method : methods) {
						if (method.getFeatureEnvy() != null) {
							int methodStartLine = method.getStartLine();
							String featureEnvyMessage = "Feature Envy detected" + "\r\n" 
									  + "ATFD: " + ((Atfd)((FeatureEnvy)method.getFeatureEnvy()).getAtfd()).getMetricValue() + "\r\n" 
									  + "LAA: "  + ((Laa) ((FeatureEnvy)method.getFeatureEnvy()).getLaa()) .getMetricValue() + "\r\n" 
									  + "FDP: "  + ((Fdp) ((FeatureEnvy)method.getFeatureEnvy()).getFdp()) .getMetricValue() + "\r\n" 
						;
							NewIssueLocation featureEnvyLocation = brokenModularizationIssue.newLocation().on(javaFile).at(javaFile.selectLine(methodStartLine)).message(featureEnvyMessage);
							brokenModularizationIssue.addLocation(featureEnvyLocation);
						}
					}
					brokenModularizationIssue.save();
				}
			}
		}
	}

	public static List<ClassNode> getClasses() {
		return classes;
	}

	// �C���y�@���ɮ� �N�|����@��scanFile��k
	@Override
	public void scanFile(JavaFileScannerContext context) {
		this.context = context;
		CompilationUnitTree cut = context.getTree();
		scan(cut);
	}

	public static void logOnFile(String filePath, String issueName) throws ClassNotFoundException {
		String path = filePath;
		File f = new File(path);
		if (!f.exists()) {
			try {
				f.createNewFile();
				writeToFile(f, issueName);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			writeToFile(f, issueName);
		}
	}

	private static void writeToFile(File f, String issueName) {
		try {
			FileWriter fw = null;
			fw = new FileWriter(f, true);
			String log = issueName;
			fw.write(log);
			if (fw != null) {
				fw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}