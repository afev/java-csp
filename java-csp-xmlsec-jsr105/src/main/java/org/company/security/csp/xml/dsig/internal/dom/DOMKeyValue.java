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
import javax.xml.crypto.dsig.keyinfo.KeyValue;



// import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.company.security.csp.xml.security.exceptions.Base64DecodingException;
import org.company.security.csp.xml.security.utils.Base64;
import org.w3c.dom.Element;

/**
 * DOM-based implementation of KeyValue.
 *
 * @author Sean Mullan
 */
public abstract class DOMKeyValue<K extends PublicKey> extends BaseStructure implements KeyValue {

    private static final String XMLDSIG_11_XMLNS 
        = "http://www.w3.org/2009/xmldsig11#";
    private static final String XMLDSIG_XMLNS 
        = "http://www.w3.org/2009/xmldsig#";
    
    private final K publicKey;

    public DOMKeyValue(K key) throws KeyException {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.publicKey = key;
    }

    /**
     * Creates a <code>DOMKeyValue</code> from an element.
     *
     * @param kvtElem a KeyValue child element
     */
    public DOMKeyValue(Element kvtElem) throws MarshalException {
        this.publicKey = unmarshalKeyValue(kvtElem);
    }

    static KeyValue unmarshal(Element kvElem) throws MarshalException {
        Element kvtElem = DOMUtils.getFirstChildElement(kvElem);
        if (kvtElem.getLocalName().equals("DSAKeyValue")) {
            return new DSA(kvtElem);
        } else if (kvtElem.getLocalName().equals("RSAKeyValue")) {
            return new RSA(kvtElem);
        } else if (kvtElem.getLocalName().equals("ECKeyValue")) {
            return new EC(kvtElem);
        } else if (kvtElem.getLocalName().equals("GOSTKeyValue")) {
        	return new GOST3410(kvtElem);
        } else {
            return new Unknown(kvtElem);
        }
    }
    
    static KeyValue newKeyValue(PublicKey key)  throws KeyException {
        String algorithm = key.getAlgorithm();
        if (algorithm.equals("DSA")) {
            return new DOMKeyValue.DSA((DSAPublicKey) key);
        } else if (algorithm.equals("RSA")) {
            return new DOMKeyValue.RSA((RSAPublicKey) key);
        } else if (algorithm.equals("EC")) {
            return new DOMKeyValue.EC((ECPublicKey) key);
        } else if (algorithm.equals("GOST3410")) {
        	return new DOMKeyValue.GOST3410((PublicKey) key);
        } else {
            throw new KeyException("unsupported key algorithm: " + algorithm);
        }
    }

    @Override
    public PublicKey getPublicKey() throws KeyException {
        if (publicKey == null) {
            throw new KeyException("can't convert KeyValue to PublicKey");
        } else {
            return publicKey;
        }
    }

    public void marshal(XmlWriter xwriter, String dsPrefix, XMLCryptoContext context)
        throws MarshalException
    {
        // create KeyValue element
        xwriter.writeStartElement(dsPrefix, "KeyValue", XMLSignature.XMLNS);
        marshalPublicKey(xwriter, publicKey, dsPrefix, context);
        xwriter.writeEndElement(); // "KeyValue"
    }

    abstract void marshalPublicKey(XmlWriter xwriter, K key, String dsPrefix,
        XMLCryptoContext context) throws MarshalException;

    abstract K unmarshalKeyValue(Element kvtElem) 
        throws MarshalException;

