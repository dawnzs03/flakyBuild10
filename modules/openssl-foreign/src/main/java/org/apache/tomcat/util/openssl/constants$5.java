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
final class constants$5 {

    // Suppresses default constructor, ensuring non-instantiability.
    private constants$5() {}
    static final FunctionDescriptor EVP_MD_free$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle EVP_MD_free$MH = RuntimeHelper.downcallHandle(
        "EVP_MD_free",
        constants$5.EVP_MD_free$FUNC
    );
    static final FunctionDescriptor EVP_PKEY_get_base_id$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle EVP_PKEY_get_base_id$MH = RuntimeHelper.downcallHandle(
        "EVP_PKEY_get_base_id",
        constants$5.EVP_PKEY_get_base_id$FUNC
    );
    static final FunctionDescriptor EVP_PKEY_get_bits$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle EVP_PKEY_get_bits$MH = RuntimeHelper.downcallHandle(
        "EVP_PKEY_get_bits",
        constants$5.EVP_PKEY_get_bits$FUNC
    );
    static final FunctionDescriptor EC_GROUP_free$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle EC_GROUP_free$MH = RuntimeHelper.downcallHandle(
        "EC_GROUP_free",
        constants$5.EC_GROUP_free$FUNC
    );
    static final FunctionDescriptor EC_GROUP_get_curve_name$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle EC_GROUP_get_curve_name$MH = RuntimeHelper.downcallHandle(
        "EC_GROUP_get_curve_name",
        constants$5.EC_GROUP_get_curve_name$FUNC
    );
    static final FunctionDescriptor EC_KEY_new_by_curve_name$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle EC_KEY_new_by_curve_name$MH = RuntimeHelper.downcallHandle(
        "EC_KEY_new_by_curve_name",
        constants$5.EC_KEY_new_by_curve_name$FUNC
    );
}


