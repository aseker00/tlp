package aseker00.tlp.proc;

import aseker00.tlp.io.Corpus;

public interface Estimator {
	public String name();
	public Processor processor();
	public void dataSetIs(Corpus data);
	public interface Notifiee {
		public void onDataSet(Corpus data);
	}
	public void notifieeIs(Notifiee key, Notifiee value); 
}