    private static PublicKey generatePublicKey(KeyFactory kf, KeySpec keyspec) {
        try {
            return kf.generatePublic(keyspec);
        } catch (InvalidKeySpecException e) {
            //@@@ should dump exception to log
            return null;
        }
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeyValue)) {
            return false;
        }
        try {
            KeyValue kv = (KeyValue)obj;
            if (publicKey == null ) {
                if (kv.getPublicKey() != null) {
                    return false;
                }
            } else if (!publicKey.equals(kv.getPublicKey())) {
                return false;
            }
        } catch (KeyException ke) {
            // no practical way to determine if the keys are equal
            return false;
        }
        
        return true;
    }

    public static BigInteger decode(Element elem) throws MarshalException {
        try {
            String base64str = BaseStructure.textOfNode(elem);
            return Base64.decodeBigIntegerFromString(base64str);
        } catch (Exception ex) {
            throw new MarshalException(ex);
        }
    }

    public static void writeBase64BigIntegerElement(
        XmlWriter xwriter, String prefix, String localName, String namespaceURI, BigInteger value
    ) {
        xwriter.writeTextElement(prefix, localName, namespaceURI, Base64.encode(value));
    }
    
    public static void marshal(XmlWriter xwriter, BigInteger bigNum) {
        xwriter.writeCharacters(Base64.encode(bigNum));
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (publicKey != null) {
            result = 31 * result + publicKey.hashCode();
        }
        
        return result;
    }
    
    static final class RSA extends DOMKeyValue<RSAPublicKey> {
        // RSAKeyValue CryptoBinaries
        private KeyFactory rsakf;

        RSA(RSAPublicKey key) throws KeyException {
            super(key);
        }

        RSA(Element elem) throws MarshalException {
            super(elem);
        }

        @Override
        void marshalPublicKey(XmlWriter xwriter, RSAPublicKey publicKey, String dsPrefix, 
            XMLCryptoContext context) throws MarshalException {
            xwriter.writeStartElement(dsPrefix, "RSAKeyValue", XMLSignature.XMLNS);
            
            writeBase64BigIntegerElement(xwriter, dsPrefix, "Modulus", XMLSignature.XMLNS, publicKey.getModulus());
            writeBase64BigIntegerElement(xwriter, dsPrefix, "Exponent", XMLSignature.XMLNS, publicKey.getPublicExponent());

            xwriter.writeEndElement(); // "RSAKeyValue"
        }

        @Override
        RSAPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (rsakf == null) {
                try {
                    rsakf = KeyFactory.getInstance("RSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create RSA KeyFactory: " + e.getMessage());
                }
            }
            Element modulusElem = DOMUtils.getFirstChildElement(kvtElem);
            BigInteger modulus = decode(modulusElem);
            Element exponentElem = DOMUtils.getNextSiblingElement(modulusElem);
            BigInteger exponent = decode(exponentElem);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) generatePublicKey(rsakf, spec);
        }
    }

    static final class DSA extends DOMKeyValue<DSAPublicKey> {
        // DSAKeyValue CryptoBinaries
        private KeyFactory dsakf;

        DSA(DSAPublicKey key) throws KeyException {
            super(key);
        }

        DSA(Element elem) throws MarshalException {
            super(elem);
        }

        @Override
        void marshalPublicKey(XmlWriter xwriter, DSAPublicKey publicKey, String dsPrefix,
                XMLCryptoContext context)
            throws MarshalException
        {
            DSAParams params = publicKey.getParams();
            
            xwriter.writeStartElement(dsPrefix, "DSAKeyValue", XMLSignature.XMLNS);
            
            // parameters J, Seed & PgenCounter are not included
            writeBase64BigIntegerElement(xwriter, dsPrefix, "P", XMLSignature.XMLNS, params.getP());
            writeBase64BigIntegerElement(xwriter, dsPrefix, "Q", XMLSignature.XMLNS, params.getQ());
            writeBase64BigIntegerElement(xwriter, dsPrefix, "G", XMLSignature.XMLNS, params.getG());
            writeBase64BigIntegerElement(xwriter, dsPrefix, "Y", XMLSignature.XMLNS, publicKey.getY() );
            
            xwriter.writeEndElement(); // "DSAKeyValue"
        }

        @Override
        DSAPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (dsakf == null) {
                try {
                    dsakf = KeyFactory.getInstance("DSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create DSA KeyFactory: " + e.getMessage());
                }
            }
            Element curElem = DOMUtils.getFirstChildElement(kvtElem);
            // check for P and Q
            BigInteger p = null;
            BigInteger q = null;
            if (curElem.getLocalName().equals("P")) {
                p = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem);
                q = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem);
            } 
            BigInteger g = null;
            if (curElem.getLocalName().equals("G")) {
                g = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem);
            }
            BigInteger y = decode(curElem);
            curElem = DOMUtils.getNextSiblingElement(curElem);
            //if (curElem != null && curElem.getLocalName().equals("J")) {
                //j = new DOMCryptoBinary(curElem.getFirstChild());
                // curElem = DOMUtils.getNextSiblingElement(curElem);
            //}
            //@@@ do we care about j, pgenCounter or seed?
            DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
            return (DSAPublicKey) generatePublicKey(dsakf, spec);
        }
    }

    static final class EC extends DOMKeyValue<ECPublicKey> {
        // ECKeyValue CryptoBinaries
        private byte[] ecPublicKey;
        private KeyFactory eckf;
        private ECParameterSpec ecParams;
        private Method encodePoint, decodePoint, getCurveName,
                       getECParameterSpec;

        EC(ECPublicKey ecKey) throws KeyException {
            super(ecKey);
            ECPoint ecPoint = ecKey.getW();
            ecParams = ecKey.getParams();
            try {
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws
                            ClassNotFoundException, NoSuchMethodException
                        {
                            getMethods();
                            return null;
                        }
                    }
                );
            } catch (PrivilegedActionException pae) {
                throw new KeyException("ECKeyValue not supported",
                                        pae.getException());
            }
            Object[] args = new Object[] { ecPoint, ecParams.getCurve() };
            try {
                ecPublicKey = (byte[])encodePoint.invoke(null, args);
            } catch (IllegalAccessException iae) {
                throw new KeyException(iae);
            } catch (InvocationTargetException ite) {
                throw new KeyException(ite);
            }
        }

        EC(Element dmElem) throws MarshalException {
            super(dmElem);
        }

        void getMethods() throws ClassNotFoundException, NoSuchMethodException {
            Class<?> c  = Class.forName("sun.security.ec.ECParameters");
            Class<?>[] params = new Class<?>[] { ECPoint.class, EllipticCurve.class };
            encodePoint = c.getMethod("encodePoint", params);
            params = new Class[] { ECParameterSpec.class };
            getCurveName = c.getMethod("getCurveName", params);
            params = new Class[] { byte[].class, EllipticCurve.class };
            decodePoint = c.getMethod("decodePoint", params);
            c  = Class.forName("sun.security.ec.NamedCurve");
            params = new Class[] { String.class };
            getECParameterSpec = c.getMethod("getECParameterSpec", params);
        }

        @Override
        void marshalPublicKey(XmlWriter xwriter, ECPublicKey publicKey, String dsPrefix,
                XMLCryptoContext context)
            throws MarshalException
        {
            String prefix = DOMUtils.getNSPrefix(context, XMLDSIG_11_XMLNS);
            xwriter.writeStartElement(prefix, "ECKeyValue", XMLDSIG_11_XMLNS);
            
            xwriter.writeStartElement(prefix, "NamedCurve", XMLDSIG_11_XMLNS);
            xwriter.writeNamespace(prefix, XMLDSIG_11_XMLNS);
            Object[] args = new Object[] { ecParams };
            try {
                String oid = (String) getCurveName.invoke(null, args);
                xwriter.writeAttribute("", "", "URI", "urn:oid:" + oid);
            } catch (IllegalAccessException iae) {
                throw new MarshalException(iae);
            } catch (InvocationTargetException ite) {
                throw new MarshalException(ite);
            }
            xwriter.writeEndElement();
            
            xwriter.writeStartElement(prefix, "PublicKey", XMLDSIG_11_XMLNS);
            String encoded = Base64.encode(ecPublicKey);
            xwriter.writeCharacters(encoded);
            xwriter.writeEndElement(); // "PublicKey"
            xwriter.writeEndElement(); // "ECKeyValue"
        }

        @Override
        ECPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (eckf == null) {
                try {
                    eckf = KeyFactory.getInstance("EC");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create EC KeyFactory: " + e.getMessage());
                }
            }
            try {
                AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Void>() {
                        @Override
                        public Void run() throws
                            ClassNotFoundException, NoSuchMethodException
                        {
                            getMethods();
                            return null;
                        }
                    }
                );
            } catch (PrivilegedActionException pae) {
                throw new MarshalException("ECKeyValue not supported",
                                           pae.getException());
            }
            ECParameterSpec ecParams = null;
            Element curElem = DOMUtils.getFirstChildElement(kvtElem);
            if (curElem.getLocalName().equals("ECParameters")) {
                throw new UnsupportedOperationException
                    ("ECParameters not supported");
            } else if (curElem.getLocalName().equals("NamedCurve")) {
                String uri = DOMUtils.getAttributeValue(curElem, "URI");
                // strip off "urn:oid"
                if (uri.startsWith("urn:oid:")) {
                    String oid = uri.substring(8);
                    try {
                        Object[] args = new Object[] { oid };
                        ecParams = (ECParameterSpec)
                                    getECParameterSpec.invoke(null, args);
                    } catch (IllegalAccessException iae) {
                        throw new MarshalException(iae);
                    } catch (InvocationTargetException ite) {
                        throw new MarshalException(ite);
                    }
                } else {
                    throw new MarshalException("Invalid NamedCurve URI");
                }
            } else {
                throw new MarshalException("Invalid ECKeyValue");
            }
            curElem = DOMUtils.getNextSiblingElement(curElem);
            ECPoint ecPoint = null;
            try {
                Object[] args = new Object[] { Base64.decode(curElem),
                                               ecParams.getCurve() };
                ecPoint = (ECPoint)decodePoint.invoke(null, args);
            } catch (Base64DecodingException bde) {
                throw new MarshalException("Invalid EC PublicKey", bde);
            } catch (IllegalAccessException iae) {
                throw new MarshalException(iae);
            } catch (InvocationTargetException ite) {
                throw new MarshalException(ite);
            }
