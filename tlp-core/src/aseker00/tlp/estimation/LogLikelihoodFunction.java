package aseker00.tlp.estimation;

public interface LogLikelihoodFunction {
	public double value();
	public LogLikelihoodFunction additionInc(LogLikelihoodFunction other);
}
