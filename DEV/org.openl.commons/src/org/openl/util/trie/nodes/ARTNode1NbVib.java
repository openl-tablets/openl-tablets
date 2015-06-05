package org.openl.util.trie.nodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeVi;
import org.openl.util.trie.cnodes.ARTNode1N;
import org.openl.util.trie.cnodes.ARTNode1NViC;
import org.openl.util.trie.cnodes.ARTNode1NVibC;
import org.openl.util.trie.cnodes.ARTNode1NbViC;
import org.openl.util.trie.cnodes.ARTNode1NbVibC;
import org.openl.util.trie.cnodes.ARTNode1Vi;
import org.openl.util.trie.cnodes.ARTNode1NbC;
import org.openl.util.trie.cnodes.ARTNode1VibC;
import org.openl.util.trie.cnodes.CNodeFactory;
import org.openl.util.trie.cnodes.SingleNodeN;
import org.openl.util.trie.cnodes.SingleNodeVi;

public final class ARTNode1NbVib implements IARTNode, IARTNodeVi {
	
	private static final int MAXIMUM_CAPACITY = 255;

	int startN;
	
	IARTNode[] nodes;
	byte[] mapperN;

	private int countN = 0;

	private int minIndexN;

	private int maxIndexN;
	
	
	public ARTNode1NbVib(){}
	
	
	protected void initN(int start, int capacity, int mapCapacity)
	{
		nodes = new IARTNode[capacity];
		mapperN = new byte[mapCapacity];
		this.startN = start;
		minIndexN = start + mapCapacity;
		maxIndexN = start;
	}
	
	
	@Override
	public IARTNode findNode(int index) {
		if (mapperN == null)
			return null;
		
		int idx = index - startN;
		if (idx < 0 || idx >= mapperN.length)
			return null;
		byte b = mapperN[idx];
		if (b == 0)
			return null;
		return  nodes[(255 - b) & 0xff];  
	}


	@Override
	public void setNode(int index, IARTNode node) {
		
		if (mapperN == null)
			initN(index);
		
		int idx = index - startN;
		
		if (idx < 0)
		{	
			growNodeDownN(index);
			setNode(index, node);
			return;
		}	
		
		if (idx >= mapperN.length)
		{	
			growMapperUpN(index);
			setNode(index, node);
			return;
		}	
		
		
		
		if (mapperN[idx] == 0)
		{
			if (countN  >= nodes.length){
				growCapacityN();
				setNode(index, node);
				return;
			}
			
			minIndexN = Math.min(index, minIndexN);
			maxIndexN = Math.max(index, maxIndexN);
			nodes[countN++] = node;
			mapperN[idx] = (byte)(256 - countN);
		}	
		
		else {
			byte b = mapperN[idx];
			nodes[(255 - b) & 0xff] = node;
			}
		
		
		
	}

	private void initN(int index) {
		
		if ('a' <= index && index <= 'z')
		{
			initN('a', 10, 'z' - 'a' + 1);
		}	
		else if ('A' <= index && index <= 'Z')
		{
			initN('A', 10, 'Z' - 'A' + 1);
		}	
		else if ('0' <= index && index <= '9')
		{
			initN('0', 5, '9' - '0' + 1);
		}
		else
		{
			int startX = Math.max(index - 10, 0);
			initN(startX, 10, 40);
		}	
	}


	private void growCapacityN() {
		int oldCapacity = nodes.length;
		int newCapacity = newCapacityN(oldCapacity * 2);
		if (newCapacity <= oldCapacity)
			throw new RuntimeException("Node capacity overflow");
		
		IARTNode[] newNodes = new IARTNode[newCapacity];
		System.arraycopy(nodes, 0, newNodes, 0, oldCapacity);
		nodes = newNodes;
		
	}


	private void growMapperUpN(int index) {
		
		int oldCapacity = mapperN.length;
		
		int newRequiredCapacity = index - minIndexN + 1;
		
		if (newRequiredCapacity <= oldCapacity)
		{
			int margin = (oldCapacity - newRequiredCapacity) / 2;
			int newStart = minIndexN - margin;
			int shift = newStart - startN; 
			int oldOrigin = minIndexN - startN;
			int oldSize = maxIndexN - minIndexN + 1;
			System.arraycopy(mapperN, oldOrigin , mapperN, margin, oldSize);
			int cleanSize = Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapperN[maxIndexN - startN - i] = 0;
			}
			startN = newStart;
			return;
		}	
	
		
		
		
		
