/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by jextract

package org.apache.tomcat.util.openssl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
final class constants$8 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$8() {}
    static final FunctionDescriptor X509_STORE_CTX_set_error$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle X509_STORE_CTX_set_error$MH = RuntimeHelper.downcallHandle(
        "X509_STORE_CTX_set_error",
        constants$8.X509_STORE_CTX_set_error$FUNC
    );
    static final FunctionDescriptor X509_STORE_CTX_get_error_depth$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle X509_STORE_CTX_get_error_depth$MH = RuntimeHelper.downcallHandle(
        "X509_STORE_CTX_get_error_depth",
        constants$8.X509_STORE_CTX_get_error_depth$FUNC
    );
    static final FunctionDescriptor X509_STORE_CTX_get_current_cert$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle X509_STORE_CTX_get_current_cert$MH = RuntimeHelper.downcallHandle(
        "X509_STORE_CTX_get_current_cert",
        constants$8.X509_STORE_CTX_get_current_cert$FUNC
    );
    static final FunctionDescriptor X509_STORE_CTX_get0_current_issuer$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle X509_STORE_CTX_get0_current_issuer$MH = RuntimeHelper.downcallHandle(
        "X509_STORE_CTX_get0_current_issuer",
        constants$8.X509_STORE_CTX_get0_current_issuer$FUNC
    );
    static final FunctionDescriptor d2i_X509_bio$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle d2i_X509_bio$MH = RuntimeHelper.downcallHandle(
        "d2i_X509_bio",
        constants$8.d2i_X509_bio$FUNC
    );
    static final FunctionDescriptor X509_free$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle X509_free$MH = RuntimeHelper.downcallHandle(
        "X509_free",
        constants$8.X509_free$FUNC
    );
}


