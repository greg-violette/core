/**
 * Copyright (C) 2012-2016 Thales Services SAS.
 *
 * This file is part of AuthZForce CE.
 *
 * AuthZForce CE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthZForce CE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthZForce CE.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.authzforce.core.pdp.impl.func;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.StatusHelper;
import org.ow2.authzforce.core.pdp.api.expression.Expression;
import org.ow2.authzforce.core.pdp.api.func.BaseFunctionSet;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionCall.EagerMultiPrimitiveTypeEval;
import org.ow2.authzforce.core.pdp.api.func.FunctionSet;
import org.ow2.authzforce.core.pdp.api.func.FunctionSignature;
import org.ow2.authzforce.core.pdp.api.value.AttributeValue;
import org.ow2.authzforce.core.pdp.api.value.Datatype;
import org.ow2.authzforce.core.pdp.api.value.IntegerValue;
import org.ow2.authzforce.core.pdp.api.value.SimpleValue;
import org.ow2.authzforce.core.pdp.api.value.StandardDatatypes;
import org.ow2.authzforce.core.pdp.api.value.StringValue;

/**
 * Implements *-substring functions
 *
 * @param <AV>
 *            parameter type
 * 
 * @version $Id: $
 */
public final class SubstringFunction<AV extends SimpleValue<String>> extends FirstOrderFunction.MultiParameterTyped<StringValue>
{

	/**
	 * Standard identifier for the string-substring function.
	 */
	public static final String NAME_STRING_SUBSTRING = XACML_NS_3_0 + "string-substring";

	/**
	 * Standard identifier for the anyURI-substring function.
	 */
	public static final String NAME_ANYURI_SUBSTRING = XACML_NS_3_0 + "anyURI-substring";

	private static final class Call extends EagerMultiPrimitiveTypeEval<StringValue>
	{

		private final String invalidArgTypesErrorMsg;
		private final String argsOutOfBoundsErrorMessage;
		private final Class<? extends SimpleValue<String>> firstParamClass;

		private Call(FunctionSignature<StringValue> functionSig, Datatype<? extends SimpleValue<String>> param0Type, List<Expression<?>> args, Datatype<?>[] remainingArgTypes)
				throws IllegalArgumentException
		{
			super(functionSig, args, remainingArgTypes);
			this.invalidArgTypesErrorMsg = "Function " + functionId + ": Invalid arg types: expected: " + param0Type + ", " + StandardDatatypes.INTEGER_FACTORY.getDatatype() + ", "
					+ StandardDatatypes.INTEGER_FACTORY.getDatatype() + "; actual: ";
			this.argsOutOfBoundsErrorMessage = "Function " + functionId + ": args out of bounds";
			this.firstParamClass = param0Type.getValueClass();
		}

		@Override
		protected StringValue evaluate(Deque<AttributeValue> args) throws IndeterminateEvaluationException
		{
			final AttributeValue rawArg0 = args.poll();
			final AttributeValue rawArg1 = args.poll();
			final AttributeValue rawArg2 = args.poll();

			final SimpleValue<String> arg0;
			final IntegerValue beginIndex;
			final IntegerValue endIndex;
			try
			{
				arg0 = firstParamClass.cast(rawArg0);
				beginIndex = (IntegerValue) rawArg1;
				endIndex = (IntegerValue) rawArg2;
			} catch (ClassCastException e)
			{
				throw new IndeterminateEvaluationException(invalidArgTypesErrorMsg + rawArg0.getDataType() + "," + rawArg1.getDataType() + "," + rawArg2.getDataType(),
						StatusHelper.STATUS_PROCESSING_ERROR, e);
			}

			/**
			 * string-susbtring(str1, beginIndex, endIndex)
			 * <p>
			 * The result SHALL be the substring of <code>arg0</code> at the position given by <code>beginIndex</code> and ending at <code>endIndex</code>. The first character of <code>arg0</code> has
			 * position zero. The negative integer value -1 given for <code>endIndex</code> indicates the end of the string. If <code>beginIndex</code> or <code>endIndex</code> are out of bounds, then
			 * the function MUST evaluate to Indeterminate with a status code of urn:oasis:names:tc:xacml:1.0:status:processing-error
			 * 
			 * @param arg0
			 *            value from which to extract the substring
			 * @param beginIndex
			 *            position in this string where to begin the substring
			 * @param endIndex
			 *            the position in this string just before which to end the substring
			 * @return the substring
			 * @throws IndeterminateEvaluationException
			 *             if {@code beginIndex} or {@code endIndex} are out of bounds
			 */
			final String substring;
			try
			{
				final int beginIndexInt = beginIndex.intValueExact();
				final int endIndexInt = endIndex.intValueExact();
				substring = endIndexInt == -1 ? arg0.getUnderlyingValue().substring(beginIndexInt) : arg0.getUnderlyingValue().substring(beginIndexInt, endIndexInt);
			} catch (ArithmeticException | IndexOutOfBoundsException e)
			{
				throw new IndeterminateEvaluationException(argsOutOfBoundsErrorMessage, StatusHelper.STATUS_PROCESSING_ERROR, e);
			}

			return new StringValue(substring);
		}
	}

	private final Datatype<AV> param0Type;

	/**
	 * Instantiates function
	 * 
	 * @param functionId
	 *            function ID
	 * @param param0Type
	 *            First parameter type
	 */
	private SubstringFunction(String functionId, Datatype<AV> param0Type)
	{
		super(functionId, StandardDatatypes.STRING_FACTORY.getDatatype(), false, Arrays.asList(param0Type, StandardDatatypes.INTEGER_FACTORY.getDatatype(),
				StandardDatatypes.INTEGER_FACTORY.getDatatype()));
		this.param0Type = param0Type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.thalesgroup.authzforce.core.func.FirstOrderFunction#getFunctionCall(java.util.List, com.thalesgroup.authzforce.core.eval.DatatypeDef[])
	 */
	/** {@inheritDoc} */
	@Override
	public FirstOrderFunctionCall<StringValue> newCall(List<Expression<?>> argExpressions, Datatype<?>... remainingArgTypes)
	{
		return new Call(functionSignature, param0Type, argExpressions, remainingArgTypes);
	}

	/**
	 * Function cluster
	 */
	public static final FunctionSet SET = new BaseFunctionSet(FunctionSet.DEFAULT_ID_NAMESPACE + "substring",
	//
			new SubstringFunction<>(NAME_STRING_SUBSTRING, StandardDatatypes.STRING_FACTORY.getDatatype()),
			//
			new SubstringFunction<>(NAME_ANYURI_SUBSTRING, StandardDatatypes.ANYURI_FACTORY.getDatatype()));

}