		int newCapacity = newCapacityN( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		int margin = (newCapacity - newRequiredCapacity) / 2;
		int newStart = minIndexN - margin;
		int oldMargin = minIndexN - startN;
		int oldSize = maxIndexN - minIndexN + 1;
		System.arraycopy(mapperN, oldMargin , newMapper, margin, oldSize);
		startN = newStart;
		mapperN = newMapper;
		
	}

	private void growNodeDownN(int index) {
		int oldCapacity = mapperN.length;
		
		int newRequiredCapacity = maxIndexN - index + 1;

		int margin = (oldCapacity - newRequiredCapacity) / 2;
		int newStart = index - margin;
		int shift = startN - newStart;
		int oldOrigin = minIndexN - startN;
		int newOrigin = oldOrigin + shift;
		int oldSize = maxIndexN - minIndexN + 1;
		
		
		if (newRequiredCapacity <= oldCapacity)
		{
			System.arraycopy(mapperN, oldOrigin , mapperN, newOrigin, oldSize);
			int cleanSize =  Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapperN[oldOrigin + i] = 0;
			}
			startN = newStart;
			return;
		}	
	
		
		
		
		
		int newCapacity = newCapacityN( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		margin = (newCapacity - newRequiredCapacity) / 2;
		newStart = minIndexN - margin;
		shift = startN - newStart;
		newOrigin = oldOrigin + shift;
		
		System.arraycopy(mapperN, oldOrigin , newMapper, newOrigin, oldSize);
		startN = newStart;
		mapperN = newMapper;
		
	}
	

	protected int newCapacityN(int required)
	{
	
	  if (required < 0)
          throw new IllegalArgumentException("Illegal initial capacity: " +
                                             required);
      if (required > MAXIMUM_CAPACITY)
          required = MAXIMUM_CAPACITY;


      return required;
      
	}


	@Override
	public int countN() {
		return countN ;
	}

	@Override
	public int minIndexN() {
		return minIndexN;
	}

	@Override
	public int maxIndexN() {
		return maxIndexN;
	}
	
	@Override
	public IIntIterator indexIteratorN() {
		return MappedArrayIterator.iterator(startN, mapperN);
	}
	


	
	int startV;
	
	int[] values;
	byte[] mapperV;

	private int countV = 0;

	private int minIndexV;

	private int maxIndexV;
	
	
	
	protected void initV(int start, int capacity, int mapCapacity)
	{
		values = new int[capacity];
		mapperV = new byte[mapCapacity];
		this.startV = start;
		minIndexV = start + mapCapacity;
		maxIndexV = start;
	}
	
	
	@Override
	public Object getValue(int index) {
		if (mapperV == null)
			return null;
		
		int idx = index - startV;
		if (idx < 0 || idx >= mapperV.length)
			return null;
		byte b = mapperV[idx];
		if (b == 0)
			return null;
		return  values[(255 - b) & 0xff];  
	}


	@Override
	public void setValue(int index, Object value) {
		
		if (mapperV == null)
			initV(index);
		
		int idx = index - startV;
		
		if (idx < 0)
		{	
			growNodeDownV(index);
			setValue(index, value);
			return;
		}	
		
		if (idx >= mapperV.length)
		{	
			growMapperUpV(index);
			setValue(index, value);
			return;
			
		}	
		
		
		
		if (mapperV[idx] == 0)
		{
			if (countV  >= values.length){
						growCapacityV();
						setValue(index, value);
						return;
			}
			
			minIndexV = Math.min(index, minIndexV);
			maxIndexV = Math.max(index, maxIndexV);
			values[countV++] = (Integer)value;
			mapperV[idx] = (byte)(256 - countV);
		}	
		
		else {
			byte b = mapperV[idx];
			values[(255 - b) & 0xff] = (Integer)value;
			}
		
		
		return;
		
	}

	private void initV(int index) {
		
		if ('a' <= index && index <= 'z')
		{
			initV('a', 10, 'z' - 'a' + 1);
		}	
		else if ('A' <= index && index <= 'Z')
		{
			initV('A', 10, 'Z' - 'A' + 1);
		}	
		else if ('0' <= index && index <= '9')
		{
			initV('0', 5, '9' - '0' + 1);
		}
		else
		{
			int startX = Math.max(index - 10, 0);
			initV(startX, 10, 40);
		}	
	}


	private void growCapacityV() {
		int oldCapacity = values.length;
		int newCapacity = newCapacityV(oldCapacity * 2);
		if (newCapacity <= oldCapacity)
			throw new RuntimeException("Node capacity overflow");
		
		int[] newValues = new int[newCapacity];
		System.arraycopy(values, 0, newValues, 0, oldCapacity);
		values = newValues;
		
		return;
	}


