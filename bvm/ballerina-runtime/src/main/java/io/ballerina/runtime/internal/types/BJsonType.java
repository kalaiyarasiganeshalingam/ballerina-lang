/*
*  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package io.ballerina.runtime.internal.types;

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.constants.TypeConstants;
import io.ballerina.runtime.api.flags.TypeFlags;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.JsonType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.internal.values.MapValueImpl;

/**
 * {@code BJSONType} represents a JSON value.
 *
 * @since 0.995.0
 */
@SuppressWarnings("unchecked")
public class BJsonType extends BType implements JsonType {

    private final boolean readonly;
    private IntersectionType immutableType;

    /**
     * Create a {@code BJSONType} which represents the JSON type.
     *
     * @param typeName string name of the type
     * @param pkg of the type
     * @param readonly whether immutable
     */
    public BJsonType(String typeName, Module pkg, boolean readonly) {
        super(typeName, pkg, MapValueImpl.class);
        this.readonly = readonly;

        if (!readonly) {
            BJsonType immutableJsonType = new BJsonType(TypeConstants.READONLY_JSON_TNAME, pkg, true);
            this.immutableType = new BIntersectionType(pkg, new Type[]{this, PredefinedTypes.TYPE_READONLY},
                                                       immutableJsonType,
                                                       TypeFlags.asMask(TypeFlags.NILABLE, TypeFlags.ANYDATA,
                                                                        TypeFlags.PURETYPE), true);
        }
    }

    public BJsonType() {
        super(TypeConstants.JSON_TNAME, null, MapValueImpl.class);
        this.readonly = false;
        BJsonType immutableJsonType = new BJsonType(TypeConstants.READONLY_JSON_TNAME, pkg, true);
        this.immutableType = new BIntersectionType(pkg, new Type[]{ this, PredefinedTypes.TYPE_READONLY},
                                                   immutableJsonType,
                                                   TypeFlags.asMask(TypeFlags.NILABLE, TypeFlags.ANYDATA,
                                                                    TypeFlags.PURETYPE), true);
    }

    @Override
    public <V extends Object> V getZeroValue() {
        return null;
    }

    @Override
    public <V extends Object> V getEmptyValue() {
        return (V) new MapValueImpl<>(this);
    }

    @Override
    public int getTag() {
        return TypeTags.JSON_TAG;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BJsonType)) {
            return false;
        }

        return super.equals(obj) && this.readonly == ((BJsonType) obj).readonly;
    }

    public boolean isNilable() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return this.readonly;
    }

    @Override
    public Type getImmutableType() {
        return this.immutableType;
    }

    @Override
    public void setImmutableType(IntersectionType immutableType) {
        this.immutableType = immutableType;
    }
}
