package org.openl.util.trie;


public class CharSequenceKey implements ISequentialKey
{
	
	public static final KeyRange UTF8RangeKey = new KeyRange() {
		
		@Override
		public int initialMin() {
			return '0';
		}
		
		@Override
		public int initialMax() {
			return 'Z';
		}
		
		@Override
		public int absMin() {
			return '\u0000';
		}
		
		@Override
		public int absMax() {
			return '\u00FF';
		}
	};
	
	CharSequence cseq;

	private KeyRange range;

	public CharSequenceKey(CharSequence cseq, KeyRange range) {
		this.cseq = cseq;
		this.range = range;
	}
	
	public CharSequenceKey(CharSequence cseq) {
		this(cseq, UTF8RangeKey);
	}


	@Override
	public int length() {
		return cseq.length();
	}

	@Override
	public int keyAt(int position) {
		return cseq.charAt(position);
	}
	

	@Override
	public KeyRange keyRange(int position) {
		return range;
	}

}
