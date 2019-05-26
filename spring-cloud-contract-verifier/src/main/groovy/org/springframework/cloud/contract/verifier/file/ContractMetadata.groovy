/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.contract.verifier.file

import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import org.springframework.cloud.contract.spec.Contract
import org.springframework.cloud.contract.spec.internal.DslProperty
import org.springframework.cloud.contract.spec.internal.Headers
import org.springframework.cloud.contract.verifier.util.ContentType
import org.springframework.cloud.contract.verifier.util.ContentUtils

/**
 * Contains metadata for a particular file with a DSL
 *
 * @author Jakub Kubrynski, codearte.io
 *
 * @since 1.0.0
 */
@CompileStatic
@EqualsAndHashCode
@ToString
class ContractMetadata {
	/**
	 * Path to the file
	 */
	final Path path
	/**
	 * Should the contract be ignored
	 */
	final boolean ignored
	/**
	 * How many files are there in the folder
	 */
	final int groupSize
	/**
	 * If scenario related will contain an order of execution
	 */
	final Integer order
	/**
	 * The list of contracts for the given file
	 */
	final Collection<Contract> convertedContract = []
	/**
	 * Converted contracts with meta data information
	 */
	final Collection<SingleContractMetadata> convertedContractWithMetadata = []

	ContractMetadata(Path path, boolean ignored, int groupSize, Integer order, Contract convertedContract) {
		this(path, ignored, groupSize, order, [convertedContract])
	}

	ContractMetadata(Path path, boolean ignored, int groupSize, Integer order, Collection<Contract> convertedContract) {
		this.groupSize = groupSize
		this.path = path
		this.ignored = ignored
		this.order = order
		this.convertedContract.addAll(convertedContract)
		this.convertedContractWithMetadata.addAll(
				this.convertedContract
						.collect { new SingleContractMetadata(it, this) })
	}
}

@CompileStatic
@EqualsAndHashCode
@ToString
class SingleContractMetadata {
	final ContractMetadata contractMetadata
	final Contract contract
	final ContentType inputContentType
	final ContentType outputContentType
	private final boolean http

	SingleContractMetadata(Contract contract, ContractMetadata contractMetadata) {
		this.contract = contract
		Headers inputHeaders = inputHeaders(contract)
		DslProperty inputBody = inputBody(contract)
		Headers outputHeaders = outputHeaders(contract)
		DslProperty outputBody = outputBody(contract)
		this.inputContentType = ContentUtils.evaluateContentType(inputHeaders, inputBody)
		this.outputContentType = ContentUtils.evaluateContentType(outputHeaders, outputBody)
		this.http = contract.request != null
		this.contractMetadata = contractMetadata
	}

	boolean isJson() {
		return this.inputContentType == ContentType.JSON ||
				this.outputContentType == ContentType.JSON
	}

	boolean isIgnored() {
		return this.contract.ignored
	}

	boolean isXml() {
		return this.inputContentType == ContentType.XML ||
				this.outputContentType == ContentType.XML
	}

	boolean isHttp() {
		return this.http
	}

	boolean isMessaging() {
		return !isHttp()
	}

	private DslProperty inputBody(Contract contract) {
		return contract.request?.body ?: contract.input?.messageBody
	}

	private Headers inputHeaders(Contract contract) {
		return contract.request?.headers ?: contract.input?.messageHeaders
	}

	private DslProperty outputBody(Contract contract) {
		return contract.response?.body ?: contract.outputMessage?.body
	}

	private Headers outputHeaders(Contract contract) {
		return contract.response?.headers ?: contract.outputMessage?.headers
	}
}
