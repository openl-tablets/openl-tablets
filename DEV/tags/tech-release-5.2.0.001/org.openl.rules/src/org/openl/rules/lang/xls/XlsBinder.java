/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openl.IOpenBinder;
import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundError;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.binding.impl.BoundCode;
import org.openl.binding.impl.BoundError;
import org.openl.binding.impl.TooManyErrorsError;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleNode;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLBuilderImpl;
import org.openl.meta.IVocabulary;
import org.openl.rules.calc.SSheetNodeBinder;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.dt.binding.DTNodeBinder;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.binding.MethodTableNodeBinder;
import org.openl.rules.structure.StructureTableNodeBinder;
import org.openl.rules.testmethod.binding.TestMethodNodeBinder;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.SyntaxErrorException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsBinder implements IOpenBinder, ITableNodeTypes
{

	IUserContext ucxt;

	/**
	 * 
	 */
	public XlsBinder(IUserContext ucxt)
	{
		this.ucxt = ucxt;
	}

	public ICastFactory getCastFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public INameSpacedMethodFactory getMethodFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public INodeBinderFactory getNodeBinderFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public INameSpacedTypeFactory getTypeFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public INameSpacedVarFactory getVarFactory()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public IBoundCode bind(IParsedCode parsedCode)
	{
		return bind(parsedCode, null);
	}

	String getOpenLName(OpenlSyntaxNode osn)
	{
		return osn == null ? "org.openl.rules.java" : osn.getOpenlName();
	}

	OpenL makeOpenL(XlsModuleSyntaxNode moduleNode)
	{
		String openlName = getOpenLName(moduleNode.getOpenlNode());
		String allImports = moduleNode.getAllImportString();

		if (allImports == null)
			return OpenL.getInstance(openlName, ucxt);

		OpenLBuilderImpl builder = new OpenLBuilderImpl();

		builder.setExtendsCategory(openlName);

		String category = openlName + "::" + moduleNode.getModule().getUri(0);
		builder.setCategory(category);
		builder.setImports(allImports);
		builder.setConfigurableResourceContext(null, ucxt);
		builder.setInheritExtendedConfigurationLoader(true);

		return OpenL.getInstance(category, ucxt, builder);

	}

	public IVocabulary makeVocabulary(XlsModuleSyntaxNode moduleNode) throws BoundError
	{
		final IdentifierNode vocNode = moduleNode.getVocabularyNode(); 
		if (vocNode == null)
			return null;

		final ClassLoader cl = ucxt.getUserClassLoader();
		IVocabulary ivoc = null;

		try
		{
			Thread.currentThread().setContextClassLoader(cl);
			
			ivoc = (IVocabulary)ucxt.execute(new IUserContext.Executable(){

				public Object execute() {
					IVocabulary voc;
					try {
						Class<?> vClass = cl.loadClass(vocNode
								.getIdentifier());
						voc = (IVocabulary) vClass.newInstance();
						return voc;
					} catch (Throwable t) {
						throw RuntimeExceptionWrapper.wrap(t);
					}
				}
				
			});
			
			return ivoc;
		} catch (Throwable t)
		{
			throw new BoundError(vocNode, "Can't Load Vocabulary", t);
		}

	}

	public IBoundCode bind(IParsedCode parsedCode,
			IBindingContextDelegator delegator)
	{
		XlsModuleSyntaxNode moduleNode = (XlsModuleSyntaxNode) parsedCode
				.getTopNode();

		OpenL openl = null;
		try
		{
			openl = makeOpenL(moduleNode);
		} catch (OpenConfigurationException e)
		{
			return new BoundCode(parsedCode, null,
					new IBoundError[] { new BoundError(moduleNode.getOpenlNode(),
							"Error Creating OpenL", e) }, 0);
		}

		IOpenBinder openlBinder = openl.getBinder();

		IBindingContext cxt = openlBinder.makeBindingContext();
		if (delegator != null)
		{
			delegator.setTopDelegate(cxt);
			cxt = delegator;
		}

		IBoundNode topNode = null;
		try
		{
			topNode = bind(moduleNode, openl, cxt);
		} catch (TooManyErrorsError err)
		{
			Log.error("Too many errors");
		}

		return new BoundCode(parsedCode, topNode, cxt.getError(), 0);
	}

	static public String getModuleName(XlsModuleSyntaxNode node)
	{
		String uri = node.getModule().getUri(0);

		try
		{
			URL url = new URL(uri);
			String file = url.getFile();
			int index = file.lastIndexOf('/');

			file = index < 0 ? file : file.substring(index + 1);

			index = file.lastIndexOf('.');

			if (index > 0)
				file = file.substring(0, index);

			return StringTool.makeJavaIdentifier(file);

		} catch (MalformedURLException e)
		{
			Log.error("Error URI to name conversion", e);
			return "UndefinedXlsType";
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
	 */
	public IBoundNode bind(XlsModuleSyntaxNode moduleNode, OpenL openl,
			IBindingContext cxt)
	{
		// TODO fix schema, name
		XlsModuleOpenClass module = new XlsModuleOpenClass(null,
				getModuleName(moduleNode), new XlsMetaInfo(moduleNode), openl);

		// int nchildren = moduleNode.getNumberOfChildren();

		// IMemberBoundNode[] children = new IMemberBoundNode[nchildren];
		ModuleBindingContext moduleContext = new ModuleBindingContext(cxt, module);
		IVocabulary vocabulary = null;
		try
		{
			vocabulary = makeVocabulary(moduleNode);
		}
		catch(BoundError b)
		{
			cxt.addError(b);
		}
		
		if (vocabulary != null)
		{
			IOpenClass[] types = null;
			try{
				types = vocabulary.getVocabularyTypes();
			} catch (BoundError e)
			{
				cxt.addError(e);
			}
			if (types != null)
				for (int i = 0; i < types.length; i++)
				{
					try
					{
						moduleContext.addType(ISyntaxConstants.THIS_NAMESPACE, types[i]);
					} catch (Throwable t)
					{
						IBoundError error = new BoundError(t, null);
						cxt.addError(error);
					}
				}
		}

		ASelector<ISyntaxNode> dataTypeSelector = new ASelector.StringValueSelector<ISyntaxNode>(
				ITableNodeTypes.XLS_DATATYPE, new SyntaxConvertor());

		// ASelector testMethodSelector = new ASelector.StringValueSelector(
		// ITableNodeTypes.XLS_TEST_METHOD, new SyntaxConvertor());

		bindInternal(moduleNode, openl, moduleContext, module, dataTypeSelector,
				null);

		return bindInternal(moduleNode, openl, moduleContext, module,
				dataTypeSelector.not(), tableComparator);

		// return bindInternal(moduleNode, openl, moduleContext, module,
		// testMethodSelector);
	}

	static Comparator<TableSyntaxNode> tableComparator = new Comparator<TableSyntaxNode>()
	{

		public int compare(TableSyntaxNode ts1, TableSyntaxNode ts2)
		{
			String s1 = ts1.getType();

			String s2 = ts2.getType();

			int i1 = ITableNodeTypes.XLS_TEST_METHOD.equals(s1)
					|| ITableNodeTypes.XLS_RUN_METHOD.equals(s1) ? 1 : 0;
			int i2 = ITableNodeTypes.XLS_TEST_METHOD.equals(s2)
					|| ITableNodeTypes.XLS_RUN_METHOD.equals(s2) ? 1 : 0;

			return i1 - i2;
		}

	};

	static class SyntaxConvertor extends AStringConvertor<ISyntaxNode>
	{

		public String getStringValue(ISyntaxNode test)
		{
			return  test.getType();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenBinder#bind(org.openl.syntax.IParsedCode)
	 */
	protected IBoundNode bindInternal(ISyntaxNode moduleNode, OpenL openl,
			ModuleBindingContext moduleContext, XlsModuleOpenClass module,
			ISelector<ISyntaxNode> childSelector, Comparator<TableSyntaxNode> cmp)
	{

		int nchildren = moduleNode.getNumberOfChildren();
		ArrayList<ISyntaxNode> list = new ArrayList<ISyntaxNode>(nchildren);

		for (int i = 0; i < nchildren; i++)
		{
			ISyntaxNode childNode = moduleNode.getChild(i);
			if (childSelector == null || childSelector.select(childNode))
				list.add(childNode);
		}

		TableSyntaxNode[] chNodes = list
				.toArray(new TableSyntaxNode[0]);

		if (cmp != null)
			Arrays.sort(chNodes, cmp);

		IMemberBoundNode[] children = new IMemberBoundNode[chNodes.length];

		for (int i = 0; i < chNodes.length; i++)
		{

			try
			{

				// ISyntaxNode childNode = moduleNode.getChild(i);
				// if (childSelector.select(childNode))
				children[i] = preBindXlsNode(chNodes[i], openl, moduleContext, module);

				if (children[i] != null)
					children[i].addTo(module);
			} catch (BoundError err)
			{
				moduleContext.addError(err);
				err.setTopLevelSyntaxNode(chNodes[i]);
				chNodes[i].addError(err);
			} catch (SyntaxErrorException se)
			{
				ISyntaxError[] ee = se.getSyntaxErrors();

				for (int j = 0; j < ee.length; j++)
				{
					// ee[j].setTopLevelSyntaxNode(chNodes[i]);
					chNodes[i].addError(ee[j]);
					moduleContext.addError(ee[j]);
				}

			}

			catch (Throwable e)
			{
				BoundError be = new BoundError(chNodes[i], null, e);
				chNodes[i].addError(be);
				moduleContext.addError(be);
			}
		}

		// if (moduleContext.getNumberOfErrors() == 0)
		for (int i = 0; i < children.length; i++)
		{
			if (children[i] != null)
			{
				try
				{
					children[i].finalizeBind(moduleContext);
				} catch (BoundError be)
				{
					be.setTopLevelSyntaxNode(chNodes[i]);
					chNodes[i].addError(be);
					moduleContext.addError(be);
				} catch (SyntaxErrorException se)
				{
					ISyntaxError[] ee = se.getSyntaxErrors();

					for (int j = 0; j < ee.length; j++)
					{
						ee[j].setTopLevelSyntaxNode(chNodes[i]);
						chNodes[i].addError(ee[j]);
						moduleContext.addError(ee[j]);
					}

				} catch (Throwable e)
				{
					BoundError be = new BoundError(chNodes[i], null, e);
					chNodes[i].addError(be);
					moduleContext.addError(be);
				}
			}
		}

		return new ModuleNode(moduleNode, moduleContext.getModule());

	}

	IMemberBoundNode preBindXlsNode(ISyntaxNode syntaxNode, OpenL openl,
			IBindingContext cxt, XlsModuleOpenClass module) throws Exception
	{
		String type = syntaxNode.getType();

		AXlsTableBinder binder = (AXlsTableBinder) binderFactory().get(type);

		if (binder == null)
			// throw new Exception("Unknown Table Type: " + type);
			return null;
		TableSyntaxNode tsn = (TableSyntaxNode) syntaxNode;

		TableProperties tp = binder.loadProperties(tsn.getTable());
		tsn.setTableProperties(tp);

		return binder.preBind(tsn, openl, cxt, module);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.IOpenBinder#makeBindingContext()
	 */
	public IBindingContext makeBindingContext()
	{
		throw new UnsupportedOperationException("XlsBinder is top level Binder");
	}

	static Map<String, AXlsTableBinder> binderFactory;

	static synchronized public Map<String, AXlsTableBinder> binderFactory()
	{
		if (binderFactory == null)
		{
			binderFactory = new HashMap<String, AXlsTableBinder>();

			for (int i = 0; i < binders.length; i++)
			{
				try
				{
					binderFactory.put(binders[i][0], (AXlsTableBinder)Class.forName(binders[i][1])
							.newInstance());
				} catch (Exception ex)
				{
					throw RuntimeExceptionWrapper.wrap(ex);
				}
			}
		}
		return binderFactory;
	}

	static final String[][] binders = {
			{ XLS_DATA, DataNodeBinder.class.getName() },
			{ XLS_DATATYPE, DatatypeNodeBinder.class.getName() },
			{ XLS_DT, DTNodeBinder.class.getName() },
			{ XLS_SPREADSHEET, SSheetNodeBinder.class.getName() },
			{ XLS_METHOD, MethodTableNodeBinder.class.getName() },
			{ XLS_TEST_METHOD, TestMethodNodeBinder.class.getName() },
			{ XLS_RUN_METHOD, TestMethodNodeBinder.class.getName() },
			{ XLS_TABLE, StructureTableNodeBinder.class.getName() }, };

}
