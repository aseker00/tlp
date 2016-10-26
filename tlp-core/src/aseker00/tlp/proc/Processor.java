package aseker00.tlp.proc;

import aseker00.tlp.io.Corpus;

public interface Processor {
	public String name();
	public void modelIs(Model model);
	public Model model();
	public void decoderIs(Decoder decoder);
	public void estimatorIs(Corpus data, Estimator estimator);
	public Estimator estimator(Corpus data);
	public void evaluatorIs(Corpus data, Evaluator evaluator);
	public Evaluator evaluator(Corpus data);
	
	// Notifications
	public interface Notifiee {
		public void onModel();
	}
	public void notifieeIs(Notifiee key, Notifiee value);
}