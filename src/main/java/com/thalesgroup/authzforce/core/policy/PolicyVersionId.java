package com.thalesgroup.authzforce.core.policy;

import java.util.Arrays;

/**
 * Representation of XACML VersionType:
 * <p>
 * 
 * <pre>
 * {@code 
 * <xs:simpleType name="VersionType"> 
 * 	<xs:restriction base="xs:string"> 
 * 		<xs:pattern value="(\d+\.)*\d+"/> 
 * 	</xs:restriction> 
 * </xs:simpleType>
 * }
 * </pre>
 * 
 * 
 */
public class PolicyVersionId implements Comparable<PolicyVersionId>
{
	private static final IllegalArgumentException UNDEFINED_VERSION_EXCEPTION = new IllegalArgumentException("Policy(Set) Version undefined");

	private final String version;
	private final int[] numbers;

	/**
	 * Creates instance from version in text form
	 * 
	 * @param version
	 *            version string
	 * @throws IllegalArgumentException
	 *             if version is null or not valid according to pattern definition in XACML schema:
	 *             "(\d+\.)*\d+"
	 */
	public PolicyVersionId(String version) throws IllegalArgumentException
	{
		if (version == null)
		{
			throw UNDEFINED_VERSION_EXCEPTION;
		}

		if (version.isEmpty() || version.startsWith(".") || version.endsWith("."))
		{
			throw new IllegalArgumentException("Invalid Policy(Set) Version: '" + version + "'");
		}

		final String[] tokens = version.split("\\.");
		numbers = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++)
		{
			final int number;
			try
			{
				number = Integer.parseInt(tokens[i], 10);
			} catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Invalid Policy(Set) Version: '" + version + "'", e);
			}

			if (number < 0)
			{
				throw new IllegalArgumentException("Invalid Policy(Set) Version: '" + version + "'. Number #" + i + " (=" + number + ") is not a positive integer");
			}

			numbers[i] = number;
		}

		this.version = version;
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(numbers);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolicyVersionId other = (PolicyVersionId) obj;
		if (!Arrays.equals(numbers, other.numbers))
			return false;
		return true;
	}

	@Override
	public int compareTo(PolicyVersionId other)
	{
		final int lowestLen = Math.min(numbers.length, other.numbers.length);
		for (int i = 0; i < lowestLen; i++)
		{
			final int comparResult = Integer.compare(numbers[i], other.numbers[i]);
			// if not equal number, we're done
			if (comparResult != 0)
			{
				return comparResult;
			}

			// equal number in both versions, so go on with next number
		}

		// all numbers are the same up to index lowestLen-1
		// the longest sequence is greater so, compare lengths
		return Integer.compare(numbers.length, other.numbers.length);
	}

	// public static void main(String[] args)
	// {
	// final PolicyVersionId v1 = new PolicyVersionId("1.3.4");
	// final PolicyVersionId v2 = new PolicyVersionId("1.2.4");
	// System.out.println(v1.compareTo(v2));
	// }

	@Override
	public String toString()
	{
		return version;
	}

	/**
	 * Get version as sequence of positive integers
	 * 
	 * @return sequence of positive integers from version
	 */
	public int[] getNumberSequence()
	{
		return numbers;
	}

}
