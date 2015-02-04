/**
 * Copyright (C) 2011-2015 Thales Services SAS.
 *
 * This file is part of AuthZForce.
 *
 * AuthZForce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthZForce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthZForce.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sun.xacml.xacmlv3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.DOMHelper;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.MatchResult;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.TargetSection;
import com.sun.xacml.attr.xacmlv3.AttributeDesignator;
import com.sun.xacml.ctx.Status;

public class AllOf extends oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOf
{
	/**
	 * the version of XACML used by the containing Match element (never used actually!)
	 */
	// private int xacmlVersion;

	/**
	 * Constructor that creates a <code>AllOf</code> from components. In
	 * {@link com.sun.xacml.xacmlv3.Target#match(com.sun.xacml.EvaluationCtx)},
	 * {@link oasis.names.tc.xacml._3_0.core.schema.wd_17.Match} objects are cast to {@link Match},
	 * so we need to make sure this is the {@link Match} type that is passed as constructor argument
	 * here to prevent ClassCastException.
	 * 
	 * @param matches
	 *            a <code>List</code> of <code>Match</code> elements
	 * @param version
	 *            XACML version
	 */
	public AllOf(List<Match> matches, int version)
	{
		if (matches == null || matches.isEmpty())
		{
			throw new IllegalArgumentException("<AllOf> empty. Must contain at least one <Match>");
		}

		this.matches = new ArrayList<oasis.names.tc.xacml._3_0.core.schema.wd_17.Match>(matches);
		// this.xacmlVersion = version;
	}

	/**
	 * Creates AllOf in internal model based on AllOf element as defined in OASIS XACML model (JAXB)
	 * 
	 * @param allOfElement
	 * @param metadata
	 * @return AllOf handler
	 * @throws ParsingException
	 *             if invalid AllOf element
	 */
	public static AllOf getInstance(oasis.names.tc.xacml._3_0.core.schema.wd_17.AllOf allOfElement, PolicyMetaData metadata) throws ParsingException
	{
		final List<Match> matchList = new ArrayList<>();
		for (final oasis.names.tc.xacml._3_0.core.schema.wd_17.Match matchElement : allOfElement.getMatches())
		{
			final Match match = Match.getInstance(matchElement, metadata);
			matchList.add(match);
		}

		if (matchList.isEmpty())
		{
			throw new ParsingException("AllOf must contain at least one Match");
		}

		return new AllOf(matchList, PolicyMetaData.XACML_VERSION_3_0);
	}

	/**
	 * creates a new <code>AllOfSelection</code> by parsing DOM node.
	 * 
	 * @param root
	 *            DOM node
	 * @param metaData
	 *            policy meta data
	 * @return <code>AllOfSelection</code>
	 * @throws ParsingException
	 *             throws, if the DOM node is invalid
	 */
	public static AllOf getInstance(Node root, PolicyMetaData metaData) throws ParsingException
	{

		List<Match> matchType = new ArrayList<>();
		NodeList children = root.getChildNodes();

		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if ("Match".equals(DOMHelper.getLocalName(child)))
			{
				matchType.add(Match.getInstance(child, metaData));
			}
		}

		if (matchType.isEmpty())
		{
			throw new ParsingException("AllOf must contain at least one Match");
		}

		return new AllOf(matchType, PolicyMetaData.XACML_VERSION_3_0);
	}

	public static List<TargetSection> getTargetSection(Node root, PolicyMetaData metadata) throws ParsingException
	{
		List<TargetSection> targetSection = new ArrayList<TargetSection>();
		NodeList children = root.getChildNodes();

		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if ("Match".equals(DOMHelper.getLocalName(child)))
			{
				NodeList myNodes = child.getChildNodes();
				for (int j = 0; j < myNodes.getLength(); j++)
				{

					if ("AttributeDesignator".equals(myNodes.item(j).getNodeName()))
					{
						String myCategory = myNodes.item(j).getAttributes().getNamedItem("Category").getNodeValue();
						targetSection.add(TargetSection.getInstance(AttributeDesignator.getInstance(child, myCategory, metadata)));
					} else if ("AttributeSelector".equals(myNodes.item(j).getNodeName()))
					{

					} else
					{
						throw new ParsingException("Unknow Element: " + myNodes.item(j).getNodeName() + ". "
								+ "Supported element of Match are AttribtueDesignator and AttributeSelector.");
					}
				}
			}
		}

		return targetSection;
	}

	/**
	 * Determines whether this <code>AllOf</code> matches the input request (whether it is
	 * applicable).Here is the table shown in the specification: <code>
	 * 		<Match> values 						<AllOf> value 
	 * 		All True				 			“Match” 
	 * 		No False and at least 
	 * 		one "Indeterminate" 				“Indeterminate”
	 * 		At least one False					"No Match"
	 * </code>
	 * 
	 * @param context
	 *            the representation of the request
	 * 
	 * @return the result of trying to match the
	 *         {@link oasis.names.tc.xacml._3_0.core.schema.wd_17.Match} and the request; if some
	 *         evaluated child Match (Match.match(ctx)) returned null, then return Indeterminate
	 */
	public MatchResult match(EvaluationCtx context)
	{
		/*
		 * By construction, there must be at least one AllOf Let's check it to be sure
		 */
		final List<oasis.names.tc.xacml._3_0.core.schema.wd_17.Match> matchList = this.getMatches();
		if (matchList == null || matchList.isEmpty())
		{
			/*
			 * TODO: provide a way to identify the <AllOf>
			 */
			final Status status = new Status(Collections.singletonList(Status.STATUS_PROCESSING_ERROR),
					"Invalid (Policy(Set)|Rule)#?<Target> / AnyOf#? / AllOf#?: 0 <Match>. Must contain at least one <Match>");
			return new MatchResult(MatchResult.INDETERMINATE, status);
		}

		MatchResult firstIndeterminate = null;
		MatchResult lastMatch = null;
		int childIndex = 0;
		for (oasis.names.tc.xacml._3_0.core.schema.wd_17.Match jaxbMatch : matchList)
		{
			final Match match = (Match) jaxbMatch;
			final MatchResult matchResult = match.match(context);
			if (matchResult == null)
			{
				// Invalid value for Match
				/*
				 * TODO: provide a way to identify the <AllOf>
				 */
				final Status status = new Status(Collections.singletonList(Status.STATUS_PROCESSING_ERROR),
						"Error processing (Policy(Set)|Rule)#?<Target> / AnyOf#? / AllOf#? / Match#" + childIndex);
				return new MatchResult(MatchResult.INDETERMINATE, status);
			}

			// matchResult != null at this point
			final int matchResultId = matchResult.getResult();
			/*
			 * Check False (=NO_MATCH) value first
			 * 
			 * FIXME: MatchResult should be an enum type to make this kind of check simpler with
			 * switch statements
			 */
			if (matchResultId == MatchResult.NO_MATCH)
			{
				/*
				 * At least one False (=NO_MATCH), so return False which is what this matchResult is
				 * already
				 */
				return matchResult;
			}

			if (matchResultId == MatchResult.INDETERMINATE)
			{
				firstIndeterminate = matchResult;
			} else
			{
				// it is a MATCH (only other possible case)
				lastMatch = matchResult;
			}

			childIndex += 1;
		}

		// No False (=NO_MATCH) occurred
		// firstIndeterminate == null iff no Indeterminate occurred
		if (firstIndeterminate == null)
		{
			// No False/Indeterminate, i.e. all True, return a True (MATCH) result
			return lastMatch;
		}

		// No False but at least one Indeterminate (firstIndeterminate != null)
		return firstIndeterminate;
	}
}