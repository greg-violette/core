/**
 *
 *  Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *    1. Redistribution of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *    2. Redistribution in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of Sun Microsystems, Inc. or the names of contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  This software is provided "AS IS," without a warranty of any kind. ALL
 *  EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 *  ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 *  OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 *  AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 *  AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 *  DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 *  REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 *  INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 *  OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 *  EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 *  You acknowledge that this software is not designed or intended for use in
 *  the design, construction, operation or maintenance of any nuclear facility.
 */
package com.sun.xacml;

import com.thalesgroup.authzforce.core.policy.PolicyVersionId;

/**
 * Supports the three version constraints that can be included with a policy reference. This class
 * also provides a simple set of comparison methods for matching against the constraints. Note that
 * this feature was introduced in XACML 2.0, which means that constraints are never used in pre-2.0
 * policy references.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class VersionConstraints
{

	// the three constraints
	private final PolicyVersionPattern versionPattern;
	private final PolicyVersionPattern earliestVersionPattern;
	private final PolicyVersionPattern latestVersionPattern;

	private static class PolicyVersionPattern
	{
		private static final int WILDCARD = -1;
		private static final int PLUS = -2;

		private final String xacmlVersionMatch;
		private final int[] matchNumbers;

		private PolicyVersionPattern(String xacmlVersionMatch)
		{
			assert xacmlVersionMatch != null;
			if (xacmlVersionMatch.isEmpty() || xacmlVersionMatch.startsWith(".") || xacmlVersionMatch.endsWith("."))
			{
				throw new IllegalArgumentException("Invalid VersionMatch expression: '" + xacmlVersionMatch + "'");
			}

			final String[] tokens = xacmlVersionMatch.split("\\.");
			matchNumbers = new int[tokens.length];
			for (int i = 0; i < tokens.length; i++)
			{
				final String token = tokens[i];
				switch (token)
				{
					case "*":
						matchNumbers[i] = WILDCARD;
						break;
					case "+":
						matchNumbers[i] = PLUS;
						break;
					default:
						final int number;
						try
						{
							number = Integer.parseInt(tokens[i], 10);
						} catch (NumberFormatException e)
						{
							throw new IllegalArgumentException("Invalid VersionMatch expression: '" + xacmlVersionMatch + "'", e);
						}

						if (number < 0)
						{
							throw new IllegalArgumentException("Invalid VersionMatch expression: '" + xacmlVersionMatch + "'. Number #" + i + " (=" + number + ") is not a positive integer");
						}

						matchNumbers[i] = number;
						break;
				}
			}

			this.xacmlVersionMatch = xacmlVersionMatch;
		}

		@Override
		public String toString()
		{
			return xacmlVersionMatch;
		}

		private boolean matches(PolicyVersionId version)
		{
			final int[] versionNumbers = version.getNumberSequence();
			final int lowestLen = Math.min(matchNumbers.length, versionNumbers.length);
			for (int i = 0; i < lowestLen; i++)
			{
				final int matchNum = matchNumbers[i];
				switch (matchNum)
				{
					case PLUS:
						// always matches everything from here
						return true;
					case WILDCARD:
						// always matches any versionNumbers[i], so go on
						break;
					default:
						if (matchNum != versionNumbers[i])
						{
							return false;
						}

						// else same number, so go on
						break;
				}
			}

			/*
			 * At this point, last matchNum is either a wildcard or integer. Version matches iff
			 * there is no extra number in either matchNumbers of versionNumbers, i.e. they have
			 * same length
			 */
			return matchNumbers.length == versionNumbers.length;
		}

		public boolean isLaterOrMatches(PolicyVersionId version)
		{
			final int[] versionNumbers = version.getNumberSequence();
			final int lowestLen = Math.min(matchNumbers.length, versionNumbers.length);
			for (int i = 0; i < lowestLen; i++)
			{
				final int matchNum = matchNumbers[i];
				switch (matchNum)
				{
					case PLUS:
						// always matches everything from here
						return true;
					case WILDCARD:
						/*
						 * Always matches any versionNumbers[i], and we could always find an
						 * acceptable version V > version argument that matches this pattern
						 * (matchNumbers) by taking a single number greater than versionNumbers[i]
						 * at the same index in V. So versionNumbers is earlier than the latest
						 * acceptable.
						 */
						return true;
					default:
						final int versionNum = versionNumbers[i];
						if (matchNum < versionNum)
						{
							return false;
						}

						if (matchNum > versionNum)
						{
							return true;
						}

						// else same number, so go on
						break;
				}
			}

			/*
			 * At this point, we know matchNumbers is a sequence of numbers (no wildcard/plus
			 * symbol). It is later than or matches versionNumbers iff same size or longer.
			 */
			return matchNumbers.length >= versionNumbers.length;
		}

		public boolean isEarlierOrMatches(PolicyVersionId version)
		{
			final int[] versionNumbers = version.getNumberSequence();
			final int lowestLen = Math.min(matchNumbers.length, versionNumbers.length);
			for (int i = 0; i < lowestLen; i++)
			{
				final int matchNum = matchNumbers[i];
				final int versionNum;
				switch (matchNum)
				{
					case PLUS:
						// always matches everything from here
						return true;
					case WILDCARD:
						versionNum = versionNumbers[i];
						if (versionNum != 0)
						{
							/*
							 * We can find an earlier matching version (with any number < versionNum
							 * here).
							 */
							return true;
						}

						// versionNum = 0. Result depends on the next numbers.
						break;
					default:
						versionNum = versionNumbers[i];
						if (matchNum < versionNum)
						{
							return true;
						}

						if (matchNum > versionNum)
						{
							return false;
						}

						// else same number, so go on
						break;
				}
			}

			/*
			 * if matchNumbers.length <= versionNumbers.length -> true
			 */
			return matchNumbers.length <= versionNumbers.length;
		}

	}

	/**
	 * Creates a <code>VersionConstraints</code> with the three optional constraint strings. Each of
	 * the three strings must conform to the VersionMatchType type defined in the XACML schema. Any
	 * of the strings may be null to specify that the given constraint is not used.
	 * 
	 * @param versionMatch
	 *            matching expression for the version; or null if none
	 * @param earliestMatch
	 *            matching expression for the earliest acceptable version; or null if none
	 * @param latestMatch
	 *            matching expression for the earliest acceptable version; or null if none
	 */
	public VersionConstraints(String versionMatch, String earliestMatch, String latestMatch)
	{
		this.versionPattern = versionMatch == null ? null : new PolicyVersionPattern(versionMatch);
		this.earliestVersionPattern = earliestMatch == null ? null : new PolicyVersionPattern(earliestMatch);
		this.latestVersionPattern = latestMatch == null ? null : new PolicyVersionPattern(latestMatch);
	}

	// /**
	// * Checks if the given version string meets all three constraints.
	// *
	// * @param otherVersion
	// * the version to compare, which is formatted as a VersionType XACML type
	// *
	// * @return true if the given version meets all the constraints
	// */
	// public boolean match(PolicyVersionId otherVersion)
	// {
	// return (versionPattern == null || versionPattern.matches(otherVersion)) &&
	// (latestVersionPattern == null || latestVersionPattern.isLaterOrMatches(otherVersion)) &&
	// (earliestVersionPattern == null || earliestVersionPattern.isEarlierOrMatches(otherVersion));
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format("Version=%s,EarliestVersion=%s,LatestVersion=%s", (versionPattern == null) ? "*" : versionPattern, (earliestVersionPattern == null) ? "*" : earliestVersionPattern, (latestVersionPattern == null) ? "*" : latestVersionPattern);
	}

	/**
	 * Check version against LatestVersion pattern
	 * 
	 * @param version
	 *            input version to be checked
	 * @return true iff LatestVersion matched
	 */
	public boolean matchLatestVersion(PolicyVersionId version)
	{
		return latestVersionPattern == null || latestVersionPattern.isLaterOrMatches(version);
	}

	/**
	 * Check version against EarliestVersion pattern
	 * 
	 * @param version
	 *            input version to be checked
	 * @return true iff EarliestVersion matched
	 */
	public boolean matchEarliestVersion(PolicyVersionId version)
	{
		return earliestVersionPattern == null || earliestVersionPattern.isEarlierOrMatches(version);
	}

	/**
	 * Check version against Version pattern
	 * 
	 * @param version
	 *            input version to be checked
	 * @return true iff Version matched
	 */
	public boolean matchVersion(PolicyVersionId version)
	{
		return versionPattern == null || versionPattern.matches(version);
	}

	/**
	 * Get Version pattern:
	 * 
	 * @return Version to be matched
	 */
	public String getVersionPattern()
	{
		return this.versionPattern.toString();
	}

	/**
	 * Get EarliestVersion pattern: matching expression for the earliest acceptable version
	 * 
	 * @return EarliestVersion to be matched
	 */
	public String getEarliestVersionPattern()
	{
		return this.earliestVersionPattern.toString();
	}

	/**
	 * Get LatestVersion pattern: matching expression for the latest acceptable version
	 * 
	 * @return LatestVersion to be matched
	 */
	public String getLatestVersionPattern()
	{
		return this.latestVersionPattern.toString();
	}

	// public static void main(String... args)
	// {
	// PolicyVersionPattern vp = new PolicyVersionPattern("1.*.4.5");
	// PolicyVersionId v = new PolicyVersionId("1.2.4.5");
	// System.out.println(vp.isLaterOrMatches(v));
	// }
}