	private void growMapperUpV(int index) {
		
		int oldCapacity = mapperV.length;
		
		int newRequiredCapacity = index - minIndexV + 1;
		
		if (newRequiredCapacity <= oldCapacity)
		{
			int margin = (oldCapacity - newRequiredCapacity) / 2;
			int newStart = minIndexV - margin;
			int shift = newStart - startV; 
			int oldOrigin = minIndexV - startV;
			int oldSize = maxIndexV - minIndexV + 1;
			System.arraycopy(mapperV, oldOrigin , mapperV, margin, oldSize);
			int cleanSize = Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapperV[maxIndexV - startV - i] = 0;
			}
			startV = newStart;
			return;
		}	
	
		
		
		
		
		int newCapacity = newCapacityV( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		int margin = (newCapacity - newRequiredCapacity) / 2;
		int newStart = minIndexV - margin;
		int oldMargin = minIndexV - startV;
		int oldSize = maxIndexV - minIndexV + 1;
		System.arraycopy(mapperV, oldMargin , newMapper, margin, oldSize);
		startV = newStart;
		mapperV = newMapper;
		
		return;
	}

	private void growNodeDownV(int index) {
		int oldCapacity = mapperV.length;
		
		int newRequiredCapacity = maxIndexV - index + 1;

		int margin = (oldCapacity - newRequiredCapacity) / 2;
		int newStart = index - margin;
		int shift = startV - newStart;
		int oldOrigin = minIndexV - startV;
		int newOrigin = oldOrigin + shift;
		int oldSize = maxIndexV - minIndexV + 1;
		
		
		if (newRequiredCapacity <= oldCapacity)
		{
			System.arraycopy(mapperV, oldOrigin , mapperV, newOrigin, oldSize);
			int cleanSize =  Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapperV[oldOrigin + i] = 0;
			}
			startV = newStart;
			return;
		}	
	
		
		
		
		
		int newCapacity = newCapacityV( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		margin = (newCapacity - newRequiredCapacity) / 2;
		newStart = minIndexV - margin;
		shift = startV - newStart;
		newOrigin = oldOrigin + shift;
		
		System.arraycopy(mapperV, oldOrigin , newMapper, newOrigin, oldSize);
		startV = newStart;
		mapperV = newMapper;
		
		return;
	
	}
	

	public int newCapacityV(int required)
	{
	
	  if (required < 0)
          throw new IllegalArgumentException("Illegal initial capacity: " +
                                             required);
      if (required > MAXIMUM_CAPACITY)
          required = MAXIMUM_CAPACITY;


      return required;
      
	}


	@Override
	public int countV() {
		return countV ;
	}

	@Override
	public int minIndexV() {
		return minIndexV;
	}

	@Override
	public int maxIndexV() {
		return maxIndexV;
	}
	
	
	@Override
	public IIntIterator indexIteratorV() {
		return MappedArrayIterator.iterator(startV, mapperV);
	}
	

// compact	

	
	private IARTNode compactX()
	{
		int rangeN = maxIndexN - minIndexN + 1;
		boolean useMapperN = CNodeFactory.useMapper(rangeN,  byte[].class, countN, Object[].class); 
		int rangeV = maxIndexV - minIndexV + 1;
		boolean useMapperV = CNodeFactory.useMapper(rangeV,  byte[].class, countV, int[].class);
		
		if (!useMapperN)
		{
			IARTNode[] arrayN = makeArrayN(rangeN);
			
			if (!useMapperV)
			{
				int[] arrayV = makeArrayVi(rangeV);
				return new ARTNode1NViC(minIndexN, countN, arrayN, minIndexV, countV, arrayV);
			}
			else
			{
				int[] mappedArrayV = makeMappedArrayVi();
				byte[] newMapperV = makeMapperV(rangeV);
				int newStartV = newMapperV == mapperV ? startV : minIndexV;
				
				return new ARTNode1NVibC(minIndexN, countN, arrayN, newStartV, newMapperV, mappedArrayV); 
			}	
		} else
		{
			IARTNode[] mappedArrayN = makeMappedArrayN(); 
			byte[] newMapperN = makeMapperN(rangeN);
			int newStartN = mapperN == newMapperN ? startN : minIndexN;
			
			if (!useMapperV)
			{
				int[] arrayV = makeArrayVi(rangeV);
				return new ARTNode1NbViC(newStartN, newMapperN, mappedArrayN, minIndexV, countV, arrayV);
			}
			else
			{
				int[] mappedArrayV = makeMappedArrayVi();
				byte[] newMapperV = makeMapperV(rangeV);
				int newStartV = newMapperV == mapperV ? startV : minIndexV;
				
				return new ARTNode1NbVibC(newStartN, newMapperN, mappedArrayN, newStartV, newMapperV, mappedArrayV); 
			}	
			
		}	
		
		
//		return this;
		
	}
	

