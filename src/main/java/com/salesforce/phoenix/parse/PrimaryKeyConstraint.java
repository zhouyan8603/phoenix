/*******************************************************************************
 * Copyright (c) 2013, Salesforce.com, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *     Neither the name of Salesforce.com nor the names of its contributors may 
 *     be used to endorse or promote products derived from this software without 
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.salesforce.phoenix.parse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.hbase.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.salesforce.phoenix.schema.ColumnModifier;

public class PrimaryKeyConstraint extends NamedNode {
    public static final PrimaryKeyConstraint EMPTY = new PrimaryKeyConstraint(null, Collections.<Pair<ColumnName, ColumnModifier>>emptyList());

    private final List<Pair<ColumnName, ColumnModifier>> columns;
    private final HashMap<ColumnName, Pair<ColumnName, ColumnModifier>> columnNameToModifier;
    
    PrimaryKeyConstraint(String name, List<Pair<ColumnName, ColumnModifier>> columns) {
        super(name);
        this.columns = columns == null ? Collections.<Pair<ColumnName, ColumnModifier>>emptyList() : ImmutableList.copyOf(columns);
        this.columnNameToModifier = Maps.newHashMapWithExpectedSize(this.columns.size());
        for (Pair<ColumnName, ColumnModifier> p : columns) {
            this.columnNameToModifier.put(p.getFirst(), p);
        }
    }

    public List<Pair<ColumnName, ColumnModifier>> getColumnNames() {
        return columns;
    }
    
    public Pair<ColumnName, ColumnModifier> getColumn(ColumnName columnName) {
    	return columnNameToModifier.get(columnName);
    }
    
    public boolean contains(ColumnName columnName) {
        return columnNameToModifier.containsKey(columnName);
    }
}
