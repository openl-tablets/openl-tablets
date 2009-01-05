package org.openl.rules.calc;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.syntax.impl.SubTextSourceCodeModule;
import org.openl.types.IOpenMethodHeader;

public class CellLoader {

	// IOpenClass paramType;
	// String paramName;
	IBindingContext cxt;
	IOpenMethodHeader header;
	IString2DataConvertor conv;

	public CellLoader(IBindingContext cxt, IOpenMethodHeader header,
			IString2DataConvertor conv) {
		super();
		this.cxt = cxt;
		this.header = header;
		this.conv = conv;
	}

	public Object loadSingleParam(IOpenSourceCodeModule srcModule,
			IMetaInfo meta) throws BoundError {
		String src = srcModule.getCode();

		if (src == null || (src = src.trim()).length() == 0)
			return null;

		if (cxt != null) {
			if (isFormula(src)) {

				int end = 0;
				if (src.startsWith("{"))
					end = -1;

				IOpenSourceCodeModule srcCode = new SubTextSourceCodeModule(
						srcModule, 1, end);

				return OpenlTool.makeMethod(srcCode, cxt.getOpenL(), header,
						cxt);
			}
		}

		try {
			Object res = conv.parse(src, null, cxt);
			if (res instanceof IMetaHolder) {
				((IMetaHolder) res).setMetaInfo(meta);
			}

			// setCellMetaInfo(cell, paramName, paramType);
			// validateValue(res, paramType);
			return res;
		} catch (Throwable t) {
			throw new BoundError(null, null, t, srcModule);
		}
	}

	static public boolean isFormula(String src) {
		if (src.startsWith("{") && src.endsWith("}"))
			return true;

		if (src.startsWith("=")
				&& (src.length() > 2 || src.length() == 2
						&& Character.isLetterOrDigit(src.charAt(1))))
			return true;
		return false;

	}

}