	public IARTNode compactN() {
		switch (countN) {
			
		case 1:
			return new SingleNodeN(minIndexN, nodes[0].compact());
		}
		
		int range = maxIndexN - minIndexN + 1;
		boolean useMapper = CNodeFactory.useMapper(range,  byte[].class, countN, Object[].class); 
		
		if (useMapper)
			return makeMappedCompactN(range);
		
		
		return makeArrayCompactN(range);
	}
	
	private IARTNode makeArrayCompactN(int range) {
		return new ARTNode1N(minIndexN, countN, makeArrayN(range));
	}
		
	
	final private IARTNode[] makeArrayN(int rangeN) {
		
		IARTNode[] res = new IARTNode[rangeN];
		
		for (int i = minIndexN; i <= maxIndexN; i++) {
			byte b = mapperN[i - startN];
			if (b != 0)
			{
				res[i - minIndexN] = nodes[(255 - b) & 0xff].compact();
			}	
			
		}
		return res;
	}
	

	private IARTNode[] makeMappedArrayN() {
		IARTNode[] newNodes = new IARTNode[countN];
		
		for (int i = 0; i < newNodes.length; i++) {
			newNodes[i] = nodes[i].compact();
		}
		
		return newNodes;
	}

	private byte[] makeMapperN(int rangeN) {
//		int newStart = startN;
		byte[] newMapper = mapperN;	
		if (((rangeN + 7) & 0xf8) < mapperN.length ){
			newMapper = new byte[rangeN];
			System.arraycopy(mapperN, minIndexN - startN, newMapper, 0, rangeN);
//			newStart = minIndexN;
		}
		
		return newMapper;
	}
	
	
	private IARTNode makeMappedCompactN(int range) {
		
		
		byte[] newMapper = makeMapperN(range);	
		int newStartN = newMapper == mapperN ? startN : minIndexN;
		
		IARTNode[] newNodes = makeMappedArrayN();
		
		
		return new ARTNode1NbC(newStartN, newMapper, newNodes);
	}
	
	
	
	public IARTNode compactV() {
		switch (countV) {
			
		case 1:
			return new SingleNodeVi(minIndexV, values[0]);
		}
		
		int range = maxIndexV - minIndexV + 1;
		boolean useMapper = CNodeFactory.useMapper(range,  byte[].class, countV, int[].class); 
		
		if (useMapper)
			return makeMappedCompactV(range);
		
		
		return makeArrayCompactV(range);
	}
	
	private int[] makeArrayVi(int rangeV) {
		int[] res = new int[rangeV];
		
		for (int i = minIndexV; i <= maxIndexV; i++) {
			byte b = mapperV[i - startV];
			if (b != 0)
			{
				res[i - minIndexV] = 0xffffffff -  values[(255 - b) & 0xff];
			}	
			
		}
		return res;
	}
	
	
	private IARTNode makeArrayCompactV(int rangeV) {
		
		int[] res = makeArrayVi(rangeV);
		return new ARTNode1Vi(minIndexV, countV, res);
	}
	
	
	private byte[] makeMapperV(int rangeV)
	{
//		int newStart = startV;
		byte[] newMapper = mapperV;	
		if (((rangeV + 7) & 0xf8) < mapperV.length ){
			newMapper = new byte[rangeV];
			System.arraycopy(mapperV, minIndexV - startV, newMapper, 0, rangeV);
//			newStart = minIndexV;
		}
		
		return newMapper;
		
	}
	
	private int[] makeMappedArrayVi()
	{
		int[] newValues = values;	
		if (((countV + 1) & 0xfe) < values.length)
		{
			newValues = new int[countV];
			System.arraycopy(values, 0, newValues, 0, countV);
		}	
		
		return newValues;
	}
	
	private IARTNode makeMappedCompactV(int range) {
		
		
		byte[] newMapper = makeMapperV(range);	
		int newStart = newMapper == mapperV ? startV : minIndexV;
		
		int[] newValues = makeMappedArrayVi();	
		
		return new ARTNode1VibC(newStart, newMapper, newValues);
	}

	@Override
	public IARTNode compact() {
		if (countV == 0)
			return compactN();
		if (countN == 0)
			return compactV();
//		nodeV = (IARTNodeV) nodeV.compact();
//		nodeN = (IARTNodeN) nodeN.compact();
		return compactX();
	}
	
	
}
