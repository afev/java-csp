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
package org.company.security.csp.xml.dsig.internal.dom;

import javax.xml.crypto.XMLStructure;

import org.w3c.dom.Node;

public abstract class BaseStructure implements XMLStructure {

    /**
     * Since the behavior of {@link Model#getStringValue(Object)} returns the value
     * of all descendant text nodes of an element, whereas we just want the immediate children.
     * 
     * @param <N>
     * @param model
     * @param node
     * @return
     */
    public static String textOfNode(Node node) {
        return node.getFirstChild().getNodeValue();
    }

    public final boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        } else {
            return false;
        }
    }

}
