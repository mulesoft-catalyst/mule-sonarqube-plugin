package com.mulesoft.services.xpath.jaxen.function.ext;

import java.util.Iterator;
import java.util.List;

import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.SimpleFunctionContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.StringFunction;

public class MatchesFunction implements Function {

	public static void registerSelfInSimpleContext() {
		// see http://jaxen.org/extensions.html
		((SimpleFunctionContext) XPathFunctionContext.getInstance()).registerFunction(null, "matches",
				new MatchesFunction());
	}

	@Override
	public Object call(Context context, List args) throws FunctionCallException {

		if (args.size() == 2) {
			return evaluate(args.get(0), args.get(1), context.getNavigator());
		}

		throw new FunctionCallException("matchs() requires two arguments.");
	}

	public static Boolean evaluate(Object strArg, Object matchArg, Navigator nav) {
		if (strArg instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> objectList = (List<Object>) strArg;
			// Check if it could load any attribute - If Empty then False
			if (objectList.isEmpty()) {
				return Boolean.FALSE;
			}

			for (Iterator<Object> iterator = objectList.iterator(); iterator.hasNext();) {
				Object object = iterator.next();
				String str = StringFunction.evaluate(object, nav);
				String regexp = StringFunction.evaluate(matchArg, nav);
				if (!str.matches(regexp)) {
					return Boolean.FALSE;
				}
			}
			return Boolean.TRUE;
		} else {
			String str = StringFunction.evaluate(strArg, nav);
			String regexp = StringFunction.evaluate(matchArg, nav);
			return (str.matches(regexp) ? Boolean.TRUE : Boolean.FALSE);
		}

	}

}
