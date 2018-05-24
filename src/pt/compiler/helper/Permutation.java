package pt.compiler.helper;

import java.util.Iterator;
import java.util.List;

public class Permutation implements Iterable<String[]> {

	private String[] options;
	private int length;
	private int counter;
	
	public Permutation(String[] options, int length) {
		setOptions(options);
		setLength(length);
	}
	
	private int getMaxPermutations() {
		return (int)Math.pow(options.length, length);
	}
	
	public void setOptions(String[] options) {
		this.options = options;
		reset();
	}
	
	public void setLength(int length) {
		this.length = length;
		reset();
	}
	
	public void reset() {
		counter = 0;
	}
	
	public Iterator<String[]> iterator() {
		return dummyIterator;
	}
	
	private Iterator<String[]> dummyIterator = new Iterator<String[]>() {

		public boolean hasNext() {
			return counter < getMaxPermutations();
		}

		public String[] next() {
			// compute permutation by counting in the base equal to the number of options
			String[] permute = new String[length];
			int radix = options.length;
			for (int i = 0; i < length; i++) {
				int index = counter / (int)Math.pow(radix, i) % radix;
				permute[i] = options[index];
			}
			
			// increment counter for next call
			counter += 1;
			return permute;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	};
}
