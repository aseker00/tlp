package aseker00.tlp.proc;

import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;

public class EstimationThread extends Thread {
	LinkedBlockingQueue<Corpus> queue;
	Processor processor;
	
	public EstimationThread(Processor processor) {
		this.queue = new LinkedBlockingQueue<Corpus>();
		this.processor = processor;
	}
	@Override
	public void start() {
		super.start();
	}
	@Override
	public void run() {
		while (true) {
			Corpus data = null;
			try {
				data = this.queue.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (data instanceof NullCorpus)
				break;
			Estimator estimator = this.processor.estimator(data);
			estimator.dataSetIs(data);
		}
	}
	
	public void dataSetIs(Corpus data) {
		if (data == null) {
			this.queue.offer(new NullCorpus());
			return;
		}
		this.queue.offer(data);
	}
	
	class NullCorpus implements Corpus {
		@Override public String name() { return null; }
		@Override public int size() { return 0; }
		@Override public CorpusReader readerNew(String name) { return null; }
	}
}