/*
                ecPoint = sun.security.ec.ECParameters.decodePoint(
                    Base64.decode(curElem), ecParams.getCurve());
*/
            ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParams);
            return (ECPublicKey) generatePublicKey(eckf, spec);
        }
    }

    static final class GOST3410 extends DOMKeyValue<PublicKey> {
    	private KeyFactory gostkf;
    	
		GOST3410(Element kvtElem) throws MarshalException {
			super(kvtElem);
		}

		GOST3410(PublicKey key) throws KeyException {
			super(key);
		}

		@Override
		void marshalPublicKey(XmlWriter xwriter, PublicKey key,
				String dsPrefix, XMLCryptoContext context)
				throws MarshalException {

			String prefix = DOMUtils.getNSPrefix(context, XMLSignature.XMLNS);
			xwriter.writeStartElement(prefix, "GOSTKeyValue", XMLSignature.XMLNS);

			writeBase64BigIntegerElement(xwriter, dsPrefix, "PublicKey", XMLSignature.XMLNS, new BigInteger(1, key.getEncoded()));
		}

		@Override
		PublicKey unmarshalKeyValue(Element kvtElem) throws MarshalException {
			if (gostkf == null) {
                try {
                	gostkf = KeyFactory.getInstance("GOST3410");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create GOST3410EL KeyFactory: " + e.getMessage());
                }
            }
			Element curElem = DOMUtils.getFirstChildElement(kvtElem);
            BigInteger asn1encode = null;
            if (curElem.getLocalName().equals("PublicKey")) {
            	asn1encode = decode(curElem);
            }
			X509EncodedKeySpec spec = new X509EncodedKeySpec(asn1encode.toByteArray());
			return generatePublicKey(gostkf, spec);
		}
    	
    }
    
    static final class Unknown extends DOMKeyValue<PublicKey> {
        private XMLStructure externalPublicKey;
        Unknown(Element elem) throws MarshalException {
            super(elem);
        }
        @Override
        PublicKey unmarshalKeyValue(Element kvElem) throws MarshalException {
            externalPublicKey = new javax.xml.crypto.dom.DOMStructure(kvElem);
            return null;
        }
        @Override
        void marshalPublicKey(XmlWriter xwriter, PublicKey publicKey, String dsPrefix,
                XMLCryptoContext context)
            throws MarshalException
        {
            xwriter.marshalStructure(externalPublicKey, dsPrefix, context);
        }
    }
}
