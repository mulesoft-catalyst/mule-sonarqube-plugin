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

/**
 * Jaxen extension function {@code matches()} for JDOM-based XPath evaluation.
 *
 * <p>This function mirrors the semantics used by legacy rules: it checks whether the input string
 * (or each string in a node set) fully matches the provided regex.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.xpath.XPathProcessor
 */
public class MatchesFunction implements Function {

	/**
	 * Registers this function in Jaxen's global {@link SimpleFunctionContext}.
	 */
	public static void registerSelfInSimpleContext() {
		// see http://jaxen.org/extensions.html
		((SimpleFunctionContext) XPathFunctionContext.getInstance()).registerFunction(null, "matches",
				new MatchesFunction());
	}

	/**
	 * Dispatches evaluation based on argument count.
	 *
	 * @param context current XPath context
	 * @param args function arguments
	 * @return boolean result
	 * @throws FunctionCallException when argument count is invalid
	 */
	@Override
	public Object call(Context context, List args) throws FunctionCallException {

		if (args.size() == 2) {
			return evaluate(args.get(0), args.get(1), context.getNavigator());
		}

		throw new FunctionCallException("matches() requires two arguments.");
	}

	/**
	 * Evaluates whether the provided string (or each string within a node set) matches the regex.
	 *
	 * @param strArg string or node set argument
	 * @param matchArg regex argument
	 * @param nav navigator used to extract string values
	 * @return {@link Boolean#TRUE} when all evaluated strings match; otherwise {@link Boolean#FALSE}
	 */
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
