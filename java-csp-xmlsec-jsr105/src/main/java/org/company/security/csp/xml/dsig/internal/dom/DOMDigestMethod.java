/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 */
/*
 * $Id$
 */
package org.company.security.csp.xml.dsig.internal.dom;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.spec.DigestMethodParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Element;

/**
 * DOM-based abstract implementation of DigestMethod.
 *
 * @author Sean Mullan
 */
public abstract class DOMDigestMethod extends BaseStructure 
    implements DigestMethod {

    static final String SHA384 =
        "http://www.w3.org/2001/04/xmldsig-more#sha384"; // see RFC 4051
    public static final String GOST3411_V1 = Constants.MoreAlgorithmsSpecNS + "gostr3411";
    public static final String GOST3411_V2 = "urn:ietf:params:xml:ns:cpxmlsec:algorithms:gostr3411";

    private String algorithm;
    private DigestMethodParameterSpec params;
    
    
    /**
     * Creates a <code>DOMDigestMethod</code>.
     *
     * @param params the algorithm-specific params (may be <code>null</code>)
     * @throws InvalidAlgorithmParameterException if the parameters are not
     *    appropriate for this digest method
     */
    DOMDigestMethod(String algorithm, AlgorithmParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params != null && !(params instanceof DigestMethodParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("params must be of type DigestMethodParameterSpec");
        }
        checkParams((DigestMethodParameterSpec)params);
        this.algorithm = algorithm;
        this.params = (DigestMethodParameterSpec)params;
    }

    /**
     * Creates a <code>DOMDigestMethod</code> from an element. This constructor
     * invokes the abstract {@link #unmarshalParams unmarshalParams} method to
     * unmarshal any algorithm-specific input parameters.
     *
     * @param dmElem a DigestMethod element
     */
    DOMDigestMethod(Element dmElem) throws MarshalException {
        this.algorithm = DOMUtils.getAttributeValue(dmElem, "Algorithm");
        
        Element paramsElem = DOMUtils.getFirstChildElement(dmElem);
        if (paramsElem != null) {
            params = unmarshalParams(paramsElem);
        }
        try {
            checkParams(params);
        } catch (InvalidAlgorithmParameterException iape) {
            throw new MarshalException(iape);
        }
    }

    static DigestMethod unmarshal(Element dmElem) throws MarshalException {
        String alg = DOMUtils.getAttributeValue(dmElem, "Algorithm");
        if (alg.equals(DigestMethod.SHA1)) {
            return new SHA1(dmElem);
        } else if (alg.equals(DigestMethod.SHA256)) {
            return new SHA256(dmElem);
        } else if (alg.equals(SHA384)) {
            return new SHA384(dmElem);
        } else if (alg.equals(DigestMethod.SHA512)) {
            return new SHA512(dmElem);
        } else if (alg.equals(GOST3411_V1)) {
        	return new GOST3411(dmElem);
        } else if (alg.equals(GOST3411_V2)) {
        	return new GOST3411(dmElem);
        } else {
            throw new MarshalException("unsupported DigestMethod algorithm: " +
                                       alg);
        }
    }
    
    static DigestMethod newDigestMethod(String algorithm,
    		DigestMethodParameterSpec params) throws NoSuchAlgorithmException,
    		InvalidAlgorithmParameterException {
    	if (algorithm == null) {
    		throw new NullPointerException();
    	}
    	if (algorithm.equals(DigestMethod.SHA1)) {
    		return new DOMDigestMethod.SHA1(algorithm, params);
    	} else if (algorithm.equals(DigestMethod.SHA256)) {
    		return new DOMDigestMethod.SHA256(algorithm, params);
    	} else if (algorithm.equals(DOMDigestMethod.SHA384)) {
    		return new DOMDigestMethod.SHA384(algorithm, params);
    	} else if (algorithm.equals(DigestMethod.SHA512)) {
    		return new DOMDigestMethod.SHA512(algorithm, params);
        } else if (algorithm.equals(GOST3411_V1)) {
        	return new GOST3411(algorithm, params);
        } else if (algorithm.equals(GOST3411_V2)) {
        	return new GOST3411(algorithm, params);
    	} else {
    		throw new NoSuchAlgorithmException("unsupported algorithm");
    	}
    }

    public String getAlgorithm() {
		return algorithm;
	}
    
    /**
     * Checks if the specified parameters are valid for this algorithm. By
     * default, this method throws an exception if parameters are specified
     * since most DigestMethod algorithms do not have parameters. Subclasses
     * should override it if they have parameters.
     *
     * @param params the algorithm-specific params (may be <code>null</code>)
     * @throws InvalidAlgorithmParameterException if the parameters are not
     *    appropriate for this digest method
     */
    void checkParams(DigestMethodParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("no parameters " +
                "should be specified for the " + getMessageDigestAlgorithm() +
                " DigestMethod algorithm");
        }
    }

    @Override
    public final AlgorithmParameterSpec getParameterSpec() {
        return params;
    }

    /**
     * Unmarshals <code>DigestMethodParameterSpec</code> from the specified 
     * <code>Element</code>.  By default, this method throws an exception since
     * most DigestMethod algorithms do not have parameters. Subclasses should 
     * override it if they have parameters.
     *
     * @param paramsElem the <code>Element</code> holding the input params
     * @return the algorithm-specific <code>DigestMethodParameterSpec</code>
     * @throws MarshalException if the parameters cannot be unmarshalled
     */
    DigestMethodParameterSpec unmarshalParams(Element paramsElem)
        throws MarshalException
    {
        throw new MarshalException("no parameters should " +
                                   "be specified for the " +
                                   getMessageDigestAlgorithm() +
                                   " DigestMethod algorithm");
    }

    /**
     * This method invokes the abstract {@link #marshalParams marshalParams} 
     * method to marshal any algorithm-specific parameters.
     */
    public static void marshal(XmlWriter xwriter, DigestMethod digest, String prefix)
        throws MarshalException
    {
        xwriter.writeStartElement(prefix, "DigestMethod", XMLSignature.XMLNS);
        xwriter.writeAttribute("", "", "Algorithm", digest.getAlgorithm());

        // this is totally over-engineered - nothing implements marshalParams.
        if (digest.getParameterSpec() != null && digest instanceof DOMDigestMethod) {
            ( (DOMDigestMethod) digest).marshalParams(xwriter, prefix);
        }
        xwriter.writeEndElement(); // "DigestMethod"
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DigestMethod)) {
            return false;
        }
        DigestMethod odm = (DigestMethod)o;

        boolean paramsEqual = (params == null ? odm.getParameterSpec() == null :
            params.equals(odm.getParameterSpec()));

        return (getAlgorithm().equals(odm.getAlgorithm()) && paramsEqual);
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        if (params != null) {
            result = 31 * result + params.hashCode();
        }
        String algorithm = getAlgorithm();
        if (algorithm != null) {
            result = 31 * result + algorithm.hashCode();
        }
        
        return result;
    }

    /**
     * Marshals the algorithm-specific parameters to an Element and
     * appends it to the specified parent element. By default, this method
     * throws an exception since most DigestMethod algorithms do not have
     * parameters. Subclasses should override it if they have parameters.
     *
     * @param parent the parent element to append the parameters to
     * @param the namespace prefix to use
     * @throws MarshalException if the parameters cannot be marshalled
     */
    void marshalParams(XmlWriter xwriter, String prefix)
        throws MarshalException
    {
        throw new MarshalException("no parameters should " +
                                   "be specified for the " +
                                   getMessageDigestAlgorithm() +
                                   " DigestMethod algorithm");
    }

    /**
     * Returns the MessageDigest standard algorithm name.
     */
    abstract String getMessageDigestAlgorithm();

    static final class SHA1 extends DOMDigestMethod {
        SHA1(String algorithm, AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(algorithm, params);
        }
        SHA1(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return DigestMethod.SHA1;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA-1";
        }
    }

    static final class SHA256 extends DOMDigestMethod {
        SHA256(String algorithm, AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(algorithm, params);
        }
        SHA256(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return DigestMethod.SHA256;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA-256";
        }
    }

    static final class SHA384 extends DOMDigestMethod {
        SHA384(String algorithm, AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(algorithm, params);
        }
        SHA384(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA384;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA-384";
        }
    }

    static final class SHA512 extends DOMDigestMethod {
        SHA512(String algorithm, AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(algorithm, params);
        }
        SHA512(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return DigestMethod.SHA512;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA-512";
        }
    }
    
    static final class GOST3411 extends DOMDigestMethod {
    	GOST3411(String algorithm, AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(algorithm, params);
        }
    	GOST3411(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        String getMessageDigestAlgorithm() {
            return "GOST3411";
        }
    }
}
