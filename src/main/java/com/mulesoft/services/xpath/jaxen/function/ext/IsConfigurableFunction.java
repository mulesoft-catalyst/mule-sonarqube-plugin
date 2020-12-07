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

public class IsConfigurableFunction implements Function {

	private static final String IS_CONFIGURABLE_REGEX = "^\\$\\{.*\\}$";

	public static void registerSelfInSimpleContext() {
		// see http://jaxen.org/extensions.html
		((SimpleFunctionContext) XPathFunctionContext.getInstance()).registerFunction(null, "is-configurable",
				new IsConfigurableFunction());
	}

	@Override
	public Object call(Context context, List args) throws FunctionCallException {

		if (args.size() == 1) {
			return evaluate(args.get(0), context.getNavigator());
		}

		throw new FunctionCallException("is-configurable() requires two arguments.");
	}

	public static Boolean evaluate(Object strArg, Navigator nav) {
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
				String regexp = StringFunction.evaluate(IS_CONFIGURABLE_REGEX, nav);
				System.out.println("str: " + str);
				System.out.println("regexp: " + regexp);
				if (!str.matches(regexp)) {
					return Boolean.FALSE;
				}
			}
			return Boolean.TRUE;
		} else {
			String str = StringFunction.evaluate(strArg, nav);
			String regexp = StringFunction.evaluate(IS_CONFIGURABLE_REGEX, nav);
			return (str.matches(regexp) ? Boolean.TRUE : Boolean.FALSE);
		}

	}

}
