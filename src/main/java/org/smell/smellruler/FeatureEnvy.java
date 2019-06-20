package org.smell.smellruler;

import java.util.List;
import org.smell.astmodeler.ClassNode;
import org.smell.astmodeler.MethodNode;
import org.smell.astmodeler.Node;
import org.smell.metricruler.Atfd;
import org.smell.metricruler.Fdp;
import org.smell.metricruler.Laa;
import org.smell.metricruler.Metric;
import org.sonar.samples.java.checks.BrokenModularizationRule;

public class FeatureEnvy implements Smell {
	private Metric laa;
	private Metric atfd;
	private Metric fdp;

	public FeatureEnvy() {
		initializeMetrics();
	}

	private void initializeMetrics() {
		this.atfd = new Atfd();
		this.laa = new Laa();
		this.fdp = new Fdp();
	}

	@Override
	public boolean detected(Node node) {
		return haveFeatureEnvySmell(node);
	}

	private void calculateATFDMetric(Node node,MethodNode methodNode) {
		((Atfd) this.atfd).calculateMetric(node,methodNode);
	}
	
	//�C��class node���ӭn���U�۪�smell value
	private boolean haveFeatureEnvySmell(Node node) {
		
		List<MethodNode> methods = ((ClassNode)node).getAllMethodNodes();					
		for(MethodNode methodNode: methods) {
			//clear metrics for every methods
			initializeMetrics();
			//((Atfd) this.atfd).setValue(calculateATFDofClass(node));
			//((Atfd) this.atfd).calculateMetric(node);
			calculateATFDMetric(node,methodNode);
			
			// laa�Omethod level��metrics�o��D�X�̧C��laa�Ӱ���class���N��
			((Laa) this.laa).calculateMetric(node,methodNode);				
			((Fdp) this.fdp).calculateMetric(node,methodNode);		
			String feResult = "D:\\test\\FESmell.txt";
			if (((Atfd) atfd).greaterThanFew() && ((Laa) laa).lessThanThreshold() && ((Fdp) fdp).lessThanFew() ) {
				String information = "Feature Envy detected in : " + node.getName() + "\r\n";	
				logInformation(feResult, information);				
				methodNode.setFeatureEnvy(this);
				return true;
				// this class node have feature envy
			}
		}							
		return false;	
	}
	
	private void logInformation(String logPath, String log) {
		try {
			BrokenModularizationRule.logOnFile(logPath, log);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public Metric getLaa() {
		return laa;
	}

	public void setLaa(Metric laa) {
		this.laa = laa;
	}

	public Metric getAtfd() {
		return atfd;
	}

	public void setAtfd(Metric atfd) {
		this.atfd = atfd;
	}

	public Metric getFdp() {
		return fdp;
	}

	public void setFdp(Metric fdp) {
		this.fdp = fdp;
	}
}